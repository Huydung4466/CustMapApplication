package com.huydung.custmapapplication.reportform.repository;

import com.huydung.custmapapplication.reportform.entity.ReportSnippetHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportSnippetHistoryRepository extends JpaRepository<ReportSnippetHistoryEntity, Long> {

    List<ReportSnippetHistoryEntity> findBySnippetIdOrderByCreatedAtDescIdDesc(Long snippetId);

    // Nhật ký gần đây cho admin
    List<ReportSnippetHistoryEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
