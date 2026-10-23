package com.huydung.custmapapplication.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketActionRequest {
    private String appStatus;       // tên một giá trị của TicketStatus (DANG_XU_LY, HEN_GIO, HEN_KHAC, KLL, DA_XU_LY)
    private String appointmentTime; // Chuỗi hiển thị giờ hẹn (server tự tạo nếu có appointmentStart)
    private String callNote;        // VD: "Đã chốt hẹn lúc 14:00 - 15:00 (gọi lúc 09:39)"
    private Boolean isPinned;       // true / false

    private LocalDateTime appointmentStart; // VD: "2026-10-18T14:00"
    private LocalDateTime appointmentEnd;
    private Boolean clearAppointment;       // true: xóa giờ hẹn đã đặt
}
