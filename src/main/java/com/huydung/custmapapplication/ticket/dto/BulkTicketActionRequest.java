package com.huydung.custmapapplication.ticket.dto;

import com.huydung.custmapapplication.ticket.enums.BulkAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Yêu cầu thao tác hàng loạt. Có hai cách chọn ca (chỉ dùng MỘT trong hai):
 * <ul>
 *   <li>ids: danh sách ca được tick riêng lẻ;</li>
 *   <li>criteria (+ excludedIds): "chọn tất cả theo bộ lọc" - server tự lấy các ca khớp bộ lọc rồi trừ các ca bỏ tick.</li>
 * </ul>
 * expectedCount là số ca người dùng đang thấy được chọn; server tính lại và từ chối nếu lệch
 * (dữ liệu đã đổi từ lúc người dùng chọn), tránh xóa / xử lý nhầm số ca khác với dự kiến.
 */
@Getter
@Setter
@NoArgsConstructor
public class BulkTicketActionRequest {
    private BulkAction action;
    private List<Long> ids;
    private TicketSearchCriteria criteria;
    private List<Long> excludedIds;
    private Integer expectedCount;
}
