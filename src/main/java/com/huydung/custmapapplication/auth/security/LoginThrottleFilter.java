package com.huydung.custmapapplication.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Chặn sớm các lần đăng nhập của tài khoản đang bị khóa (kể cả khi nhập đúng mật khẩu), trước khi Spring Security xác thực.
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

    private final LoginAttemptService attempts;
    private final String loginProcessingPath;

    public LoginThrottleFilter(LoginAttemptService attempts, String loginProcessingPath) {
        this.attempts = attempts;
        this.loginProcessingPath = loginProcessingPath;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().equals(request.getContextPath() + loginProcessingPath);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (attempts.isBlocked(request.getParameter("username"), request.getRemoteAddr())) {
            new DefaultRedirectStrategy().sendRedirect(request, response, "/login?locked=true");
            return;
        }
        chain.doFilter(request, response);
    }
}
