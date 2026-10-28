package com.huydung.custmapapplication.reportform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportSnippetRequest {
    private String field;      // mã ReportField (khi thêm mới)
    private String taskGroup;  // mã ReportTaskGroup; trống = dùng chung mọi nhóm (khi thêm mới)
    private String content;    // nội dung mẫu; khi duyệt: nội dung admin chỉnh lại (trống = giữ nguyên nội dung đề xuất)
    private String reason;     // khi từ chối: lý do (bắt buộc)
}
