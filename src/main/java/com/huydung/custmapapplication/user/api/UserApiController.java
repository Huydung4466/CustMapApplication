package com.huydung.custmapapplication.user.api;

import com.huydung.custmapapplication.user.dto.CameraRoleRequest;
import com.huydung.custmapapplication.user.dto.CreateUserRequest;
import com.huydung.custmapapplication.user.dto.ResetPasswordRequest;
import com.huydung.custmapapplication.user.dto.UserDTO;
import com.huydung.custmapapplication.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** API THAY ĐỔI dữ liệu tài khoản dành cho ADMIN (chặn trong SecurityConfig: /api/users/**). */
@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    // ADMIN đặt lại mật khẩu cho nhân viên quên mật khẩu (chỉ tài khoản ROLE_STAFF, không phải ADMIN)
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable("id") Long id,
                                                             @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(Map.of("message",
                "Đã đặt lại mật khẩu. Người dùng sẽ phải đổi mật khẩu ở lần đăng nhập tới."));
    }

    // ADMIN cấp / thu hồi quyền Camera (lưu, xóa, tải ảnh hiện trường) cho tài khoản; có hiệu lực từ lần đăng nhập kế tiếp của người đó
    @PutMapping("/{id}/camera-role")
    public ResponseEntity<UserDTO> setCameraRole(@PathVariable("id") Long id, @RequestBody CameraRoleRequest request) {
        return ResponseEntity.ok(userService.setCameraRole(id, request != null && Boolean.TRUE.equals(request.getEnabled())));
    }
}
