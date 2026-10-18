package com.huydung.custmapapplication.user.controller;

import com.huydung.custmapapplication.user.dto.CreateUserRequest;
import com.huydung.custmapapplication.user.dto.ResetPasswordRequest;
import com.huydung.custmapapplication.user.dto.UserDTO;
import com.huydung.custmapapplication.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    // ADMIN đặt lại mật khẩu cho nhân viên quên mật khẩu (đã nằm trong nhóm /api/users/** chỉ ADMIN)
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable("id") Long id,
                                                             @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(Map.of("message",
                "Đã đặt lại mật khẩu. Người dùng sẽ phải đổi mật khẩu ở lần đăng nhập tới."));
    }
}
