package com.huydung.custmapapplication.ticket.enums;

/**
 * Trạng thái NỘP BÁO CÁO của ca đã xử lý (cột report_status, lưu dạng chuỗi = tên enum; null coi như chưa nộp).
 * Báo cáo được nộp qua Google Form nên Google không báo ngược về hệ thống: "Đã mở form" là lúc người dùng bấm mở form,
 * còn "Đã nộp" do người dùng xác nhận (hộp hỏi ở trang /report-form hoặc nút đánh dấu trên thẻ ở trang Lịch sử).
 * Nhãn, biểu tượng, class màu khai báo ở đây; giao diện lấy từ dữ liệu ca (TicketResponseDTO), không viết cứng ở JSP / JS.
 */
public enum ReportStatus {

    NOT_SUBMITTED("Chưa nộp", "fa-regular fa-file", "report-not-submitted"),
    OPENED("Đã mở form", "fa-solid fa-hourglass-half", "report-opened"),
    SUBMITTED("Đã nộp", "fa-solid fa-file-circle-check", "report-submitted");

    private final String label;
    private final String icon;       // class Font Awesome
    private final String badgeClass; // class CSS của nhãn

    ReportStatus(String label, String icon, String badgeClass) {
        this.label = label;
        this.icon = icon;
        this.badgeClass = badgeClass;
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

    /** Chuỗi trong DB -> trạng thái; null / rỗng / không nhận ra = chưa nộp. */
    public static ReportStatus resolve(String code) {
        ReportStatus s = fromCodeOrNull(code);
        return s == null ? NOT_SUBMITTED : s;
    }

    /** Mã người dùng gửi lên -> trạng thái; mã lạ thì báo lỗi. */
    public static ReportStatus fromCode(String code) {
        ReportStatus s = fromCodeOrNull(code);
        if (s == null) {
            throw new IllegalArgumentException("Trạng thái nộp báo cáo không hợp lệ");
        }
        return s;
    }

    private static ReportStatus fromCodeOrNull(String code) {
        if (code == null || code.isBlank()) return null;
        for (ReportStatus s : values()) {
            if (s.name().equalsIgnoreCase(code.trim())) return s;
        }
        return null;
    }
}
