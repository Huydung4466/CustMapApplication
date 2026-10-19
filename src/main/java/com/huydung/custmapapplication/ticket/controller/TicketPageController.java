package com.huydung.custmapapplication.ticket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Controller chỉ trả về trang giao diện (JSP); dữ liệu lấy qua /api/tickets/...
@Controller
public class TicketPageController {

    // Trang Lịch sử: danh sách ca đã làm / đã lưu trữ của account đang đăng nhập
    @GetMapping("/history")
    public String historyPage() {
        return "history";
    }

    // Trang Khách đã hẹn (nút 1 trên thanh điều hướng): các ca đã bấm Hẹn giờ / Hẹn khác
    @GetMapping("/appointments")
    public String appointmentsPage() {
        return "appointments";
    }
}
