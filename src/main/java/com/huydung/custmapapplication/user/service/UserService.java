package com.huydung.custmapapplication.user.service;

import com.huydung.custmapapplication.user.dto.ChangePasswordRequest;
import com.huydung.custmapapplication.user.dto.CreateUserRequest;
import com.huydung.custmapapplication.user.dto.ResetPasswordRequest;
import com.huydung.custmapapplication.user.dto.UpdateProfileRequest;
import com.huydung.custmapapplication.user.dto.UserDTO;

import java.util.List;

public interface UserService {

    UserDTO createUser(CreateUserRequest request);

    // Thông tin tài khoản của chính người dùng (trang Cá nhân)
    UserDTO getProfile(Long userId);

    // Người dùng tự sửa các thông tin được phép (hiện là số điện thoại)
    UserDTO updateProfile(Long userId, UpdateProfileRequest request);
    List<UserDTO> getAllUsers(String keyword, Integer status);
    void initDefaultUsersAndRoles();

    // Người dùng tự đổi mật khẩu của chính mình
    void changePassword(Long userId, ChangePasswordRequest request);

    // ADMIN đặt lại mật khẩu cho tài khoản nhân viên ROLE_STAFF (người đó sẽ bị buộc đổi ở lần đăng nhập tới)
    void resetPassword(Long userId, ResetPasswordRequest request);

    // ADMIN cấp / thu hồi quyền Camera (ROLE_CAMERA: lưu, xóa, tải ảnh hiện trường) cho một tài khoản.
    // Người đó cần đăng nhập lại để quyền mới có hiệu lực.
    UserDTO setCameraRole(Long userId, boolean enabled);
}
