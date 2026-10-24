package com.huydung.custmapapplication.user.controller;

import com.huydung.custmapapplication.user.dto.UserDTO;
import com.huydung.custmapapplication.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chỉ chứa các API HIỂN THỊ danh sách tài khoản (ADMIN - chặn trong SecurityConfig: /api/users/**).
 * API tạo tài khoản / đặt lại mật khẩu (thay đổi dữ liệu) nằm ở package user.api.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return ResponseEntity.ok(userService.getAllUsers(keyword, status));
    }
}
