package com.huydung.custmapapplication.user.service;

import com.huydung.custmapapplication.user.dto.ChangePasswordRequest;
import com.huydung.custmapapplication.user.dto.CreateUserRequest;
import com.huydung.custmapapplication.user.dto.ResetPasswordRequest;
import com.huydung.custmapapplication.user.dto.UserDTO;

import java.util.List;

public interface UserService {

    UserDTO createUser(CreateUserRequest request);
    List<UserDTO> getAllUsers(String keyword, Integer status);
    void initDefaultUsersAndRoles();

    // Người dùng tự đổi mật khẩu của chính mình
    void changePassword(Long userId, ChangePasswordRequest request);

    // ADMIN đặt lại mật khẩu cho người khác (người đó sẽ bị buộc đổi ở lần đăng nhập tới)
    void resetPassword(Long userId, ResetPasswordRequest request);
}
