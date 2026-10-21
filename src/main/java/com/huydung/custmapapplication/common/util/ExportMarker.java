package com.huydung.custmapapplication.common.util;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Đánh dấu file Excel do chính hệ thống xuất ra (lưu trong thuộc tính "Từ khóa" của file).
 * Dùng để chặn việc import nhầm file báo cáo: file xuất có bố cục khác file dữ liệu gốc nên nếu import sẽ ghi sai dữ liệu.
 */
public final class ExportMarker {

    public static final String MARKER = "CUSTMAP-EXPORT";

    private ExportMarker() {
    }

    public static void mark(XSSFWorkbook workbook) {
        workbook.getProperties().getCoreProperties().setKeywords(MARKER);
    }

    public static boolean isExported(Workbook workbook) {
        if (!(workbook instanceof XSSFWorkbook)) {
            return false;
        }
        String keywords = ((XSSFWorkbook) workbook).getProperties().getCoreProperties().getKeywords();
        return keywords != null && keywords.contains(MARKER);
    }
}
