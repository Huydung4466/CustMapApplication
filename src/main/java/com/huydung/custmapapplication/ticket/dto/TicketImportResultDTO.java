package com.huydung.custmapapplication.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketImportResultDTO {
    private String fileName;
    private int totalRowsRead;
    private int insertedCount;
    private int updatedCount;
    private int archivedCount;   // Số ca chuyển sang Lịch sử sau lần import này
    private int restoredCount;   // Số ca cũ xuất hiện lại trong file nên được đưa về danh sách
    private String message;
}
