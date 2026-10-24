package com.huydung.custmapapplication.constant;

/**
 * Tên sheet trong các file Excel import.
 */
public final class ExcelSheetConstant {

    private ExcelSheetConstant() {
    }

    /** Sheet chứa dữ liệu chi tiết trong file dữ liệu gốc (tên đúng như trong file, viết "tiêt" không phải "tiết"). */
    public static final String DETAIL_SHEET = "chi tiêt";

    /** Cách viết đúng chính tả của tên sheet trên, phòng khi file được đổi tên. */
    public static final String DETAIL_SHEET_ALT = "chi tiết";
}
