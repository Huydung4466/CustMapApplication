package com.huydung.custmapapplication.ticket.repository;

// Bộ điều kiện lọc danh sách ca đang mở (assignedStaff đã được xử lý theo quyền người dùng).
// appointed = true: chỉ lấy ca ĐÃ HẸN (HEN_GIO / HEN_KHAC); false: lấy ca còn lại (danh sách chính, không gồm ca đã hẹn)
public record TicketFilter(String assignedStaff,
                           String keyword,
                           String appStatus,
                           Boolean onlyPinned,
                           String area,
                           String cabinetPrefix,
                           String priority,
                           boolean appointed) {
}
