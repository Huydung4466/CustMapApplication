package com.huydung.custmapapplication.user.controller;

import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.user.dto.UserDTO;
import com.huydung.custmapapplication.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API HIỂN THỊ thông tin tài khoản của chính người đang đăng nhập (mọi vai trò). Sửa thông tin: user.api.ProfileApiController. */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final AuthService authService;
    private final UserService userService;

    public ProfileController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserDTO> getMyProfile() {
        Long userId = authService.getCurrentLoggedUser().getId();
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}
