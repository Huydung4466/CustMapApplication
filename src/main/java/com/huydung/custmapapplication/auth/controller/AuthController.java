package com.huydung.custmapapplication.auth.controller;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;
import com.huydung.custmapapplication.auth.security.CustomUserDetails;
import com.huydung.custmapapplication.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // Trả về trang giao diện đăng nhập (login.jsp).
    // Người đã đăng nhập (kể cả đăng nhập bằng "Ghi nhớ") mà vào lại /login (bấm Back, reload...) thì chuyển về dashboard,
    // riêng tài khoản bị buộc đổi mật khẩu thì chuyển sang trang đổi mật khẩu.
    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal CustomUserDetails user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        if (loggedIn && user != null) {
            return user.isMustChangePassword() ? "redirect:/change-password" : "redirect:/dashboard";
        }
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
