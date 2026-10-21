package com.huydung.custmapapplication.export.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Nơi lưu tạm các file Excel đã xuất. Mỗi người dùng có một thư mục riêng (theo ID tài khoản).
 * Tên file = yyyyMMdd_HHmmss_xxxxxxxx.xlsx, nên thời điểm tạo đọc thẳng từ tên file, không cần bảng dữ liệu.
 * File hết hạn (mặc định 30 phút) bị xóa bởi tác vụ định kỳ mỗi phút, và cũng bị xóa ngay khi có ai đó chạm tới.
 */
@Component
public class ExportFileStorage {

    private static final Logger log = LoggerFactory.getLogger(ExportFileStorage.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern ID_PATTERN = Pattern.compile("\\d{8}_\\d{6}_[0-9a-f]{8}");
    private static final String EXT = ".xlsx";

    /** Một file đã lưu. */
    public record Stored(String id, Path path, LocalDateTime createdAt, LocalDateTime expiresAt, long size) {
    }

    private final Path baseDir;
    private final Duration ttl;
    private final int ttlMinutes;
    private final int maxPerUser;

    public ExportFileStorage(@Value("${app.export.dir:./data/exports}") String dir,
                             @Value("${app.export.ttl-minutes:30}") int ttlMinutes,
                             @Value("${app.export.max-per-user:10}") int maxPerUser) {
        this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
        this.ttlMinutes = Math.max(1, ttlMinutes);
        this.ttl = Duration.ofMinutes(this.ttlMinutes);
        this.maxPerUser = Math.max(1, maxPerUser);
        log.info("Thư mục lưu file Excel xuất ra: {} (tự xóa sau {} phút)", this.baseDir, this.ttlMinutes);
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    public int getMaxPerUser() {
        return maxPerUser;
    }

    /** Chuẩn bị chỗ cho một file mới của người dùng (tạo thư mục nếu cần). File chưa tồn tại cho tới khi được ghi. */
    public Stored newFile(Long userId) throws IOException {
        Path userDir = userDir(userId);
        Files.createDirectories(userDir);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String id = STAMP.format(now) + "_" + UUID.randomUUID().toString().substring(0, 8);
        return new Stored(id, userDir.resolve(id + EXT), now, now.plus(ttl), 0L);
    }

    /** Các file còn hạn của người dùng, mới nhất trước. File đã hết hạn bị xóa luôn. */
    public List<Stored> list(Long userId) {
        Path userDir = userDir(userId);
        List<Stored> result = new ArrayList<>();
        if (!Files.isDirectory(userDir)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(userDir, "*" + EXT)) {
            for (Path p : stream) {
                toStored(p).ifPresent(result::add);
            }
        } catch (IOException e) {
            log.warn("Không đọc được thư mục {}: {}", userDir, e.getMessage());
        }
        result.sort(Comparator.comparing(Stored::createdAt).reversed());
        return result;
    }

    /** Tìm file theo ID (chỉ trong thư mục của chính người dùng). Hết hạn hoặc không có -> rỗng. */
    public Optional<Stored> find(Long userId, String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            return Optional.empty();
        }
        Path p = userDir(userId).resolve(id + EXT).normalize();
        if (!p.startsWith(userDir(userId)) || !Files.isRegularFile(p)) {
            return Optional.empty();
        }
        return toStored(p);
    }

    public boolean delete(Long userId, String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            return false;
        }
        Path p = userDir(userId).resolve(id + EXT).normalize();
        if (!p.startsWith(userDir(userId))) {
            return false;
        }
        return deleteQuietly(p);
    }

    /** Xóa file lỗi/dở dang khi ghi thất bại. */
    public void discard(Stored stored) {
        deleteQuietly(stored.path());
    }

    /** Chạy mỗi phút: xóa mọi file Excel đã quá hạn (kể cả của tài khoản khác hoặc sót lại sau khi khởi động lại). */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 20_000L)
    public void purgeExpired() {
        if (!Files.isDirectory(baseDir)) {
            return;
        }
        int removed = 0;
        try (Stream<Path> files = Files.walk(baseDir, 2)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                if (Files.isRegularFile(p) && p.getFileName().toString().endsWith(EXT)
                        && toStored(p).isEmpty() && !Files.exists(p)) {
                    removed++;
                }
            }
        } catch (IOException e) {
            log.warn("Không quét được thư mục file xuất: {}", e.getMessage());
        }
        if (removed > 0) {
            log.info("Đã xóa {} file Excel xuất ra đã hết hạn", removed);
        }
    }

    // ---------- nội bộ ----------

    private Path userDir(Long userId) {
        return baseDir.resolve(String.valueOf(userId)).normalize();
    }

    // Trả về file nếu còn hạn; nếu hết hạn thì xóa và trả về rỗng
    private Optional<Stored> toStored(Path p) {
        String name = p.getFileName().toString();
        String id = name.substring(0, name.length() - EXT.length());
        LocalDateTime created;
        if (ID_PATTERN.matcher(id).matches()) {
            try {
                created = LocalDateTime.parse(id.substring(0, 15), STAMP);
            } catch (RuntimeException e) {
                created = null;
            }
        } else {
            created = null;
        }
        if (created == null) {
            // Tên lạ (không phải file do hệ thống tạo): dùng giờ sửa file để vẫn được dọn dẹp, nhưng không cho tải
            try {
                LocalDateTime modified = LocalDateTime.ofInstant(Files.getLastModifiedTime(p).toInstant(),
                        java.time.ZoneId.systemDefault());
                if (LocalDateTime.now().isAfter(modified.plus(ttl))) {
                    deleteQuietly(p);
                }
            } catch (IOException ignored) {
                // bỏ qua
            }
            return Optional.empty();
        }
        LocalDateTime expires = created.plus(ttl);
        if (LocalDateTime.now().isAfter(expires)) {
            deleteQuietly(p);
            return Optional.empty();
        }
        long size;
        try {
            size = Files.size(p);
        } catch (IOException e) {
            size = 0L;
        }
        return Optional.of(new Stored(id, p, created, expires, size));
    }

    private boolean deleteQuietly(Path p) {
        try {
            return Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Không xóa được file {}: {}", p, e.getMessage());
            return false;
        }
    }
}
