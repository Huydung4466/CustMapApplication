package com.huydung.custmapapplication.ticket.controller;

import com.huydung.custmapapplication.ticket.enums.HistoryTab;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Controller chỉ trả về trang giao diện (JSP); dữ liệu lấy qua /api/tickets/...
@Controller
public class TicketPageController {

    // Trang Lịch sử 1: công việc đã hoàn thành (bấm "Đã xử lý")
    @GetMapping("/history")
    public String historyCompletedPage(Model model) {
        return historyPage(model, HistoryTab.COMPLETED);
    }

    // Trang Lịch sử 2: ca không liên lạc được (KLL) hoặc các vấn đề khác (không còn trong file GNOC...)
    @GetMapping("/history/issues")
    public String historyIssuesPage(Model model) {
        return historyPage(model, HistoryTab.ISSUES);
    }

    // Hai trang dùng chung một JSP (history.jsp); trang nào hiện ca nào do HistoryTab quyết định
    private String historyPage(Model model, HistoryTab tab) {
        model.addAttribute("historyTab", tab.name());
        model.addAttribute("historyTabLabel", tab.getLabel());
        model.addAttribute("historyTabIcon", tab.getIcon());
        return "history";
    }

    // Trang Khách đã hẹn (nút 1 trên thanh điều hướng): các ca đã bấm Hẹn giờ / Hẹn khác
    @GetMapping("/appointments")
    public String appointmentsPage() {
        return "appointments";
    }
}
