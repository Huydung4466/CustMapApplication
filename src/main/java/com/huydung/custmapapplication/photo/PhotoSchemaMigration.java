package com.huydung.custmapapplication.photo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cột ticket_id của bảng ticket_photos trước đây là NOT NULL; ảnh chụp nhanh (không gắn ca) cần cột này được để trống.
 * ddl-auto=update chỉ thêm cột mới chứ không gỡ ràng buộc NOT NULL cũ, nên gỡ ở đây khi khởi động.
 * Chạy lại nhiều lần vẫn an toàn (PostgreSQL bỏ qua nếu cột đã cho phép trống). Lỗi chỉ ghi log, không chặn khởi động.
 */
@Component
public class PhotoSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PhotoSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public PhotoSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE ticket_photos ALTER COLUMN ticket_id DROP NOT NULL");
            log.info("Đã cho phép ticket_photos.ticket_id để trống (ảnh chụp nhanh không gắn ca)");
        } catch (Exception e) {
            log.warn("Không chỉnh được cột ticket_photos.ticket_id (bảng chưa có hoặc không đủ quyền). "
                    + "Nếu lưu ảnh không gắn ca báo lỗi, hãy chạy tay: ALTER TABLE ticket_photos ALTER COLUMN ticket_id DROP NOT NULL; - {}", e.getMessage());
        }
    }
}
