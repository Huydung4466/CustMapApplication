package com.huydung.custmapapplication.ticket.importer;

import com.huydung.custmapapplication.ticket.enums.TicketType;

/** Loại và mã (khóa) của một dòng Excel. */
public record RowKey(TicketType type, String code) {
}
