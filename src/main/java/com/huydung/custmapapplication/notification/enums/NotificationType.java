package com.huydung.custmapapplication.notification.enums;

/** Loại thông báo: tiêu đề, biểu tượng và class màu nằm ở đây để giao diện không phải viết cứng. */
public enum NotificationType {
    REQUEST_PENDING("Yêu cầu chờ duyệt", "fa-solid fa-hourglass-half", "nt-warning"),
    APPROVED("Mẫu báo cáo đã được duyệt", "fa-solid fa-circle-check", "nt-success"),
    APPROVED_EDITED("Mẫu đã được duyệt và chỉnh sửa", "fa-solid fa-pen-to-square", "nt-info"),
    REJECTED("Yêu cầu bị từ chối", "fa-solid fa-circle-xmark", "nt-danger"),
    DELETE_APPROVED("Yêu cầu xóa đã được chấp thuận", "fa-solid fa-trash-can", "nt-muted"),
    REQUEST_CANCELLED("Yêu cầu đã bị hủy", "fa-solid fa-ban", "nt-muted"),
    SNIPPET_REMOVED("Mẫu báo cáo đã bị xóa", "fa-solid fa-trash-can", "nt-muted");

    private final String title;
    private final String icon;
    private final String colorClass;

    NotificationType(String title, String icon, String colorClass) {
        this.title = title;
        this.icon = icon;
        this.colorClass = colorClass;
    }

    public String getTitle() { return title; }

    public String getIcon() { return icon; }

    public String getColorClass() { return colorClass; }

    public static NotificationType fromCode(String code) {
        for (NotificationType t : values()) {
            if (t.name().equals(code)) return t;
        }
        return REQUEST_PENDING;
    }
}
