package com.huydung.custmapapplication.auth.controller;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;
import com.huydung.custmapapplication.auth.security.CustomUserDetails;
import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.user.dto.ChangePasswordRequest;
import com.huydung.custmapapplication.user.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostConstruct
    public void initData() {
        userService.initDefaultUsersAndRoles();
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

    // API trả về JSON chứa thông tin user đang đăng nhập (mục 4 yêu cầu của bạn)
    @GetMapping("/api/auth/me")
    @ResponseBody
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentLoggedUser());
    }

    // API đổi mật khẩu của chính người đang đăng nhập; đổi xong thì hủy phiên để đăng nhập lại bằng mật khẩu mới
    @PostMapping("/api/auth/change-password")
    @ResponseBody
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
