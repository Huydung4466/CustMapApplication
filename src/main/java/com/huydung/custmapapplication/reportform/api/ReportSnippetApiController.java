package com.huydung.custmapapplication.reportform.api;

import com.huydung.custmapapplication.reportform.dto.ReportSnippetRequest;
import com.huydung.custmapapplication.reportform.dto.SnippetActionResult;
import com.huydung.custmapapplication.reportform.service.ReportSnippetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API THAY ĐỔI mẫu báo cáo. Nhân viên thêm / sửa / xóa thì chờ admin duyệt; duyệt và từ chối chỉ dành cho admin
 * (quyền được kiểm tra ở service, không chỉ ẩn nút trên giao diện).
 */
@RestController
@RequestMapping("/api/report-snippets")
public class ReportSnippetApiController {

    private final ReportSnippetService snippetService;

    public ReportSnippetApiController(ReportSnippetService snippetService) {
        this.snippetService = snippetService;
    }

    @PostMapping
    public ResponseEntity<SnippetActionResult> create(@RequestBody ReportSnippetRequest request) {
        return ResponseEntity.ok(snippetService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SnippetActionResult> edit(@PathVariable Long id, @RequestBody ReportSnippetRequest request) {
        return ResponseEntity.ok(snippetService.requestEdit(id, request.getContent()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SnippetActionResult> delete(@PathVariable Long id) {
        return ResponseEntity.ok(snippetService.requestDelete(id));
    }

    // Body không bắt buộc: có "content" thì admin chỉnh lại nội dung rồi mới duyệt
    @PostMapping("/{id}/approve")
    public ResponseEntity<SnippetActionResult> approve(@PathVariable Long id,
                                                       @RequestBody(required = false) ReportSnippetRequest request) {
        return ResponseEntity.ok(snippetService.approve(id, request == null ? null : request.getContent()));
    }

    // Body bắt buộc có "reason" (lý do từ chối)
    @PostMapping("/{id}/reject")
    public ResponseEntity<SnippetActionResult> reject(@PathVariable Long id,
                                                      @RequestBody(required = false) ReportSnippetRequest request) {
        return ResponseEntity.ok(snippetService.reject(id, request == null ? null : request.getReason()));
    }
}
