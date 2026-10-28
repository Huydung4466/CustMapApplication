package com.huydung.custmapapplication.reportform.controller;

import com.huydung.custmapapplication.reportform.dto.ReportFormMetaDTO;
import com.huydung.custmapapplication.reportform.dto.ReportSnippetDTO;
import com.huydung.custmapapplication.reportform.dto.SnippetHistoryDTO;
import com.huydung.custmapapplication.reportform.service.ReportSnippetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Chỉ chứa các API ĐỌC của chức năng nộp báo cáo. API thêm / sửa / xóa / duyệt mẫu nằm ở package reportform.api.
 */
@RestController
@RequestMapping("/api")
public class ReportFormController {

    private final ReportSnippetService snippetService;

    public ReportFormController(ReportSnippetService snippetService) {
        this.snippetService = snippetService;
    }

    // Cấu hình form: địa chỉ, nhóm nhiệm vụ, 4 mục viết, nhóm gợi ý theo loại ca, các trạng thái của mẫu
    @GetMapping("/report-form/meta")
    public ResponseEntity<ReportFormMetaDTO> getMeta() {
        return ResponseEntity.ok(snippetService.getMeta());
    }

    // Mẫu dùng được cho một mục (CONTENT / ERROR / WARNING / PROPOSAL): đã duyệt + mẫu chính mình đang chờ duyệt
    @GetMapping("/report-snippets")
    public ResponseEntity<List<ReportSnippetDTO>> listUsable(@RequestParam("field") String field) {
        return ResponseEntity.ok(snippetService.listUsable(field));
    }

    // Trang quản lý /report-snippets: admin thấy tất cả; nhân viên thấy mẫu đã duyệt + mẫu / yêu cầu của chính mình
    @GetMapping("/report-snippets/manage")
    public ResponseEntity<List<ReportSnippetDTO>> listForManage() {
        return ResponseEntity.ok(snippetService.listForManage());
    }

    // Chỉ admin (kiểm tra trong service): toàn bộ mẫu kể cả đang chờ duyệt
    @GetMapping("/report-snippets/admin")
    public ResponseEntity<List<ReportSnippetDTO>> listAllForAdmin() {
        return ResponseEntity.ok(snippetService.listAllForAdmin());
    }

    // Chỉ admin: số yêu cầu đang chờ duyệt (hiện trên nút ở trang cá nhân)
    @GetMapping("/report-snippets/pending-count")
    public ResponseEntity<Map<String, Long>> pendingCount() {
        return ResponseEntity.ok(Map.of("count", snippetService.countAwaitingApproval()));
    }

    // Chỉ admin: nhật ký thao tác gần đây trên mọi mẫu
    @GetMapping("/report-snippets/history")
    public ResponseEntity<List<SnippetHistoryDTO>> recentHistory(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return ResponseEntity.ok(snippetService.listRecentHistory(limit));
    }

    // Lịch sử một mẫu: admin xem mọi mẫu, nhân viên chỉ xem mẫu do mình tạo
    @GetMapping("/report-snippets/{id}/history")
    public ResponseEntity<List<SnippetHistoryDTO>> history(@PathVariable Long id) {
        return ResponseEntity.ok(snippetService.getHistory(id));
    }
}
