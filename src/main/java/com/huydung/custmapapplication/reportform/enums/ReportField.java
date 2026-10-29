package com.huydung.custmapapplication.reportform.enums;

/**
 * 4 mục viết tay trong form báo cáo, nhân viên có thể lưu thành mẫu để dùng lại.
 * Mã entry của từng ô trong Google Form nằm ở application.properties: app.report-form.entry.<tên mục viết thường>
 * (content, error, warning, proposal). Thêm mục mới (khi form có thêm ô viết): thêm một giá trị ở đây
 * và thêm dòng app.report-form.entry.<tên> tương ứng vào properties.
 */
public enum ReportField {

    CONTENT("Nội dung đã thực hiện"),
    ERROR("Lỗi phát hiện"),
    WARNING("Cảnh báo Công ty Công trình"),
    PROPOSAL("Kiến nghị, đề xuất");

    private final String label;

    ReportField(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
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
