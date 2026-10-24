package com.huydung.custmapapplication.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionResultDTO {
    private String action;   // DONE | DELETE
    private int requested;   // số ca được chọn
    private int affected;    // số ca thực sự bị tác động
    private String message;
}
