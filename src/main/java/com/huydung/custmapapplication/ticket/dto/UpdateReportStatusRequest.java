package com.huydung.custmapapplication.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReportStatusRequest {
    private String status; // tên một giá trị của ReportStatus (NOT_SUBMITTED, OPENED, SUBMITTED)
}
