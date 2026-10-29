package com.huydung.custmapapplication.notification.controller;

import com.huydung.custmapapplication.notification.dto.NotificationPageDTO;
import com.huydung.custmapapplication.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Chỉ chứa các API ĐỌC thông báo. Người nhận luôn lấy từ tài khoản đang đăng nhập, không nhận id người dùng từ client.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Danh sách thông báo mới nhất của chính mình, phân trang (số thông báo mỗi trang do NotificationService.PAGE_SIZE quyết định)
    @GetMapping
    public ResponseEntity<NotificationPageDTO> list(@RequestParam(value = "page", defaultValue = "0") int page) {
        return ResponseEntity.ok(notificationService.list(page));
    }

    // Số thông báo chưa đọc (hiện trên chuông, giao diện gọi định kỳ)
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount()));
    }
}
