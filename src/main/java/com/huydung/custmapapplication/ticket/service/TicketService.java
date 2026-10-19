package com.huydung.custmapapplication.ticket.service;

import com.huydung.custmapapplication.common.dto.AbstractDTO;
import com.huydung.custmapapplication.ticket.dto.TicketImportResultDTO;
import com.huydung.custmapapplication.ticket.dto.TicketResponseDTO;
import com.huydung.custmapapplication.ticket.dto.TicketSearchCriteria;
import com.huydung.custmapapplication.ticket.dto.UpdateTicketActionRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface TicketService {

    // confirmArchive = true: đồng ý lưu trữ nhiều ca cùng lúc khi file import không còn chứa các ca đó
    TicketImportResultDTO importExcelFile(MultipartFile file, boolean confirmArchive);

    // Danh sách ca đang mở, có phân trang (page bắt đầu từ 1)
    AbstractDTO<TicketResponseDTO> getTicketsForCurrentUser(TicketSearchCriteria criteria);

    // Danh sách ca ĐÃ HẸN (Hẹn giờ / Hẹn khác), xếp theo giờ hẹn gần nhất, có phân trang
    AbstractDTO<TicketResponseDTO> getAppointmentsForCurrentUser(TicketSearchCriteria criteria);

    AbstractDTO<TicketResponseDTO> getHistoryForCurrentUser(String staffFilter, String keyword,
                                                            LocalDate fromDate, LocalDate toDate,
                                                            Integer page, Integer size);

    TicketResponseDTO updateTicketAction(Long ticketId, UpdateTicketActionRequest request);

    // Đưa ca từ Lịch sử về danh sách chính
    TicketResponseDTO restoreTicket(Long ticketId);

    List<String> getAssignedStaffList();

    List<String> getAreaListForStaff(String staffName);

    List<String> getCabinetPrefixList(String staffName, String area);
}
