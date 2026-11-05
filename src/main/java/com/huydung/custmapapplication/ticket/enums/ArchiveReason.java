package com.huydung.custmapapplication.ticket.enums;

/**
 * Lý do một ca nằm trong Lịch sử (cột archive_reason, lưu dạng chuỗi = tên enum).
 * Mỗi lý do thuộc về một trang của Lịch sử ({@link HistoryTab}).
 * Cờ canComplete: ca có lý do này được phép chuyển THẲNG sang "Đã xử lý" ngay trong Lịch sử (nút "Đã xử lý" ở trang Lịch sử),
 * không phải đưa về danh sách chính rồi mới bấm, tránh ca bị trôi giữa các ca đang làm. Muốn mở cho lý do nào thì đổi cờ ở đây.
 */
public enum ArchiveReason {

    /** Bấm "Đã xử lý" (hoặc ADMIN chuyển hàng loạt). */
    DONE("Đã xử lý", "fa-solid fa-check", "reason-done", HistoryTab.COMPLETED, false),

    /** Bấm KLL: không liên lạc được khách hàng. */
    KLL("Không liên lạc được", "fa-solid fa-phone-slash", "reason-kll", HistoryTab.ISSUES, true),

    /** Đã gọi khách nhưng khách báo không có vấn đề gì. */
    CALLED_OK("Đã gọi - khách OK", "fa-solid fa-phone", "reason-called-ok", HistoryTab.ISSUES, false),

    /** Ca không còn xuất hiện trong file import mới. */
    NOT_IN_IMPORT("Không còn trong file GNOC", "fa-solid fa-box-archive", "reason-closed", HistoryTab.ISSUES, false);

    private final String label;
    private final String icon;       // class Font Awesome
    private final String badgeClass; // class CSS của nhãn trên thẻ ca
    private final HistoryTab tab;
    private final boolean canComplete; // true: được chuyển thẳng sang Đã xử lý từ Lịch sử

    ArchiveReason(String label, String icon, String badgeClass, HistoryTab tab, boolean canComplete) {
        this.label = label;
        this.icon = icon;
        this.badgeClass = badgeClass;
        this.tab = tab;
        this.canComplete = canComplete;
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

    public boolean isCanComplete() {
        return canComplete;
    }

    /** Trả về null nếu chuỗi rỗng hoặc không khớp lý do nào (ca lưu trữ cũ chưa ghi lý do). */
    public static ArchiveReason fromCode(String code) {
        if (code == null) return null;
        for (ArchiveReason r : values()) {
            if (r.name().equalsIgnoreCase(code.trim())) return r;
        }
        return null;
    }
}
