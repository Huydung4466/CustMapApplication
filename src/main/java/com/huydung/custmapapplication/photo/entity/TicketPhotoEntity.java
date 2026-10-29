package com.huydung.custmapapplication.photo.entity;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ảnh chụp tại hiện trường, do tài khoản có quyền Camera (ROLE_CAMERA) lưu lên máy chủ.
 * Ảnh gắn với một ca (ticketId) hoặc là ảnh chụp nhanh không gắn ca (ticketId = null).
 * File ảnh nằm trên ổ đĩa (thư mục app.photo.dir, trong thư mục con theo tên đăng nhập người chụp);
 * bảng này chỉ lưu đường dẫn tương đối và thông tin lúc chụp (giờ, tọa độ, địa chỉ, người chụp, hạn tự xóa).
 */
@Entity
@Table(name = "ticket_photos", indexes = {
        @Index(name = "idx_photo_ticket", columnList = "ticket_id"),
        @Index(name = "idx_photo_taken_at", columnList = "taken_at"),
        @Index(name = "idx_photo_taken_by", columnList = "taken_by_user_id"),
        @Index(name = "idx_photo_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
public class TicketPhotoEntity extends BaseEntity {

    // null = ảnh chụp nhanh không gắn ca. (Cột cũ từng là NOT NULL: PhotoSchemaMigration gỡ ràng buộc này khi khởi động.)
    @Column(name = "ticket_id")
    private Long ticketId;

    // Đường dẫn tương đối so với app.photo.dir.
    // Ảnh mới: <tên đăng nhập>/yyyy/MM/dd/<uuid>.jpg; ảnh cũ (trước khi có thư mục theo tài khoản): yyyy/MM/dd/<uuid>.jpg
    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    // Giờ chụp theo đồng hồ của SERVER (không phụ thuộc giờ trên điện thoại)
    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

    // Hạn tự xóa = giờ chụp + app.photo.retention-hours (mặc định 72 giờ). null = ảnh cũ, không bao giờ tự xóa.
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // Độ chính xác GPS (mét) do thiết bị báo
    @Column(name = "accuracy_m")
    private Double accuracyM;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    // GEOCODE: tự dò từ GPS | TICKET: lấy địa chỉ lắp đặt của KH trong ca | null: không có địa chỉ
    @Column(name = "address_source", length = 20)
    private String addressSource;

    @Column(name = "taken_by_user_id")
    private Long takenByUserId;

    @Column(name = "taken_by_name", length = 100)
    private String takenByName;

    // Tên đăng nhập người chụp (null với ảnh cũ)
    @Column(name = "taken_by_username", length = 100)
    private String takenByUsername;
}
