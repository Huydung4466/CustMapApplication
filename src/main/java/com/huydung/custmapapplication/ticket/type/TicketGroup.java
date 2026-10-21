package com.huydung.custmapapplication.ticket.type;

/**
 * Nhóm lớn của ca = mỗi nhóm là một tab trên giao diện.
 * Thêm nhóm mới ở đây thì giao diện tự có thêm tab.
 */
public enum TicketGroup {

    PHAN_ANH("Phản ánh", "fa-headset"),
    HA_TANG("Hạ tầng", "fa-tower-cell");

    private final String label;
    private final String icon; // class Font Awesome

    TicketGroup(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
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
