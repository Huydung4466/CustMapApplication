package com.huydung.custmapapplication.reportform.enums;

/**
 * Các mục viết tay trong form báo cáo (nội dung, lỗi, cảnh báo, kiến nghị, số cài Tammi), nhân viên có thể lưu thành mẫu để dùng lại.
 * Mã entry của từng ô trong Google Form nằm ở application.properties: app.report-form.entry.<tên mục viết thường>
 * (content, error, warning, proposal, tammi). Thêm mục mới (khi form có thêm ô viết): thêm một giá trị ở đây
 * và thêm dòng app.report-form.entry.<tên> tương ứng vào properties.
 */
public enum ReportField {

    CONTENT("Nội dung đã thực hiện"),
    ERROR("Lỗi phát hiện"),
    WARNING("Cảnh báo Công ty Công trình"),
    PROPOSAL("Kiến nghị, đề xuất"),
    // Ô "Số Cài đặt tammi cho Khách hàng": các số điện thoại khách được cài Tammi, nhiều số cách nhau bằng dấu chấm phẩy
    TAMMI("Số cài đặt Tammi cho khách hàng", "Nếu cài nhiều số thì nhập cách nhau bởi dấu \";\" (VD: 0912345678;0987654321)");

    private final String label;
    private final String hint;   // dòng gợi ý hiện dưới tên ô; rỗng = không có

    ReportField(String label) {
        this(label, "");
    }

    ReportField(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }

    public String getLabel() {
        return label;
    }

    public String getHint() {
        return hint;
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
