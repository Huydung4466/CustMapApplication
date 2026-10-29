package com.huydung.custmapapplication.reportform.repository;

import com.huydung.custmapapplication.reportform.entity.ReportSnippetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportSnippetRepository extends JpaRepository<ReportSnippetEntity, Long> {

    // Mẫu một người được DÙNG khi nộp báo cáo: mẫu đã duyệt + mẫu chính mình tạo đang chờ duyệt (mẫu bị từ chối không dùng được)
    @Query("""
            SELECT s FROM ReportSnippetEntity s
            WHERE s.fieldCode = :field AND (s.status = 'APPROVED' OR (s.status = 'PENDING' AND s.createdById = :userId))
            ORDER BY s.content
            """)
    List<ReportSnippetEntity> findVisible(@Param("field") String field, @Param("userId") Long userId);

    // Trang quản lý của nhân viên: mẫu đã duyệt + mọi mẫu chính mình tạo (kể cả chờ duyệt, bị từ chối)
    @Query("""
            SELECT s FROM ReportSnippetEntity s
            WHERE s.status = 'APPROVED' OR s.createdById = :userId
            ORDER BY s.fieldCode, s.taskGroup, s.content
            """)
    List<ReportSnippetEntity> findForManage(@Param("userId") Long userId);

    // Dùng để chặn trùng nội dung trong cùng mục + cùng nhóm
    List<ReportSnippetEntity> findByFieldCodeAndTaskGroup(String fieldCode, String taskGroup);

    List<ReportSnippetEntity> findByFieldCodeAndTaskGroupIsNull(String fieldCode);

    // Trang admin: toàn bộ mẫu (cả đang chờ duyệt, bị từ chối)
    @Query("SELECT s FROM ReportSnippetEntity s ORDER BY s.fieldCode, s.taskGroup, s.content")
    List<ReportSnippetEntity> findAllForAdmin();

    // Số yêu cầu đang chờ admin duyệt (mẫu mới + yêu cầu sửa / xóa)
    @Query("SELECT COUNT(s) FROM ReportSnippetEntity s WHERE s.status = 'PENDING' OR s.pendingChange IS NOT NULL")
    long countAwaitingApproval();
}
