package com.huydung.custmapapplication.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Thông tin một loại ca cho giao diện: dựng tab, bộ lọc và số lượng ca đang mở. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketTypeInfoDTO {
    private String code;         // PORT_KEM...
    private String label;        // Port kém GPON
    private String group;        // HA_TANG
    private String groupLabel;   // Hạ tầng
    private String groupIcon;    // fa-tower-cell
    private String color;        // warning (màu Bootstrap)
    private long openCount;      // số ca đang mở (chưa hẹn) theo phạm vi nhân viên đang xem
}
