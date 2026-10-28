package com.huydung.custmapapplication.reportform.dto;

import java.time.LocalDateTime;

/** Một dòng lịch sử của mẫu; resultState* là trạng thái của mẫu sau thao tác (SnippetState). */
public record SnippetHistoryDTO(
        Long id,
        Long snippetId,
        String fieldLabel,
        String taskGroupLabel,
        String action,
        String actionLabel,
        String actionIcon,
        String resultState,
        String resultStateLabel,
        String resultStateBadge,
        String actorName,
        String contentBefore,
        String contentAfter,
        String note,
        LocalDateTime createdAt) {
}
