package com.huydung.custmapapplication.ticket.importer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Dòng tiêu đề của sheet: cho phép tìm cột theo TÊN thay vì theo vị trí,
 * nên file đổi thứ tự cột hoặc chèn thêm cột vẫn đọc đúng.
 * Tên được so khớp không phân biệt hoa thường và bỏ khoảng trắng thừa.
 */
public final class ExcelHeader {

    private final Map<String, Integer> indexByName = new HashMap<>();

    private ExcelHeader() {
    }

    public static ExcelHeader from(Row headerRow, DataFormatter formatter) {
        ExcelHeader h = new ExcelHeader();
        if (headerRow == null) return h;
        for (Cell cell : headerRow) {
            String name = normalize(formatter.formatCellValue(cell));
            if (!name.isEmpty()) {
                h.indexByName.putIfAbsent(name, cell.getColumnIndex());
            }
        }
        return h;
    }

    /** Vị trí cột (từ 0), hoặc -1 nếu file không có cột này. */
    public int indexOf(String headerName) {
        return indexByName.getOrDefault(normalize(headerName), -1);
    }

    public boolean has(String headerName) {
        return indexOf(headerName) >= 0;
    }

    static String normalize(String s) {
        return s == null ? "" : s.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
