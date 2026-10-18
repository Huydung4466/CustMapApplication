package com.huydung.custmapapplication.ticket.repository;

// Bộ điều kiện lọc danh sách ca đang mở (assignedStaff đã được xử lý theo quyền người dùng)
public record TicketFilter(String assignedStaff,
                           String keyword,
                           String appStatus,
                           Boolean onlyPinned,
                           String area,
                           String cabinetPrefix,
                           String priority) {
}
