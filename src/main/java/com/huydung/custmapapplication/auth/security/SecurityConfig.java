package com.huydung.custmapapplication.auth.security;

import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import jakarta.servlet.SessionCookieConfig;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Cookie phiên: luôn HttpOnly (JS không đọc được). Secure chỉ bật khi chạy sau HTTPS: app.security.cookie-secure=true
    // (đặt trong application-pro.properties; máy phát triển chạy http://localhost thì để false)
    @Bean
    public ServletContextInitializer sessionCookieHardening(@Value("${app.security.cookie-secure:false}") boolean secure) {
        return servletContext -> {
            SessionCookieConfig cookie = servletContext.getSessionCookieConfig();
            cookie.setHttpOnly(true);
            cookie.setSecure(secure);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CustomUserDetailsService userDetailsService,
                                           LoginAttemptService loginAttempts,
                                           @Value("${app.security.cookie-secure:false}") boolean cookieSecure,
                                           @Value("${app.remember-me.key:cust-map-remember-me-key-doi-khi-trien-khai}") String rememberMeKey,
                                           @Value("${app.remember-me.days:30}") int rememberMeDays) throws Exception {
        http
                // Bật CSRF: token nằm trong cookie XSRF-TOKEN, js/csrf.js tự gửi lại qua header X-XSRF-TOKEN cho mọi lệnh
                // ghi (POST/PUT/DELETE, kể cả upload multipart); form đăng nhập gửi qua trường ẩn _csrf
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                // Chặn dò mật khẩu: tài khoản đang bị khóa bị từ chối trước khi xác thực
                .addFilterBefore(new LoginThrottleFilter(loginAttempts, "/do-login"), UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31_536_000))
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Camera và GPS chỉ cho chính trang này dùng; chặn micro
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(self), geolocation=(self), microphone=()"))
                )
                .authorizeHttpRequests(auth -> auth
                        // Cho phép forward/include tới các file JSP trong WEB-INF/views (kể cả fragment thanh điều hướng)
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR).permitAll()
                        // Cho phép truy cập trang login và tài nguyên tĩnh
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        // API Quản lý User chỉ dành cho ADMIN
                        .requestMatchers("/api/users/**").hasAuthority("ROLE_ADMIN")
                        // Import file Excel, danh sách nhân viên và thao tác hàng loạt (Đã xử lý / Xóa) chỉ dành cho ADMIN
                        .requestMatchers("/api/tickets/import", "/api/tickets/staffs", "/api/tickets/bulk").hasAuthority("ROLE_ADMIN")
                        // Xuất báo cáo Excel (tạo, danh sách, tải về, xóa) chỉ dành cho ADMIN
                        .requestMatchers("/api/exports/**").hasAuthority("ROLE_ADMIN")
                        // Sửa cấu hình hệ thống (VD: bật/tắt mục in lên ảnh) chỉ dành cho ADMIN; đọc cấu hình thì ai đăng nhập cũng được
                        .requestMatchers(HttpMethod.PUT, "/api/settings/**").hasAuthority("ROLE_ADMIN")
                        // Các trang và API còn lại yêu cầu phải đăng nhập
                        .anyRequest().authenticated()
                )
                // Tài khoản bị buộc đổi mật khẩu chỉ được vào trang đổi mật khẩu
                .addFilterAfter(new ForcePasswordChangeFilter(), AnonymousAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/do-login")
                        .successHandler((request, response, authentication) -> {
                            loginAttempts.reset(request.getParameter("username"), request.getRemoteAddr());
                            new DefaultRedirectStrategy().sendRedirect(request, response, "/dashboard");
                        })
                        .failureHandler((request, response, exception) -> {
                            String username = request.getParameter("username");
                            String ip = request.getRemoteAddr();
                            loginAttempts.recordFailure(username, ip);
                            new DefaultRedirectStrategy().sendRedirect(request, response,
                                    loginAttempts.isBlocked(username, ip) ? "/login?locked=true" : "/login?error=true");
                        })
                        .permitAll()
                )
                // "Ghi nhớ mật khẩu": giữ đăng nhập bằng cookie khi session hết hạn / tắt trình duyệt.
                // Key cố định để khởi động lại Tomcat không làm mất ghi nhớ; đổi mật khẩu thì cookie cũ tự vô hiệu.
                .rememberMe(rm -> rm
                        .key(rememberMeKey)
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(rememberMeDays * 24 * 60 * 60)
                        .useSecureCookie(cookieSecure)
                        .userDetailsService(userDetailsService)
                )
                .logout(logout -> logout
                        // Các trang hiện dùng liên kết GET tới /logout nên vẫn cho phép GET (khi bật CSRF mặc định chỉ nhận POST)
                        .logoutRequestMatcher(request -> request.getRequestURI().equals(request.getContextPath() + "/logout"))
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}
