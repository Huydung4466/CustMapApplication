package com.huydung.custmapapplication.ticket.importer;

import com.huydung.custmapapplication.ticket.enums.TicketType;
import org.apache.poi.ss.usermodel.DataFormatter;

import java.util.Set;

/**
 * Một định dạng file import. Mỗi định dạng là một Spring bean, service tự duyệt tất cả bean để nhận diện file
 * từ dòng tiêu đề, nên thêm định dạng mới chỉ cần thêm một class implements interface này.
 */
public interface TicketFileFormat {

    /** Tên hiển thị trong thông báo import. */
    String name();

    /** File có phải định dạng này không (dựa vào tên các cột tiêu đề). */
    boolean supports(ExcelHeader header);

    /**
     * Các loại ca mà file định dạng này bao phủ. Ca đang mở thuộc các loại này mà không còn trong file
     * mới là ca bị lưu trữ; ca của loại khác không bị đụng tới.
     */
    Set<TicketType> coveredTypes();

    /** Tạo bộ đọc dòng. Ném IllegalArgumentException nếu file thiếu cột bắt buộc. */
    TicketRowReader open(ExcelHeader header, DataFormatter formatter);
}
