package com.huydung.custmapapplication.ticket.repository;

import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.huydung.custmapapplication.ticket.type.TicketType;

public interface CustomTicketRepository {

    // Ca ĐANG MỞ (chưa lưu trữ). limit <= 0: lấy tất cả; ngược lại lấy 'limit' ca bắt đầu từ vị trí 'offset'
    List<CustomerTicketEntity> findTicketsCustom(TicketFilter filter, boolean sortByAppointment, int offset, int limit);

    // Tổng số ca đang mở thỏa bộ lọc (để tính số trang)
    long countTicketsCustom(TicketFilter filter);

    // Ca ĐÃ LƯU TRỮ (trang Lịch sử), mới lưu trữ nhất lên đầu
    List<CustomerTicketEntity> findHistoryTickets(String assignedStaff,
                                                  String keyword,
                                                  LocalDateTime from,
                                                  LocalDateTime toExclusive,
                                                  int offset,
                                                  int limit);

    long countHistoryTickets(String assignedStaff,
                             String keyword,
                             LocalDateTime from,
                             LocalDateTime toExclusive);

    // Số ca đang mở (chưa hẹn) theo từng loại, trong phạm vi nhân viên (để hiện số trên tab)
    Map<TicketType, Long> countOpenByType(String assignedStaff);

    // Đếm số lượng trùng lặp của từng mã "Thuê bao phản ánh" (d500_gftth_...), chỉ tính ca PHẢN ÁNH
    Map<String, Integer> countDuplicateSubscribers(Collection<String> subscriberAccounts);

    // Lấy danh sách tên các nhân viên có trong cột "Giao nhân viên"
    List<String> findAllDistinctAssignedStaff();

    List<String> findAllDistinctAreas();

    List<String> findAllAreasByStaff(String assignedStaff);

    // 7 ký tự đầu của mã tủ (VD: DLK0412) theo nhân viên và khu vực
    List<String> findAllCabinetPrefixes(String assignedStaff, String area);
}
