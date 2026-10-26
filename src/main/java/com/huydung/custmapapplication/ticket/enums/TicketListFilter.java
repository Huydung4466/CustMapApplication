package com.huydung.custmapapplication.ticket.enums;

/**
 * Các lựa chọn trong ô lọc "Trạng thái" của danh sách ca ở dashboard.
 * Đây là NƠI DUY NHẤT khai báo các lựa chọn đó: nhãn và điều kiện lọc đi kèm. Giao diện lấy danh sách từ API
 * /api/tickets/meta (listFilters) nên thêm bộ lọc mới chỉ cần:
 * <ol>
 *   <li>thêm một giá trị ở đây (nhãn + điều kiện);</li>
 *   <li>nếu cần điều kiện mới ngoài các điều kiện dưới đây: thêm cờ vào TicketFilter và xử lý trong CustomTicketRepositoryImpl.</li>
 * </ol>
 * LƯU Ý: không đổi tên giá trị đã có (giao diện gửi tên này lên server).
 */
public enum TicketListFilter {

    ALL("\uD83D\uDDC2\uFE0F Tất cả trạng thái", null, false, false),
    DANG_XU_LY("⏳ Đang xử lý", TicketStatus.DANG_XU_LY.name(), false, false),
    PINNED("⭐ Đã ghim", null, true, false),
    /** Ca có account trùng với ca khác (cùng điều kiện với nhãn "Trùng xN" trên thẻ ca). */
    DUPLICATE("🔁 Ca trùng account", null, false, true);

    private final String label;
    private final String appStatus;       // khác null: chỉ lấy ca có trạng thái này
    private final boolean onlyPinned;     // true: chỉ lấy ca đã ghim
    private final boolean onlyDuplicates; // true: chỉ lấy ca trùng account

    TicketListFilter(String label, String appStatus, boolean onlyPinned, boolean onlyDuplicates) {
        this.label = label;
        this.appStatus = appStatus;
        this.onlyPinned = onlyPinned;
        this.onlyDuplicates = onlyDuplicates;
    }

    public String getLabel() {
        return label;
    }

    public String getAppStatus() {
        return appStatus;
    }

    public boolean isOnlyPinned() {
        return onlyPinned;
    }

    public boolean isOnlyDuplicates() {
        return onlyDuplicates;
    }

    /** Trả về null nếu chuỗi rỗng hoặc không khớp lựa chọn nào (không phân biệt hoa thường). */
    public static TicketListFilter fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (TicketListFilter f : values()) {
            if (f.name().equalsIgnoreCase(code.trim())) return f;
        }
        return null;
    }
}
