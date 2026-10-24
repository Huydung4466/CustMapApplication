package com.huydung.custmapapplication.ticket.enums;

/**
 * Lý do một ca nằm trong Lịch sử (cột archive_reason, lưu dạng chuỗi = tên enum).
 * Mỗi lý do thuộc về một trang của Lịch sử ({@link HistoryTab}).
 */
public enum ArchiveReason {

    /** Bấm "Đã xử lý" (hoặc ADMIN chuyển hàng loạt). */
    DONE("Đã xử lý", "fa-solid fa-check", "reason-done", HistoryTab.COMPLETED),

    /** Bấm KLL: không liên lạc được khách hàng. */
    KLL("Không liên lạc được", "fa-solid fa-phone-slash", "reason-kll", HistoryTab.ISSUES),

    /** Ca không còn xuất hiện trong file import mới. */
    NOT_IN_IMPORT("Không còn trong file GNOC", "fa-solid fa-box-archive", "reason-closed", HistoryTab.ISSUES);

    private final String label;
    private final String icon;       // class Font Awesome
    private final String badgeClass; // class CSS của nhãn trên thẻ ca
    private final HistoryTab tab;

    ArchiveReason(String label, String icon, String badgeClass, HistoryTab tab) {
        this.label = label;
        this.icon = icon;
        this.badgeClass = badgeClass;
        this.tab = tab;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public HistoryTab getTab() {
        return tab;
    }
}
