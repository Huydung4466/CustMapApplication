package com.huydung.custmapapplication.auth.controller;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;
import com.huydung.custmapapplication.auth.security.CustomUserDetails;
import com.huydung.custmapapplication.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Chỉ chứa các endpoint HIỂN THỊ (trang JSP và API chỉ đọc).
 * Endpoint làm thay đổi dữ liệu nằm ở package auth.api.
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Trả về trang giao diện đăng nhập (login.jsp)
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Trang chính sau khi đăng nhập (dashboard.jsp)
    @GetMapping({"/", "/dashboard"})
    public String dashboardPage() {
        return "dashboard";
    }

    // Trang đổi mật khẩu (change-password.jsp). forced = true khi tài khoản bị buộc đổi mật khẩu
    @GetMapping("/change-password")
    public String changePasswordPage(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("forced", user != null && user.isMustChangePassword());
        return "change-password";
    }

    // API trả về JSON chứa thông tin user đang đăng nhập
    @GetMapping("/api/auth/me")
    @ResponseBody
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentLoggedUser());
    }
}
