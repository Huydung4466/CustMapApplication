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
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CustomUserDetailsService userDetailsService,
                                           @Value("${app.remember-me.key:cust-map-remember-me-key-doi-khi-trien-khai}") String rememberMeKey,
                                           @Value("${app.remember-me.days:30}") int rememberMeDays) throws Exception {
        http
                // Tắt CSRF cho các API nội bộ để gọi Ajax/Multipart Upload dễ dàng
                .csrf(csrf -> csrf.disable())
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
                        // Ảnh hiện trường: lưu ảnh, xóa ảnh, tải về, bật / tắt tự xóa chỉ dành cho tài khoản có quyền Camera;
                        // xem danh sách / ảnh / ảnh thu nhỏ thì ai đăng nhập cũng được (quyền cũng được kiểm tra lại trong service)
                        .requestMatchers(HttpMethod.POST, "/api/photos", "/api/tickets/*/photos").hasAuthority("ROLE_CAMERA")
                        .requestMatchers(HttpMethod.DELETE, "/api/photos/**").hasAuthority("ROLE_CAMERA")
                        .requestMatchers(HttpMethod.PUT, "/api/photos/settings").hasAuthority("ROLE_CAMERA")
                        .requestMatchers(HttpMethod.GET, "/api/photos/*/download").hasAuthority("ROLE_CAMERA")
                        // Các trang và API còn lại yêu cầu phải đăng nhập
                        .anyRequest().authenticated()
                )
                // Tài khoản bị buộc đổi mật khẩu chỉ được vào trang đổi mật khẩu
                .addFilterAfter(new ForcePasswordChangeFilter(), AnonymousAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/do-login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                // "Ghi nhớ mật khẩu": giữ đăng nhập bằng cookie khi session hết hạn / tắt trình duyệt.
                // Key cố định để khởi động lại Tomcat không làm mất ghi nhớ; đổi mật khẩu thì cookie cũ tự vô hiệu.
                .rememberMe(rm -> rm
                        .key(rememberMeKey)
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(rememberMeDays * 24 * 60 * 60)
                        .userDetailsService(userDetailsService)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}
