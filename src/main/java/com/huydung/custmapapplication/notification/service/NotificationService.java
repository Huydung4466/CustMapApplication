package com.huydung.custmapapplication.notification.service;

import com.huydung.custmapapplication.notification.dto.NotificationPageDTO;
import com.huydung.custmapapplication.notification.enums.NotificationType;

public interface NotificationService {

    // Số thông báo mỗi trang (riêng cho thông báo): chuông hiện 5 thông báo mới nhất, bấm "Tải thêm" để xem tiếp
    int PAGE_SIZE = 5;

    // ---- Dành cho các chức năng khác gửi thông báo (chạy chung giao dịch với nơi gọi)

    // Gửi cho mọi admin đang hoạt động, trừ excludeUserId (thường là chính người vừa thao tác)
    void notifyAdmins(NotificationType type, String message, Long snippetId, Long excludeUserId);

    void notifyUser(Long userId, NotificationType type, String message, Long snippetId);

    // Mẫu đã được xử lý: các thông báo "chờ duyệt" chưa đọc của mẫu đó tự chuyển thành đã đọc với mọi admin
    void resolveAdminRequests(Long snippetId);

    // ---- Dành cho người dùng đang đăng nhập (luôn chỉ thấy thông báo của chính mình)

    NotificationPageDTO list(int page);

    long unreadCount();

    void markRead(Long id);

    void markAllRead();
}
