package com.huydung.custmapapplication.ticket.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Trạng thái xử lý của ca (cột app_status, lưu dạng chuỗi = tên enum).
 * Đây là nơi DUY NHẤT khai báo các trạng thái: nhãn, biểu tượng, màu, việc có chuyển ca vào Lịch sử hay không.
 * Giao diện lấy tất cả từ API /api/tickets/meta nên không phải khai báo lại ở JSP / JS.
 * Thêm trạng thái mới: thêm giá trị ở đây và đưa vào {@link StatusContext} nếu muốn hiện trong hộp chọn.
 */
public enum TicketStatus {

    DANG_XU_LY("Đang xử lý", "fa-solid fa-spinner fa-spin", "status-dang-xu-ly",
            false, null),
    HEN_GIO("Hẹn giờ", "fa-solid fa-circle-check", "status-hen-gio",
            true, null),
    HEN_KHAC("Hẹn khác", "fa-regular fa-calendar", "status-hen-khac",
            true, null),
    /** Không liên lạc được: ca chuyển vào Lịch sử (trang "Không liên lạc được & khác"). */
    KLL("KLL", "fa-solid fa-phone-slash", "status-kll",
            false, ArchiveReason.KLL),
    /** Đã xử lý hoàn tất: ca chuyển vào Lịch sử (trang "Đã hoàn thành"). */
    DA_XU_LY("Đã xử lý", "fa-solid fa-check", "status-da-xu-ly",
            false, ArchiveReason.DONE);

    private final String label;      // nhãn ngắn hiện trên thẻ ca
    private final String icon;       // class Font Awesome
    private final String badgeClass; // class CSS của nhãn
    private final boolean appointed; // true: ca đã hẹn (nằm ở trang "Đã hẹn", không hiện ở danh sách chính)
    private final ArchiveReason archiveReason; // khác null: đặt trạng thái này thì ca chuyển vào Lịch sử với lý do này

    TicketStatus(String label, String icon, String badgeClass, boolean appointed, ArchiveReason archiveReason) {
        this.label = label;
        this.icon = icon;
        this.badgeClass = badgeClass;
        this.appointed = appointed;
        this.archiveReason = archiveReason;
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

    public boolean isAppointed() {
        return appointed;
    }

    public ArchiveReason getArchiveReason() {
        return archiveReason;
    }

    /** true nếu đặt trạng thái này thì ca được chuyển vào Lịch sử. */
    public boolean archives() {
        return archiveReason != null;
    }

    /** Trả về null nếu chuỗi rỗng hoặc không khớp trạng thái nào (không phân biệt hoa thường). */
    public static TicketStatus fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (TicketStatus s : values()) {
            if (s.name().equalsIgnoreCase(code.trim())) return s;
        }
        return null;
    }

    /** true nếu chuỗi là trạng thái chuyển ca vào Lịch sử (KLL hoặc Đã xử lý). */
    public static boolean isArchiving(String code) {
        TicketStatus s = fromCode(code);
        return s != null && s.archives();
    }

    /** Mã các trạng thái "đã hẹn" (HEN_GIO, HEN_KHAC), dùng làm tham số truy vấn. */
    public static List<String> appointedCodes() {
        return Arrays.stream(values()).filter(TicketStatus::isAppointed).map(Enum::name).toList();
    }
}
