package com.huydung.custmapapplication.ticket.importer;

import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import org.apache.poi.ss.usermodel.Row;

import java.util.List;

/** Đọc từng dòng của một file đã được nhận diện định dạng (gắn với tiêu đề của file đó). */
public interface TicketRowReader {

    /** Loại + mã của dòng; null nếu dòng trống hoặc không đọc được (dòng bị bỏ qua và được ghi vào cảnh báo). */
    RowKey readKey(Row row);

    /** Ghi dữ liệu của dòng vào entity (entity mới hoặc ca đã có). */
    void fill(Row row, RowKey key, CustomerTicketEntity entity);

    /** Cảnh báo gom được sau khi đọc xong (cột thiếu, dòng bị bỏ qua...) để báo lại cho người import. */
    List<String> warnings();
}
