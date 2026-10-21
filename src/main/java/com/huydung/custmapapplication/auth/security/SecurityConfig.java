package com.huydung.custmapapplication.auth.security;

import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}
