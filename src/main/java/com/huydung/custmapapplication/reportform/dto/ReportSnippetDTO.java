package com.huydung.custmapapplication.reportform.dto;

import java.time.LocalDateTime;

/**
 * Một mẫu gửi ra giao diện.
 * status / pendingChange / pendingContent: dữ liệu gốc trong DB.
 * state* : trạng thái hiển thị (SnippetState) kèm nhãn / biểu tượng / class màu để giao diện không phải viết cứng.
 * request*: loại yêu cầu đang chờ admin duyệt (Thêm mới / Sửa / Xóa), null nếu không có; stateNote là câu giải thích tương ứng.
 * mine = do chính người đang đăng nhập tạo hoặc đề nghị thay đổi; owner = do chính họ tạo.
 * directEdit / directDelete = thao tác có hiệu lực ngay (không cần admin duyệt) với người đang đăng nhập.
 */
public record ReportSnippetDTO(
        Long id,
        String field,
        String fieldLabel,
        String taskGroup,
        String taskGroupLabel,
        String content,
        String status,
        String pendingChange,
        String pendingContent,
        String state,
        String stateLabel,
        String stateIcon,
        String stateBadge,
        String stateNote,
        String requestType,
        String requestLabel,
        String requestPill,
        String rejectReason,
        String rejectedByName,
        LocalDateTime rejectedAt,
        String approvedByName,
        LocalDateTime approvedAt,
        String createdByName,
        LocalDateTime createdAt,
        String changeByName,
        LocalDateTime changeAt,
        boolean mine,
        boolean owner,
        boolean directEdit,
        boolean directDelete) {
}
