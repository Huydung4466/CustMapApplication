package com.huydung.custmapapplication.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Trang Báo cáo (nút 3 trên thanh điều hướng): hiện mới là trang giữ chỗ, sẽ làm sau
@Controller
public class ReportPageController {

    @GetMapping("/report")
    public String reportPage() {
        return "report";
    }
}
