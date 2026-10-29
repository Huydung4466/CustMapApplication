package com.huydung.custmapapplication.notification.api;

import com.huydung.custmapapplication.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** API THAY ĐỔI thông báo: đánh dấu đã đọc (một cái hoặc tất cả). Chỉ tác động lên thông báo của chính người đăng nhập. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;

    public NotificationApiController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Long>> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(Map.of("unread", notificationService.unreadCount()));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Long>> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(Map.of("unread", 0L));
    }
}
