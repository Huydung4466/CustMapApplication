package com.huydung.custmapapplication.reportform.dto;

/** Kết quả một thao tác: applied = có hiệu lực ngay; false = đã ghi nhận và đang chờ admin duyệt. */
public record SnippetActionResult(boolean applied, String message, ReportSnippetDTO snippet) {
}
