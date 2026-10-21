package com.huydung.custmapapplication.ticket.api;

import com.huydung.custmapapplication.ticket.dto.BulkActionResultDTO;
import com.huydung.custmapapplication.ticket.dto.BulkTicketActionRequest;
import com.huydung.custmapapplication.ticket.dto.TicketImportResultDTO;
import com.huydung.custmapapplication.ticket.dto.TicketResponseDTO;
import com.huydung.custmapapplication.ticket.dto.UpdateTicketActionRequest;
import com.huydung.custmapapplication.ticket.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Các API THAY ĐỔI dữ liệu của ca phản ánh (import, cập nhật xử lý, đưa về danh sách chính). */
@RestController
@RequestMapping("/api/tickets")
public class TicketApiController {

    private final TicketService ticketService;

    public TicketApiController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // 1. Upload và bóc tách file Excel (.xlsx) vào Database (chỉ ADMIN - chặn trong SecurityConfig)
    //    confirmArchive=true: đồng ý chuyển nhiều ca sang Lịch sử khi file không còn chứa các ca đó
    @PostMapping("/import")
    public ResponseEntity<TicketImportResultDTO> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmArchive", defaultValue = "false") boolean confirmArchive) {
        return ResponseEntity.ok(ticketService.importExcelFile(file, confirmArchive));
    }

    // 2. Cập nhật trạng thái ca (Hẹn giờ, KLL, Hẹn khác, Đã ghim, Xử lý ca)
    @PutMapping("/{id}/action")
    public ResponseEntity<TicketResponseDTO> updateAction(
            @PathVariable("id") Long id,
            @RequestBody UpdateTicketActionRequest request) {
        return ResponseEntity.ok(ticketService.updateTicketAction(id, request));
    }

    // 3. Thao tác hàng loạt (chỉ ADMIN - chặn trong SecurityConfig và kiểm tra lại trong service):
    //    action = DONE (chuyển Đã xử lý) | DELETE (xóa vĩnh viễn kèm ảnh)
    //    chọn ca bằng ids HOẶC criteria (+ excludedIds), kèm expectedCount để đối chiếu
    @PostMapping("/bulk")
    public ResponseEntity<BulkActionResultDTO> bulkAction(@RequestBody BulkTicketActionRequest request) {
        return ResponseEntity.ok(ticketService.bulkAction(request));
    }

    // 4. Đưa ca từ Lịch sử về danh sách chính
    @PutMapping("/{id}/restore")
    public ResponseEntity<TicketResponseDTO> restore(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.restoreTicket(id));
    }
}
