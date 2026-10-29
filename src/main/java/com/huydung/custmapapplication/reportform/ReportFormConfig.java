package com.huydung.custmapapplication.reportform;

import com.huydung.custmapapplication.reportform.enums.ReportField;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cấu hình Google Form "BÁO CÁO KẾT QUẢ CÔNG VIỆC HẰNG NGÀY - ĐỘI KỸ THUẬT", đọc từ application.properties:
 *   app.report-form.url                  = link form (đuôi /viewform)
 *   app.report-form.entry.task-group     = mã entry của ô "Nhóm nhiệm vụ chính"
 *   app.report-form.entry.<mục viết>     = mã entry của 4 mục viết: content, error, warning, proposal (tên theo {@link ReportField})
 *   app.report-form.entry.<mục thông tin>= mã entry của các ô thông tin ca: date, staff, area, service-type, account,
 *                                          customer-name, phone, address, cabinet, contractor
 * Đổi / tạo lại form thì chỉ cần sửa properties. Thiếu cấu hình nào thì ứng dụng báo rõ ngay lúc khởi động.
 * Nhãn các lựa chọn của form (địa bàn, loại dịch vụ, nhóm nhiệm vụ) vẫn ghi ở ReportTaskGroup và report-form.js.
 */
@Component
@ConfigurationProperties(prefix = "app.report-form")
public class ReportFormConfig implements InitializingBean {

    /** Độ dài tối đa của một mẫu (link điền sẵn quá dài thì Google từ chối). */
    public static final int MAX_CONTENT_LENGTH = 500;

    public static final String TASK_GROUP_KEY = "task-group";

    /** Các mục thông tin ca mà trang nộp báo cáo điền sẵn vào form. */
    private static final List<String> INFO_KEYS = List.of("date", "staff", "area", "service-type", "account",
            "customer-name", "phone", "address", "cabinet", "contractor");

    private String url;
    private Map<String, String> entry = new LinkedHashMap<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getEntry() {
        return entry;
    }

    public void setEntry(Map<String, String> entry) {
        this.entry = entry;
    }

    public String formUrl() {
        return url == null ? "" : url.trim();
    }

    public String taskGroupEntry() {
        return entryOf(TASK_GROUP_KEY);
    }

    public String fieldEntry(ReportField field) {
        return entryOf(field.name().toLowerCase(Locale.ROOT));
    }

    /** Mã entry của các ô thông tin ca (khóa như trong properties), gửi cho giao diện dựng link điền sẵn. */
    public Map<String, String> infoEntries() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : INFO_KEYS) {
            map.put(key, entryOf(key));
        }
        return map;
    }

    private String entryOf(String key) {
        String v = entry.get(key);
        return v == null ? "" : v.trim();
    }

    @Override
    public void afterPropertiesSet() {
        List<String> missing = new ArrayList<>();
        if (formUrl().isEmpty()) missing.add("app.report-form.url");
        if (taskGroupEntry().isEmpty()) missing.add("app.report-form.entry." + TASK_GROUP_KEY);
        for (ReportField f : ReportField.values()) {
            if (fieldEntry(f).isEmpty()) missing.add("app.report-form.entry." + f.name().toLowerCase(Locale.ROOT));
        }
        for (String key : INFO_KEYS) {
            if (entryOf(key).isEmpty()) missing.add("app.report-form.entry." + key);
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình Google Form báo cáo trong application.properties: " + String.join(", ", missing));
        }
    }
}
