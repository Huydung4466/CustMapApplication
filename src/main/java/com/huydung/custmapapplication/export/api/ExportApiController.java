package com.huydung.custmapapplication.export.api;

import com.huydung.custmapapplication.export.dto.ExportFileDTO;
import com.huydung.custmapapplication.export.dto.ExportRequest;
import com.huydung.custmapapplication.export.service.ExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** API THAY ĐỔI dữ liệu của báo cáo Excel: tạo file mới và xóa sớm (ADMIN - chặn trong SecurityConfig). */
@RestController
@RequestMapping("/api/exports")
public class ExportApiController {

    private final ExportService exportService;

    public ExportApiController(ExportService exportService) {
        this.exportService = exportService;
    }

    // Tạo file Excel báo cáo mới (3 sheet, tô màu theo trạng thái)
    @PostMapping("/tickets")
    public ResponseEntity<ExportFileDTO> exportTickets(@RequestBody(required = false) ExportRequest request) {
        return ResponseEntity.ok(exportService.exportTickets(request));
    }

    // Xóa sớm một file đã xuất
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable("id") String id) {
        exportService.deleteExport(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa file"));
    }
}
