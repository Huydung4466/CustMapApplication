package com.huydung.custmapapplication.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Trang Cá nhân (nút 4 trên thanh điều hướng). Dữ liệu lấy qua /api/profile và (ADMIN) /api/users
@Controller
public class UserPageController {

    @GetMapping("/profile")
    public String profilePage() {
        return "profile";
    }
}
