package com.huydung.custmapapplication.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

// Kế thừa ResponseEntityExceptionHandler để các lỗi chuẩn của Spring MVC
// (thiếu tham số, JSON sai, method không hỗ trợ, file quá lớn...) vẫn giữ mã lỗi đúng
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // Import sắp lưu trữ rất nhiều ca: trả 409 để giao diện hỏi xác nhận rồi gửi lại
    @ExceptionHandler(ImportConfirmationRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleConfirmArchive(ImportConfirmationRequiredException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        body.put("code", "CONFIRM_ARCHIVE");
        body.put("archiveCount", ex.getArchiveCount());
        body.put("activeCount", ex.getActiveCount());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // Lỗi đọc file import (do mình chủ động ném) - hiện nguyên thông báo cho người dùng
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleImportError(IllegalStateException ex) {
        log.warn("Lỗi xử lý dữ liệu: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // Lỗi bất ngờ - ghi log đầy đủ, chỉ trả thông báo chung ra ngoài
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Lỗi không mong muốn", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message == null ? "Có lỗi xảy ra" : message);
        return ResponseEntity.status(status).body(body);
    }
}
