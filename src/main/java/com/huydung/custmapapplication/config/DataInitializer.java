package com.huydung.custmapapplication.config;

import com.huydung.custmapapplication.user.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Khởi tạo dữ liệu mặc định (Role, tài khoản ban đầu) khi ứng dụng khởi động.
 * Trước đây nằm trong AuthController (@PostConstruct); tách ra đây để controller chỉ làm việc hiển thị.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.initDefaultUsersAndRoles();
    }
}
