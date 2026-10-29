package com.huydung.custmapapplication.photo;

import com.huydung.custmapapplication.photo.entity.TicketPhotoEntity;
import com.huydung.custmapapplication.photo.repository.TicketPhotoRepository;
import com.huydung.custmapapplication.user.entity.UserEntity;
import com.huydung.custmapapplication.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Chuyển ẢNH CŨ (lưu từ trước khi có thư mục theo tài khoản, dạng yyyy/MM/dd/uuid.jpg) vào thư mục của người đã chụp:
 *   app.photo.dir/yyyy/MM/dd/uuid.jpg  ->  app.photo.dir/<tên đăng nhập>/yyyy/MM/dd/uuid.jpg
 * rồi cập nhật đường dẫn trong bảng ticket_photos (và ghi tên đăng nhập người chụp).
 *
 * CHỈ CHẠY KHI BẬT: đặt app.photo.migrate-legacy=true (hoặc biến môi trường APP_PHOTO_MIGRATE_LEGACY=true) cho MỘT lần khởi động,
 * rồi tắt đi. Chạy lại vẫn an toàn: ảnh đã chuyển (đã có tên đăng nhập) được bỏ qua.
 * - Ảnh cũ KHÔNG có hạn tự xóa (expires_at để trống) và vẫn như vậy sau khi chuyển.
 * - Ảnh không rõ người chụp (không còn tài khoản) giữ nguyên chỗ cũ.
 * - Mỗi ảnh được chuyển và cập nhật trong một giao dịch riêng; nếu cập nhật database lỗi thì file được chuyển trả về chỗ cũ.
 * Nên sao lưu thư mục ảnh trước khi chạy.
 */
@Component
@ConditionalOnProperty(name = "app.photo.migrate-legacy", havingValue = "true")
public class LegacyPhotoMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyPhotoMigration.class);

    private enum Result { MOVED, NO_USER, MISSING_FILE }

    private final TicketPhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate tx;
    private final Path baseDir;

    public LegacyPhotoMigration(TicketPhotoRepository photoRepository,
                                UserRepository userRepository,
                                PlatformTransactionManager transactionManager,
                                @Value("${app.photo.dir:./data/photos}") String photoDir) {
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.tx = new TransactionTemplate(transactionManager);
        this.baseDir = Paths.get(photoDir).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Long> ids = tx.execute(status -> photoRepository.findAll().stream()
                .filter(p -> p.getTakenByUsername() == null)
                .map(TicketPhotoEntity::getId)
                .collect(Collectors.toList()));
        if (ids == null || ids.isEmpty()) {
            log.info("Chuyển ảnh cũ: không có ảnh nào cần chuyển");
            return;
        }
        log.info("Chuyển ảnh cũ: có {} ảnh cần chuyển vào thư mục theo tài khoản ({})", ids.size(), baseDir);

        int moved = 0, noUser = 0, missing = 0, failed = 0;
        for (Long id : ids) {
            try {
                Result r = tx.execute(status -> migrateOne(id));
                if (r == Result.MOVED) moved++;
                else if (r == Result.NO_USER) noUser++;
                else if (r == Result.MISSING_FILE) missing++;
            } catch (Exception e) {
                failed++;
                log.error("Chuyển ảnh cũ: lỗi với ảnh ID {}", id, e);
            }
        }
        log.info("Chuyển ảnh cũ xong: đã chuyển {}, không rõ người chụp {}, thiếu file {}, lỗi {}. Hãy tắt app.photo.migrate-legacy.",
                moved, noUser, missing, failed);
    }

    private Result migrateOne(Long id) {
        TicketPhotoEntity photo = photoRepository.findById(id).orElse(null);
        if (photo == null || photo.getTakenByUsername() != null) {
            return Result.MOVED;   // đã được chuyển / đã bị xóa trong lúc chạy: coi như xong
        }
        Optional<UserEntity> user = photo.getTakenByUserId() == null
                ? Optional.empty() : userRepository.findById(photo.getTakenByUserId());
        if (user.isEmpty()) {
            log.warn("Chuyển ảnh cũ: ảnh ID {} không rõ người chụp, giữ nguyên chỗ cũ ({})", id, photo.getFilePath());
            return Result.NO_USER;
        }
        UserEntity u = user.get();
        String folder = ownerFolder(u.getUsername(), u.getId());

        Path src = baseDir.resolve(photo.getFilePath()).normalize();
        if (!src.startsWith(baseDir)) {
            throw new IllegalStateException("Đường dẫn ảnh không hợp lệ: " + photo.getFilePath());
        }
        if (!Files.isRegularFile(src)) {
            // Không còn file trên ổ đĩa: chỉ ghi tên đăng nhập để lần sau không xét lại
            log.warn("Chuyển ảnh cũ: ảnh ID {} không còn file {}", id, src);
            photo.setTakenByUsername(u.getUsername());
            photoRepository.save(photo);
            return Result.MISSING_FILE;
        }

        String newRelative = folder + "/" + photo.getFilePath();
        Path dest = baseDir.resolve(newRelative).normalize();
        if (!dest.startsWith(baseDir)) {
            throw new IllegalStateException("Đường dẫn đích không hợp lệ: " + newRelative);
        }
        try {
            Files.createDirectories(dest.getParent());
            Files.move(src, dest);   // không ghi đè: đích đã tồn tại thì báo lỗi
        } catch (IOException e) {
            throw new IllegalStateException("Không chuyển được file " + src + " -> " + dest, e);
        }
        try {
            photo.setFilePath(newRelative);
            photo.setTakenByUsername(u.getUsername());
            photoRepository.saveAndFlush(photo);
        } catch (RuntimeException e) {
            try {
                Files.move(dest, src);   // cập nhật database lỗi: trả file về chỗ cũ để ảnh vẫn xem được
            } catch (IOException ioe) {
                log.error("Không trả được file về chỗ cũ: {} -> {}", dest, src, ioe);
            }
            throw e;
        }
        removeEmptyParents(src);
        return Result.MOVED;
    }

    // Giống TicketPhotoServiceImpl.ownerFolder: tên đăng nhập, chỉ giữ chữ, số, dấu chấm, gạch ngang, gạch dưới
    private String ownerFolder(String username, Long userId) {
        String name = username == null ? "" : username.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.length() > 60) name = name.substring(0, 60);
        if (name.isEmpty() || name.startsWith(".")) name = "u_" + userId;
        return name;
    }

    // Dọn thư mục ngày / tháng / năm đã trống ở chỗ cũ (dừng ở thư mục gốc hoặc thư mục còn file)
    private void removeEmptyParents(Path file) {
        Path dir = file.getParent();
        while (dir != null && dir.startsWith(baseDir) && !dir.equals(baseDir)) {
            try {
                Files.delete(dir);
            } catch (IOException e) {
                break;
            }
            dir = dir.getParent();
        }
    }
}
