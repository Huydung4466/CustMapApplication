package com.huydung.custmapapplication.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tài khoản đang bị buộc đổi mật khẩu chỉ được vào trang đổi mật khẩu, đăng xuất và các tài nguyên tĩnh.
 * Mọi đường dẫn khác: trang web -> chuyển sang /change-password, API -> trả 403 dạng JSON.
 * Lưu ý: KHÔNG gắn @Component, filter này được đăng ký trong SecurityConfig.
 */
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails user
                && user.isMustChangePassword()) {

            String path = request.getRequestURI().substring(request.getContextPath().length());

            if (!isAllowed(path)) {
                if (path.startsWith("/api/")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"message\":\"Bạn cần đổi mật khẩu trước khi tiếp tục sử dụng\",\"code\":\"PASSWORD_CHANGE_REQUIRED\"}");
                } else {
                    response.sendRedirect(request.getContextPath() + "/change-password");
                }
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowed(String path) {
        return path.equals("/change-password")
                || path.equals("/api/auth/change-password")
                || path.equals("/api/auth/me")
                || path.equals("/logout")
                || path.equals("/login")
                || path.equals("/do-login")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/");
    }
}
