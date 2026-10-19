package com.huydung.custmapapplication.user.dto;

/**
 * Các thông tin người dùng được tự sửa trên trang Cá nhân.
 * Họ tên KHÔNG nằm ở đây vì hệ thống dùng họ tên để ghép ca phản ánh với nhân viên (cột "Giao nhân viên");
 * tên đăng nhập và quyền cũng do quản trị viên quản lý.
 */
public class UpdateProfileRequest {

    private String phone;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
