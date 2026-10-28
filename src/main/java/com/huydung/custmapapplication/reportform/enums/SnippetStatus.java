package com.huydung.custmapapplication.reportform.enums;

/**
 * Trạng thái LƯU TRONG DB của mẫu. Trạng thái hiển thị cho người dùng (5 loại) do SnippetState suy ra từ
 * trạng thái này kết hợp với yêu cầu đang chờ (SnippetChange).
 */
public enum SnippetStatus {
    PENDING,   // mới tạo (hoặc gửi lại sau khi bị từ chối), chờ admin duyệt (chỉ người tạo và admin nhìn thấy)
    APPROVED,  // đã duyệt, cả đội dùng được
    REJECTED;  // mẫu mới bị admin từ chối (kèm lý do); chỉ người tạo và admin nhìn thấy, người tạo sửa lại được

    public boolean is(String code) {
        return name().equals(code);
    }
}
