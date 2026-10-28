package com.huydung.custmapapplication.notification.entity;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một thông báo gửi cho MỘT người nhận (gửi cho nhiều admin thì mỗi admin một dòng để trạng thái đã đọc tách riêng).
 * Thông báo lưu bền trong DB nên tải lại trang hay đăng nhập lại vẫn còn.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_recipient_read", columnList = "recipient_id,read_at"),
        @Index(name = "idx_notif_recipient_created", columnList = "recipient_id,created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class NotificationEntity extends BaseEntity {

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "type", nullable = false, length = 30)
    private String type;                 // NotificationType

    @Column(name = "message", nullable = false, length = 600)
    private String message;

    @Column(name = "snippet_id")
    private Long snippetId;              // mẫu báo cáo liên quan (bấm vào thông báo sẽ mở mẫu này)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;        // null = chưa đọc
}
