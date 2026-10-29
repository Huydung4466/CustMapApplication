package com.huydung.custmapapplication.ticket.controller;

import com.huydung.custmapapplication.common.dto.AbstractDTO;
import com.huydung.custmapapplication.ticket.dto.TicketMetaDTO;
import com.huydung.custmapapplication.ticket.dto.TicketResponseDTO;
import com.huydung.custmapapplication.ticket.dto.TicketSearchCriteria;
import com.huydung.custmapapplication.ticket.dto.TicketTypeInfoDTO;
import com.huydung.custmapapplication.ticket.service.TicketService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    // Số khách đã hẹn (hiện trên tab "Đã hẹn" ở thanh dưới)
    @GetMapping("/appointments/count")
    public ResponseEntity<Map<String, Long>> countAppointments() {
        return ResponseEntity.ok(Map.of("count", ticketService.countAppointmentsForCurrentUser()));
    }

    // 3. Danh sách ca đã lưu trữ (các trang Lịch sử): tab = COMPLETED (đã hoàn thành) | ISSUES (không liên lạc được và vấn đề khác)
    @GetMapping("/history")
    public ResponseEntity<AbstractDTO<TicketResponseDTO>> getHistory(
            @RequestParam(value = "tab", required = false) String tab,
            @RequestParam(value = "staffFilter", required = false) String staffFilter,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return ResponseEntity.ok(ticketService.getHistoryForCurrentUser(staffFilter, keyword, fromDate, toDate, tab, page, size));
    }

    // Danh mục trạng thái xử lý, lý do lưu trữ và các trang Lịch sử (sinh từ enum TicketStatus / ArchiveReason / HistoryTab):
    // giao diện dựng hộp "Cập nhật xử lý ca", nhãn trạng thái và nút chuyển trang Lịch sử từ đây
    @GetMapping("/meta")
    public ResponseEntity<TicketMetaDTO> getMeta() {
        return ResponseEntity.ok(TicketMetaDTO.build());
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

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getTicketDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.getTicketDetail(id));
    }
}
