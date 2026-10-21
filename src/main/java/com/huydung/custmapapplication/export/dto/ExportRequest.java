package com.huydung.custmapapplication.export.dto;

/** Yêu cầu xuất báo cáo Excel. */
public class ExportRequest {

    // Họ tên nhân viên cần xuất; để trống hoặc "ALL" = tất cả nhân viên
    private String staff;
    // ALL = tất cả ca | CHANGED = chỉ ca có thay đổi (đã xử lý, KLL, hẹn giờ, hẹn khác, không còn trong file)
    private String scope;

    public String getStaff() { return staff; }
    public void setStaff(String staff) { this.staff = staff; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
