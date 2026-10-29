package com.huydung.custmapapplication.reportform.entity;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một đoạn chữ đã lưu sẵn để dùng lại khi nộp báo cáo (VD: lỗi "không quấn đáy").
 * Dùng chung cả đội. Nhân viên thêm / sửa / xóa đều phải qua admin duyệt; chưa duyệt thì còn "đọng" ở trạng thái chờ; bị từ chối thì giữ lại kèm lý do.
 */
@Entity
@Table(name = "report_snippets", indexes = {
        @Index(name = "idx_snippet_field_group", columnList = "field_code,task_group"),
        @Index(name = "idx_snippet_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class ReportSnippetEntity extends BaseEntity {

    @Column(name = "field_code", nullable = false, length = 20)
    private String fieldCode;            // ReportField

    @Column(name = "task_group", length = 40)
    private String taskGroup;            // ReportTaskGroup; null = dùng chung mọi nhóm

    @Column(name = "content", nullable = false, length = 600)
    private String content;              // nội dung đang có hiệu lực (mẫu chờ duyệt: nội dung đề xuất)

    @Column(name = "status", nullable = false, length = 20)
    private String status;               // SnippetStatus

    @Column(name = "pending_change", length = 10)
    private String pendingChange;        // SnippetChange; null = không có yêu cầu nào đang chờ

    @Column(name = "pending_content", length = 600)
    private String pendingContent;       // nội dung đề xuất khi pendingChange = EDIT

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_by_name", length = 100)
    private String createdByName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "change_by_id")
    private Long changeById;

    @Column(name = "change_by_name", length = 100)
    private String changeByName;

    @Column(name = "change_at")
    private LocalDateTime changeAt;

    @Column(name = "approved_by_name", length = 100)
    private String approvedByName;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Từ chối mẫu mới: lý do + người từ chối (mẫu bị từ chối vẫn giữ lại để người tạo xem lý do và sửa gửi lại)
    @Column(name = "reject_reason", length = 300)
    private String rejectReason;

    @Column(name = "rejected_by_name", length = 100)
    private String rejectedByName;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    // Khóa lạc quan: hai admin cùng duyệt một yêu cầu thì chỉ một người thành công, người kia nhận báo "đã được xử lý".
    // default 0 để dòng cũ trong DB (khi hibernate tự thêm cột) có giá trị hợp lệ.
    @Version
    @Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
    private Long version;
}
