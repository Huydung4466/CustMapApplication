package com.huydung.custmapapplication.reportform.enums;

/**
 * 5 trạng thái hiển thị của mẫu. Nhãn, biểu tượng và màu nằm ở đây; giao diện lấy từ API, không viết cứng trong JSP / JS.
 * DELETED không có dòng nào trong bảng mẫu (mẫu đã xóa thật); nó chỉ xuất hiện trong lịch sử và thông báo.
 */
public enum SnippetState {
    PENDING("Chờ duyệt", "fa-solid fa-hourglass-half", "st-pending"),
    APPROVED("Đã duyệt", "fa-solid fa-circle-check", "st-approved"),
    REJECTED("Bị từ chối", "fa-solid fa-circle-xmark", "st-rejected"),
    PENDING_DELETE("Chờ xóa", "fa-regular fa-trash-can", "st-pending-delete"),
    DELETED("Đã xóa", "fa-solid fa-trash-can", "st-deleted");

    private final String label;
    private final String icon;
    private final String badgeClass;

    SnippetState(String label, String icon, String badgeClass) {
        this.label = label;
        this.icon = icon;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }

    public String getIcon() { return icon; }

    public String getBadgeClass() { return badgeClass; }

    /** Suy ra trạng thái hiển thị từ trạng thái lưu trong DB + yêu cầu đang chờ. */
    public static SnippetState resolve(String status, String pendingChange) {
        if (SnippetStatus.REJECTED.is(status)) return REJECTED;
        if (SnippetStatus.PENDING.is(status)) return PENDING;
        if (SnippetChange.DELETE.is(pendingChange)) return PENDING_DELETE;
        if (SnippetChange.EDIT.is(pendingChange)) return PENDING;
        return APPROVED;
    }
}
