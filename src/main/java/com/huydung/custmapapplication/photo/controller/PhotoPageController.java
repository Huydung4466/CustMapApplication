package com.huydung.custmapapplication.photo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang Ảnh (photos.jsp): mọi tài khoản đăng nhập đều vào xem được. */
@Controller
public class PhotoPageController {

    @GetMapping("/photos")
    public String photosPage() {
        return "photos";
    }
}
