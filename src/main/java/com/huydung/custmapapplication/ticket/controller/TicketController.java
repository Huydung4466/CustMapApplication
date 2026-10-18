package com.huydung.custmapapplication.ticket.controller;

import com.huydung.custmapapplication.common.dto.AbstractDTO;
import com.huydung.custmapapplication.ticket.dto.TicketImportResultDTO;
import com.huydung.custmapapplication.ticket.dto.TicketResponseDTO;
import com.huydung.custmapapplication.ticket.dto.TicketSearchCriteria;
import com.huydung.custmapapplication.ticket.dto.UpdateTicketActionRequest;
import com.huydung.custmapapplication.ticket.service.TicketService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
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

    // 2. Danh sách ca đang mở, phân trang bằng page (từ 1) và size (lọc theo nhân viên, từ khóa, trạng thái, ghim, khu vực, đầu mã tủ, ưu tiên;
    //    sort = DEFAULT | APPOINTMENT | NEAREST (kèm lat, lng))
    @GetMapping
    public ResponseEntity<AbstractDTO<TicketResponseDTO>> getTickets(TicketSearchCriteria criteria) {
        return ResponseEntity.ok(ticketService.getTicketsForCurrentUser(criteria));
    }

    // 3. Danh sách ca đã làm / đã lưu trữ (trang Lịch sử)
    @GetMapping("/history")
    public ResponseEntity<AbstractDTO<TicketResponseDTO>> getHistory(
            @RequestParam(value = "staffFilter", required = false) String staffFilter,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return ResponseEntity.ok(ticketService.getHistoryForCurrentUser(staffFilter, keyword, fromDate, toDate, page, size));
    }

    // 4. Cập nhật trạng thái ca (Hẹn giờ, KLL, Hẹn khác, Đã ghim, Xử lý ca)
    @PutMapping("/{id}/action")
    public ResponseEntity<TicketResponseDTO> updateAction(
            @PathVariable("id") Long id,
            @RequestBody UpdateTicketActionRequest request) {
        return ResponseEntity.ok(ticketService.updateTicketAction(id, request));
    }

    // 5. Đưa ca từ Lịch sử về danh sách chính
    @PutMapping("/{id}/restore")
    public ResponseEntity<TicketResponseDTO> restore(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.restoreTicket(id));
    }

    // 6. Danh sách nhân viên địa bàn có trong dữ liệu Excel (chỉ ADMIN - chặn trong SecurityConfig)
    @GetMapping("/staffs")
    public ResponseEntity<List<String>> getStaffList() {
        return ResponseEntity.ok(ticketService.getAssignedStaffList());
    }

    @GetMapping("/areas")
    public ResponseEntity<List<String>> getAreaList(@RequestParam(value = "staffName", required = false) String staffName) {
        return ResponseEntity.ok(ticketService.getAreaListForStaff(staffName));
    }

    @GetMapping("/cabinet-prefixes")
    public ResponseEntity<List<String>> getCabinetPrefixes(
            @RequestParam(value = "staffName", required = false) String staffName,
            @RequestParam(value = "area", required = false) String area) {
        return ResponseEntity.ok(ticketService.getCabinetPrefixList(staffName, area));
    }
}
