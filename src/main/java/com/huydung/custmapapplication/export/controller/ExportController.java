package com.huydung.custmapapplication.export.controller;

import com.huydung.custmapapplication.export.dto.ExportDownload;
import com.huydung.custmapapplication.export.dto.ExportFileDTO;
import com.huydung.custmapapplication.export.service.ExportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chỉ chứa các API HIỂN THỊ của báo cáo Excel: danh sách file đã xuất và tải file về (ADMIN - chặn trong SecurityConfig).
 * API tạo / xóa file (thay đổi dữ liệu) nằm ở package export.api.
 */
@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    // Danh sách file còn hạn của người dùng hiện tại, kèm số giây còn lại trước khi bị xóa
    @GetMapping
    public ResponseEntity<List<ExportFileDTO>> listMyExports() {
        return ResponseEntity.ok(exportService.listMyExports());
    }

    // Tải file về. File vẫn còn trên hệ thống tới khi hết hạn nên có thể tải lại
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable("id") String id) {
        ExportDownload file = exportService.openDownload(id);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .contentLength(file.size())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(new FileSystemResource(file.path()));
    }
}
