package com.huydung.custmapapplication.reportform.entity;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Nhật ký mỗi thao tác trên mẫu báo cáo (tạo, sửa, xóa, duyệt, từ chối): ai làm, lúc nào, nội dung trước / sau, lý do.
 * Không nối khóa ngoại tới mẫu để mẫu bị xóa thật rồi mà lịch sử vẫn còn.
 */
@Entity
@Table(name = "report_snippet_history", indexes = {
        @Index(name = "idx_snippet_hist_snippet", columnList = "snippet_id,created_at"),
        @Index(name = "idx_snippet_hist_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ReportSnippetHistoryEntity extends BaseEntity {

    @Column(name = "snippet_id", nullable = false)
    private Long snippetId;

    @Column(name = "field_code", length = 20)
    private String fieldCode;

    @Column(name = "task_group", length = 40)
    private String taskGroup;

    @Column(name = "action", nullable = false, length = 30)
    private String action;               // SnippetAction

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Column(name = "content_before", length = 600)
    private String contentBefore;

    @Column(name = "content_after", length = 600)
    private String contentAfter;

    @Column(name = "note", length = 400)
    private String note;                 // lý do từ chối / ghi chú admin đã chỉnh sửa

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
