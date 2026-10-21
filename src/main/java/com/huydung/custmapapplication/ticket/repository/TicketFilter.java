package com.huydung.custmapapplication.ticket.repository;

import com.huydung.custmapapplication.ticket.type.TicketType;

import java.util.Collection;

// Bộ điều kiện lọc danh sách ca đang mở (assignedStaff đã được xử lý theo quyền người dùng).
// types: chỉ lấy các loại này; null hoặc rỗng = mọi loại
// appointed = true: chỉ lấy ca ĐÃ HẸN (HEN_GIO / HEN_KHAC); false: lấy ca còn lại (danh sách chính, không gồm ca đã hẹn)
public record TicketFilter(String assignedStaff,
                           String keyword,
                           String appStatus,
                           Boolean onlyPinned,
                           String area,
                           String cabinetPrefix,
                           String priority,
                           boolean appointed,
                           Collection<TicketType> types) {
}
