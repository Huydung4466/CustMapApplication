package com.huydung.custmapapplication.reportform.dto;

import java.util.List;
import java.util.Map;

/**
 * Cấu hình form báo cáo cho giao diện: địa chỉ form, các nhóm nhiệm vụ, các mục viết (kèm mã entry và gợi ý),
 * mã entry của các ô thông tin ca, các trạng thái của mẫu. Địa chỉ và mã entry lấy từ application.properties.
 */
public record ReportFormMetaDTO(
        String formUrl,
        String taskGroupEntry,
        int maxContentLength,
        List<TaskGroupInfo> taskGroups,
        List<FieldInfo> fields,
        Map<String, String> infoEntries,
        Map<String, String> suggestedGroupByTicketType,
        List<StateInfo> states) {

    public record TaskGroupInfo(String code, String label) {
    }

    public record FieldInfo(String code, String label, String entryId, String hint) {
    }

    /** Một trạng thái hiển thị của mẫu (SnippetState): nhãn, biểu tượng, class màu. */
    public record StateInfo(String code, String label, String icon, String badgeClass) {
    }
}
