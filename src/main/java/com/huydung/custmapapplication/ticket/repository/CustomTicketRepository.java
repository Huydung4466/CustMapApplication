package com.huydung.custmapapplication.ticket.repository;

import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.huydung.custmapapplication.ticket.enums.HistoryTab;
import com.huydung.custmapapplication.ticket.enums.TicketType;

public interface CustomTicketRepository {

    // Ca ĐANG MỞ (chưa lưu trữ). limit <= 0: lấy tất cả; ngược lại lấy 'limit' ca bắt đầu từ vị trí 'offset'
    List<CustomerTicketEntity> findTicketsCustom(TicketFilter filter, boolean sortByAppointment, int offset, int limit);

    // Tổng số ca đang mở thỏa bộ lọc (để tính số trang)
    long countTicketsCustom(TicketFilter filter);

    // --- Thao tác hàng loạt (ADMIN) ---

    // Chỉ lấy ID của các ca đang mở thỏa bộ lọc (nhẹ hơn nhiều so với lấy cả entity)
    List<Long> findTicketIdsCustom(TicketFilter filter);

    // Chuyển các ca đang mở trong danh sách sang Đã xử lý + Lịch sử; trả về số ca thực sự được chuyển
    int markDoneByIds(Collection<Long> ids, LocalDateTime now);

    // Xóa vĩnh viễn các ca; trả về số ca đã xóa
    int deleteByIds(Collection<Long> ids);

    // Ca ĐÃ LƯU TRỮ (các trang Lịch sử), mới lưu trữ nhất lên đầu.
    // tab != null: chỉ lấy ca có lý do lưu trữ thuộc trang đó (đã hoàn thành / không liên lạc được và vấn đề khác); null: lấy tất cả
    List<CustomerTicketEntity> findHistoryTickets(String assignedStaff,
                                                  String keyword,
                                                  LocalDateTime from,
                                                  LocalDateTime toExclusive,
                                                  HistoryTab tab,
                                                  int offset,
                                                  int limit);

    long countHistoryTickets(String assignedStaff,
                             String keyword,
                             LocalDateTime from,
                             LocalDateTime toExclusive,
                             HistoryTab tab);

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
