package com.huydung.custmapapplication.reportform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Chỉ trả về trang giao diện (JSP); dữ liệu lấy qua /api/report-form/... và /api/report-snippets/...
@Controller
public class ReportFormPageController {

    // Trang trung gian trước khi sang Google Form: xem thông tin ca, chọn nhóm nhiệm vụ, chọn / viết 4 mục
    @GetMapping("/report-form/{ticketId}")
    public String reportFormPage(@PathVariable Long ticketId, Model model) {
        model.addAttribute("ticketId", ticketId);
        return "report-form";
    }

    // Trang quản lý mẫu: mọi tài khoản vào được (thêm / xem / sửa / xóa mẫu, xem yêu cầu của mình); phần duyệt chỉ admin dùng được
    // (quyền thật được kiểm tra ở service, dữ liệu và thao tác của admin không lộ cho người khác)
    @GetMapping("/report-snippets")
    public String snippetsAdminPage() {
        return "report-snippets";
    }
}
