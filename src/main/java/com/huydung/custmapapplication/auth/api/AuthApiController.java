package com.huydung.custmapapplication.auth.api;

import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.user.dto.ChangePasswordRequest;
import com.huydung.custmapapplication.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Các API của phần tài khoản đăng nhập có THAY ĐỔI dữ liệu. */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;
    private final UserService userService;

    public AuthApiController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    // Đổi mật khẩu của chính người đang đăng nhập; đổi xong thì hủy phiên để đăng nhập lại bằng mật khẩu mới
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request,
                                                              HttpServletRequest httpRequest) {
        Long userId = authService.getCurrentLoggedUser().getId();
        userService.changePassword(userId, request);

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."));
    }
}
