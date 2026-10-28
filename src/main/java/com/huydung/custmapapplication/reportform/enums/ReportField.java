package com.huydung.custmapapplication.reportform.enums;

/**
 * 4 mục viết tay trong form báo cáo, nhân viên có thể lưu thành mẫu để dùng lại.
 * Thêm mục mới (khi form có thêm ô viết): thêm một giá trị ở đây với mã entry của ô đó.
 */
public enum ReportField {

    CONTENT("Nội dung đã thực hiện", "241578016"),
    ERROR("Lỗi phát hiện", "1528598723"),
    WARNING("Cảnh báo Công ty Công trình", "1214196581"),
    PROPOSAL("Kiến nghị, đề xuất", "1469085041");

    private final String label;
    private final String entryId; // mã entry của ô tương ứng trong Google Form

    ReportField(String label, String entryId) {
        this.label = label;
        this.entryId = entryId;
    }

    public String getLabel() {
        return label;
    }

    public String getEntryId() {
        return entryId;
    }

    public static ReportField fromCode(String code) {
        if (code != null) {
            for (ReportField f : values()) {
                if (f.name().equalsIgnoreCase(code.trim())) return f;
            }
        }
        throw new IllegalArgumentException("Mục báo cáo không hợp lệ");
    }
}
