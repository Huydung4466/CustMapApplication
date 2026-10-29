package com.huydung.custmapapplication.reportform;

/**
 * Cấu hình Google Form "BÁO CÁO KẾT QUẢ CÔNG VIỆC HẰNG NGÀY - ĐỘI KỸ THUẬT".
 * Mã entry lấy từ chính form: nếu đội đổi / tạo lại form thì cập nhật các mã ở đây,
 * ở ReportField (4 mục viết) và ở REPORT_ENTRY trong static/js/report-form.js (các mục thông tin ca).
 */
public final class ReportFormConfig {

    private ReportFormConfig() {
    }

    public static final String FORM_URL =
            "https://docs.google.com/forms/d/e/1FAIpQLSfCOYYflGscdYCRIoTd71J4twFHl3KqTMzANM20Ps2FjeQHzA/viewform";

    /** Mã entry của ô "Nhóm nhiệm vụ chính". */
    public static final String TASK_GROUP_ENTRY = "429269979";

    /** Độ dài tối đa của một mẫu (link điền sẵn quá dài thì Google từ chối). */
    public static final int MAX_CONTENT_LENGTH = 500;
}
