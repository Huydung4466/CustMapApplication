package com.huydung.custmapapplication.ticket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Gom các tham số lọc của danh sách ca (query string -> object)
@Getter
@Setter
@NoArgsConstructor
public class TicketSearchCriteria {
    private String staffFilter;
    private String keyword;
    private String appStatus;
    // Lựa chọn ô lọc "Trạng thái" của dashboard (tên một giá trị của TicketListFilter); có giá trị thì thay cho appStatus / onlyPinned
    private String statusFilter;
    private Boolean onlyPinned;
    private String area;
    private String cabinetPrefix;
    private String priority;

    // Lọc theo loại: ticketType (PORT_KEM...) ưu tiên hơn ticketGroup (PHAN_ANH | HA_TANG); bỏ trống cả hai = mọi loại
    private String ticketType;
    private String ticketGroup;

    // DEFAULT (ghim trước, theo ID) | APPOINTMENT (theo giờ hẹn) | NEAREST (gần vị trí hiện tại nhất)
    private String sort;
    private Double lat;
    private Double lng;

    // Phân trang: page bắt đầu từ 1 (mặc định 1), size mặc định 10, tối đa 50
    private Integer page;
    private Integer size;
}
