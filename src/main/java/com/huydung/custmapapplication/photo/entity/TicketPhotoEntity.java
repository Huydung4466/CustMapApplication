package com.huydung.custmapapplication.photo.entity;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ảnh chụp tại hiện trường của một ca phản ánh.
 * File ảnh nằm trên ổ đĩa (thư mục app.photo.dir); bảng này chỉ lưu đường dẫn tương đối
 * và thông tin lúc chụp (giờ, tọa độ, địa chỉ, người chụp) để làm báo cáo.
 */
@Entity
@Table(name = "ticket_photos", indexes = {
        @Index(name = "idx_photo_ticket", columnList = "ticket_id"),
        @Index(name = "idx_photo_taken_at", columnList = "taken_at"),
        @Index(name = "idx_photo_taken_by", columnList = "taken_by_user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class TicketPhotoEntity extends BaseEntity {

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    // Đường dẫn tương đối so với app.photo.dir, VD: 2026/10/18/3f2a9c1e-....jpg
    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    // Giờ chụp theo đồng hồ của SERVER (không phụ thuộc giờ trên điện thoại)
    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

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
}
