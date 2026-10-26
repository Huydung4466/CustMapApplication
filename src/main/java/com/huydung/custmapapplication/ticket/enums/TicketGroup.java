package com.huydung.custmapapplication.ticket.enums;

/**
 * Nhóm lớn của ca = mỗi nhóm là một tab trên giao diện.
 * Thêm nhóm mới ở đây thì giao diện tự có thêm tab.
 */
public enum TicketGroup {

    // Tham số cuối: true = các ca cùng loại + cùng account chỉ hiện MỘT thẻ (kèm "Trùng xN"); false = hiện đủ từng ca
    PHAN_ANH("Phản ánh", "fa-headset", false),
    HA_TANG("Hạ tầng", "fa-tower-cell", true);

    private final String label;
    private final String icon; // class Font Awesome
    private final boolean collapseDuplicateAccounts;

    TicketGroup(String label, String icon, boolean collapseDuplicateAccounts) {
        this.label = label;
        this.icon = icon;
        this.collapseDuplicateAccounts = collapseDuplicateAccounts;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    /** true: ca cùng loại + cùng account được gộp thành một thẻ ở danh sách (xem ticket.duplicate.DuplicateRule). */
    public boolean isCollapseDuplicateAccounts() {
        return collapseDuplicateAccounts;
    }

    /** Trả về null nếu chuỗi rỗng hoặc không khớp nhóm nào. */
    public static TicketGroup fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (TicketGroup g : values()) {
            if (g.name().equalsIgnoreCase(code.trim())) return g;
        }
        return null;
    }
}
