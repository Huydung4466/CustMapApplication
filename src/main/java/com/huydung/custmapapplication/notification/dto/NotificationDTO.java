package com.huydung.custmapapplication.notification.dto;

import java.time.LocalDateTime;

/** Một thông báo gửi ra giao diện; title / icon / colorClass lấy từ NotificationType, link là đường dẫn (chưa có context path). */
public record NotificationDTO(
        Long id,
        String type,
        String title,
        String icon,
        String colorClass,
        String message,
        Long snippetId,
        String link,
        boolean read,
        LocalDateTime createdAt) {
}
