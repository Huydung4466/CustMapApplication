package com.huydung.custmapapplication.ticket.controller;

import com.huydung.custmapapplication.common.dto.AbstractDTO;
import com.huydung.custmapapplication.ticket.dto.TicketResponseDTO;
import com.huydung.custmapapplication.ticket.dto.TicketSearchCriteria;
import com.huydung.custmapapplication.ticket.dto.TicketTypeInfoDTO;
import com.huydung.custmapapplication.ticket.service.TicketService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Chỉ chứa các API HIỂN THỊ (đọc dữ liệu) của ca phản ánh.
 * API thay đổi dữ liệu (import, cập nhật xử lý, đưa về danh sách) nằm ở package ticket.api.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // 1. Danh sách ca đang cần xử lý (KHÔNG gồm ca đã hẹn), phân trang bằng page (từ 1) và size
    //    (lọc theo nhân viên, từ khóa, trạng thái, ghim, khu vực, đầu mã tủ, ưu tiên;
    //    sort = DEFAULT | APPOINTMENT | NEAREST (kèm lat, lng))
    @GetMapping
    public ResponseEntity<AbstractDTO<TicketResponseDTO>> getTickets(TicketSearchCriteria criteria) {
        return ResponseEntity.ok(ticketService.getTicketsForCurrentUser(criteria));
    }

    // 2. Danh sách ca ĐÃ HẸN (trạng thái HEN_GIO / HEN_KHAC), xếp theo giờ hẹn gần nhất; appStatus = HEN_GIO | HEN_KHAC để lọc riêng
    @GetMapping("/appointments")
    public ResponseEntity<AbstractDTO<TicketResponseDTO>> getAppointments(TicketSearchCriteria criteria) {
        return ResponseEntity.ok(ticketService.getAppointmentsForCurrentUser(criteria));
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

    // Các loại ca (PHAN_ANH, PORT_KEM...) kèm số ca đang mở: giao diện dựng tab và bộ lọc loại từ đây
    @GetMapping("/types")
    public ResponseEntity<List<TicketTypeInfoDTO>> getTicketTypes(
            @RequestParam(value = "staffFilter", required = false) String staffFilter) {
        return ResponseEntity.ok(ticketService.getTicketTypeSummary(staffFilter));
    }

    // 4. Danh sách nhân viên địa bàn có trong dữ liệu Excel (chỉ ADMIN - chặn trong SecurityConfig)
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
