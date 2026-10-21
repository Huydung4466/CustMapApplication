package com.huydung.custmapapplication.ticket.importer;

import com.huydung.custmapapplication.constant.ComplaintExcelConstant;
import com.huydung.custmapapplication.ticket.converter.TicketConverter;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.enums.TicketType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * File phản ánh khách hàng (nhận diện bằng cột "Mã phản ánh").
 * Giữ nguyên cách đọc theo vị trí cột có sẵn trong TicketConverter để không ảnh hưởng dữ liệu cũ.
 */
@Component
public class ComplaintFileFormat implements TicketFileFormat {

    private final TicketConverter converter;

    public ComplaintFileFormat(TicketConverter converter) {
        this.converter = converter;
    }

    @Override
    public String name() {
        return "Phản ánh khách hàng";
    }

    @Override
    public boolean supports(ExcelHeader header) {
        return header.has(ComplaintExcelConstant.HEADER_REPORT_CODE);
    }

    @Override
    public Set<TicketType> coveredTypes() {
        return Set.of(TicketType.PHAN_ANH);
    }

    @Override
    public TicketRowReader open(ExcelHeader header, DataFormatter formatter) {
        return new TicketRowReader() {
            @Override
            public RowKey readKey(Row row) {
                String code = converter.extractTicketCode(row, formatter);
                return code.isEmpty() ? null : new RowKey(TicketType.PHAN_ANH, code);
            }

            @Override
            public void fill(Row row, RowKey key, CustomerTicketEntity entity) {
                entity.setTicketType(TicketType.PHAN_ANH);
                converter.mapExcelRowToEntity(row, entity, formatter);
            }

            @Override
            public List<String> warnings() {
                return List.of();
            }
        };
    }
}
