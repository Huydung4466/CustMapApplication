package com.huydung.custmapapplication.notification.dto;

import java.util.List;

/** Một trang thông báo; unreadCount là tổng số chưa đọc của người dùng (không phải số trong trang này). */
public record NotificationPageDTO(List<NotificationDTO> items, long unreadCount, boolean hasMore) {
}
