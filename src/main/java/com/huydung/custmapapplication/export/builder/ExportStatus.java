package com.huydung.custmapapplication.export.builder;

import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;

import java.time.LocalDateTime;

/**
 * Trạng thái của ca khi xuất báo cáo, kèm màu tô dòng (nền + chữ, dạng RGB).
 * Thứ tự khai báo cũng là thứ tự hiển thị trong phần chú giải.
 */
public enum ExportStatus {

    DONE("✔ Đã xử lý", "Đã xử lý", "Ca đã xử lý xong", 0xC6EFCE, 0x006100),
    KLL("✖ Không liên lạc được", "KLL", "Không liên lạc được khách hàng", 0xFFC7CE, 0x9C0006),
    OVERDUE("⚠ Hẹn giờ - QUÁ HẸN", "Quá hẹn", "Đã hẹn giờ nhưng đã quá giờ hẹn mà chưa cập nhật", 0xF8CBAD, 0x843C0C),
    HEN_GIO("Hẹn giờ", "Hẹn giờ", "Đã chốt giờ hẹn với khách, chưa đến giờ", 0xFFF2CC, 0x7F6000),
    HEN_KHAC("Hẹn khác", "Hẹn khác", "Khách hẹn theo cách khác (không có giờ cụ thể)", 0xDDEBF7, 0x1F4E78),
    DANG_XU_LY("Đang xử lý", "Đang xử lý", "Chưa có thay đổi nào", 0xFFFFFF, 0x000000),
    NOT_IN_IMPORT("Không còn trong file", "Không còn trong file", "Ca không còn xuất hiện trong file import mới nhất", 0xE7E6E6, 0x595959);

    private final String label;       // Hiện ở cột "Trạng thái"
    private final String shortLabel;  // Tiêu đề cột thống kê / chú giải
    private final String description;
    private final int fillRgb;
    private final int fontRgb;

    ExportStatus(String label, String shortLabel, String description, int fillRgb, int fontRgb) {
        this.label = label;
        this.shortLabel = shortLabel;
        this.description = description;
        this.fillRgb = fillRgb;
        this.fontRgb = fontRgb;
    }

    public String getLabel() { return label; }
    public String getShortLabel() { return shortLabel; }
    public String getDescription() { return description; }
    public int getFillRgb() { return fillRgb; }
    public int getFontRgb() { return fontRgb; }

    /** Ca có thay đổi trong quá trình làm việc (khác "Đang xử lý"). */
    public boolean isChanged() {
        return this != DANG_XU_LY;
    }

    /** Ca đã có kết quả cuối cùng (Đã xử lý hoặc KLL): được đưa vào sheet "Đã xử lý & KLL". */
    public boolean isProcessed() {
        return this == DONE || this == KLL;
    }

    public static ExportStatus of(CustomerTicketEntity t, LocalDateTime now) {
        String app = t.getAppStatus() == null ? "" : t.getAppStatus().trim().toUpperCase();
        String reason = t.getArchiveReason() == null ? "" : t.getArchiveReason().trim().toUpperCase();

        if ("DA_XU_LY".equals(app) || "DONE".equals(reason)) return DONE;
        if ("KLL".equals(app) || "KLL".equals(reason)) return KLL;
        if ("NOT_IN_IMPORT".equals(reason)) return NOT_IN_IMPORT;
        if ("HEN_GIO".equals(app)) {
            LocalDateTime limit = t.getAppointmentEnd() != null ? t.getAppointmentEnd() : t.getAppointmentStart();
            return (limit != null && limit.isBefore(now)) ? OVERDUE : HEN_GIO;
        }
        if ("HEN_KHAC".equals(app)) return HEN_KHAC;
        return DANG_XU_LY;
    }
}
