package com.huydung.custmapapplication.photo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Chuyển TOÀN BỘ ảnh từ thư mục cũ (app.photo.old-dir) sang thư mục mới (app.photo.dir), giữ nguyên cấu trúc con:
 *   <old-dir>/<tên đăng nhập>/yyyy/MM/dd/uuid.jpg  ->  <app.photo.dir>/<tên đăng nhập>/yyyy/MM/dd/uuid.jpg
 *   <old-dir>/yyyy/MM/dd/uuid.jpg                  ->  <app.photo.dir>/yyyy/MM/dd/uuid.jpg   (ảnh rất cũ, sau đó LegacyPhotoMigration sẽ xếp vào thư mục tài khoản)
 *
 * Database chỉ lưu đường dẫn TƯƠNG ĐỐI nên KHÔNG cần sửa dữ liệu, chỉ chuyển file.
 *
 * CHỈ CHẠY KHI BẬT: app.photo.migrate-old-dir=true, cho MỘT lần khởi động rồi tắt đi. Chạy lại vẫn an toàn.
 * Chạy TRƯỚC LegacyPhotoMigration (@Order(1)). Nên sao lưu thư mục ảnh cũ trước khi chạy.
 * - File đích đã tồn tại và cùng dung lượng: coi như đã chuyển, xóa bản ở chỗ cũ.
 * - File đích đã tồn tại nhưng khác dung lượng: giữ nguyên cả hai, ghi cảnh báo.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "app.photo.migrate-old-dir", havingValue = "true")
public class PhotoDirMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PhotoDirMigration.class);

    private final Path oldDir;
    private final Path newDir;

    public PhotoDirMigration(@Value("${app.photo.old-dir:./data/photos}") String oldDir,
                             @Value("${app.photo.dir:./data/photos}") String newDir) {
        this.oldDir = Paths.get(oldDir).toAbsolutePath().normalize();
        this.newDir = Paths.get(newDir).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Chuyển thư mục ảnh: từ {} sang {}", oldDir, newDir);

        if (oldDir.equals(newDir)) {
            log.info("Chuyển thư mục ảnh: thư mục cũ và mới trùng nhau, không cần chuyển");
            return;
        }
        if (newDir.startsWith(oldDir) || oldDir.startsWith(newDir)) {
            log.error("Chuyển thư mục ảnh: thư mục này nằm trong thư mục kia ({} / {}), bỏ qua để tránh mất ảnh. Hãy chọn hai thư mục tách biệt.",
                    oldDir, newDir);
            return;
        }
        if (!Files.isDirectory(oldDir)) {
            log.warn("Chuyển thư mục ảnh: không thấy thư mục cũ {}. Kiểm tra lại app.photo.old-dir (đường dẫn tương đối được tính từ thư mục chạy Tomcat).", oldDir);
            return;
        }

        List<Path> files;
        try (Stream<Path> s = Files.walk(oldDir)) {
            files = s.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Chuyển thư mục ảnh: không đọc được thư mục cũ {}", oldDir, e);
            return;
        }
        if (files.isEmpty()) {
            log.info("Chuyển thư mục ảnh: thư mục cũ không có file nào");
            removeEmptyDirs();
            return;
        }

        int moved = 0, alreadyThere = 0, conflict = 0, failed = 0;
        for (Path src : files) {
            Path dest = newDir.resolve(oldDir.relativize(src).toString()).normalize();
            if (!dest.startsWith(newDir)) {
                failed++;
                log.error("Chuyển thư mục ảnh: đường dẫn đích không hợp lệ cho {}", src);
                continue;
            }
            try {
                if (Files.exists(dest)) {
                    if (Files.size(dest) == Files.size(src)) {
                        Files.delete(src);
                        alreadyThere++;
                    } else {
                        conflict++;
                        log.warn("Chuyển thư mục ảnh: {} đã có ở chỗ mới nhưng khác dung lượng, giữ nguyên cả hai", dest);
                    }
                    continue;
                }
                Files.createDirectories(dest.getParent());
                Files.move(src, dest);   // khác ổ đĩa thì tự sao chép rồi xóa; không ghi đè
                moved++;
            } catch (IOException e) {
                failed++;
                log.error("Chuyển thư mục ảnh: lỗi khi chuyển {} -> {}", src, dest, e);
            }
        }
        log.info("Chuyển thư mục ảnh xong: đã chuyển {}, đã có sẵn ở chỗ mới {}, trùng tên khác nội dung {}, lỗi {}. Hãy tắt app.photo.migrate-old-dir.",
                moved, alreadyThere, conflict, failed);

        removeEmptyDirs();
    }

    // Dọn các thư mục con đã trống ở chỗ cũ (giữ lại thư mục gốc cũ)
    private void removeEmptyDirs() {
        try (Stream<Path> s = Files.walk(oldDir)) {
            List<Path> dirs = s.filter(Files::isDirectory)
                    .filter(d -> !d.equals(oldDir))
                    .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
                    .collect(Collectors.toList());
            for (Path d : dirs) {
                try {
                    Files.delete(d);   // chỉ xóa được thư mục trống
                } catch (IOException ignored) {
                    // còn file bên trong: giữ lại
                }
            }
        } catch (IOException e) {
            log.warn("Chuyển thư mục ảnh: không dọn được thư mục trống ở {}", oldDir, e);
        }
    }
}
