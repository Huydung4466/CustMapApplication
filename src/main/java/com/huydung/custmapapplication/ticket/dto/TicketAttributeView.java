package com.huydung.custmapapplication.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một dòng thuộc tính đã sẵn sàng hiển thị trên thẻ (nhãn + giá trị + đơn vị). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketAttributeView {
    private String label;
    private String value;
    private String unit;
}
