package com.huydung.custmapapplication.photo.dto;

import java.util.List;

/** Một trang ảnh của trang Ảnh, kèm cấu hình tự xóa để giao diện biết hiện đếm ngược hay không. */
public record PhotoPageDTO(List<TicketPhotoDTO> items, boolean hasMore, boolean autoDeleteEnabled, int retentionHours) {
}
