package com.huydung.custmapapplication.user.api;

import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.user.dto.UpdateProfileRequest;
import com.huydung.custmapapplication.user.dto.UserDTO;
import com.huydung.custmapapplication.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API THAY ĐỔI thông tin tài khoản của chính người đang đăng nhập (chỉ sửa được các trường cho phép). */
@RestController
@RequestMapping("/api/profile")
public class ProfileApiController {

    private final AuthService authService;
    private final UserService userService;

    public ProfileApiController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PutMapping
    public ResponseEntity<UserDTO> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        Long userId = authService.getCurrentLoggedUser().getId();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }
}
