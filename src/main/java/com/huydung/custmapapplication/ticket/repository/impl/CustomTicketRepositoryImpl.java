package com.huydung.custmapapplication.ticket.repository.impl;

import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.repository.CustomTicketRepository;
import com.huydung.custmapapplication.ticket.repository.TicketFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.huydung.custmapapplication.ticket.duplicate.DuplicateRule;
import com.huydung.custmapapplication.ticket.enums.HistoryTab;
import com.huydung.custmapapplication.ticket.enums.TicketStatus;
import com.huydung.custmapapplication.ticket.enums.TicketType;

@Repository
public class CustomTicketRepositoryImpl implements CustomTicketRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // Điều kiện WHERE + tham số, dùng chung cho câu lấy danh sách và câu đếm
    private static class QueryParts {
        final StringBuilder where;
        final Map<String, Object> params = new HashMap<>();

        QueryParts(String baseWhere) {
            this.where = new StringBuilder(baseWhere);
        }
    }

    private QueryParts buildOpenTicketWhere(TicketFilter f) {
        QueryParts q = buildOpenTicketConditions(f);
        if (f.collapseDuplicates()) {
            appendCollapseDuplicates(q);
        }
        return q;
    }

    // Gộp trùng: với các loại ca bị gộp, chỉ giữ ca có ID nhỏ nhất trong nhóm (cùng loại + cùng account) trong số các ca
    // thỏa CÙNG bộ lọc. Nhờ vậy lọc theo từ khóa / trạng thái... vẫn thấy nhóm nếu có ít nhất một ca khớp.
    // Ca không có account không bị gộp. Dùng chung điều kiện lọc nên danh sách, số trang và chọn hàng loạt luôn khớp nhau.
    private void appendCollapseDuplicates(QueryParts q) {
        List<TicketType> collapsing = DuplicateRule.collapsingTypes();
        if (collapsing.isEmpty()) return;

        String inner = withAlias(q.where.toString(), "t2");
        q.where.append("AND (t.ticketType IS NULL OR t.ticketType NOT IN :collapseTypes ")
                .append("OR t.subscriberAccount IS NULL OR TRIM(t.subscriberAccount) = '' ")
                .append("OR t.id = (SELECT MIN(t2.id) FROM CustomerTicketEntity t2 ").append(inner)
                .append("AND t2.ticketType = t.ticketType AND t2.subscriberAccount = t.subscriberAccount)) ");
        q.params.put("collapseTypes", collapsing);
    }

    // Chỉ lấy ca có account trùng với ít nhất một ca khác CÙNG loại. Cùng quy tắc đếm với nhãn "Trùng xN"
    // (xem DuplicateRule / countDuplicateSubscribers): nhóm gộp trùng (Hạ tầng) chỉ tính ca đang mở,
    // nhóm không gộp (Phản ánh) tính cả ca đã lưu trữ.
    private void appendOnlyDuplicates(QueryParts q) {
        q.where.append("AND t.subscriberAccount IS NOT NULL AND TRIM(t.subscriberAccount) <> '' ")
                .append("AND EXISTS (SELECT 1 FROM CustomerTicketEntity d WHERE d.subscriberAccount = t.subscriberAccount ")
                .append("AND d.id <> t.id AND (d.ticketType = t.ticketType OR (d.ticketType IS NULL AND t.ticketType IS NULL)) ");
        List<TicketType> collapsing = DuplicateRule.collapsingTypes();
        if (!collapsing.isEmpty()) {
            q.where.append("AND (d.archivedAt IS NULL OR t.ticketType IS NULL OR t.ticketType NOT IN :collapseTypes) ");
            q.params.put("collapseTypes", collapsing);
        }
        q.where.append(") ");
    }

    // Đổi bí danh "t." trong chuỗi điều kiện thành bí danh khác (dùng cho truy vấn con); \\b để không đụng chữ khác
    private static String withAlias(String jpql, String alias) {
        return jpql.replaceAll("\\bt\\.(?=[a-zA-Z])", alias + ".");
    }

    private QueryParts buildOpenTicketConditions(TicketFilter f) {
        // Chỉ lấy ca đang mở (chưa nằm trong Lịch sử)
        QueryParts q = new QueryParts("WHERE t.archivedAt IS NULL ");

        appendStaffFilter(q.where, q.params, f.assignedStaff());

        // Ca đã hẹn (Hẹn giờ / Hẹn khác) chuyển sang trang "Đã hẹn", không hiện ở danh sách chính
        if (f.appointed()) {
            q.where.append("AND t.appStatus IN :appointed ");
        } else {
            q.where.append("AND (t.appStatus IS NULL OR t.appStatus NOT IN :appointed) ");
        }
        q.params.put("appointed", TicketStatus.appointedCodes());

        if (f.keyword() != null && !f.keyword().trim().isEmpty()) {
            q.where.append("""
                    AND (LOWER(t.customerName) LIKE LOWER(:kw)
                      OR LOWER(t.subscriberAccount) LIKE LOWER(:kw)
                      OR LOWER(t.contactPhone) LIKE LOWER(:kw)
                      OR LOWER(t.reportPhone) LIKE LOWER(:kw)
                      OR LOWER(t.installAddress) LIKE LOWER(:kw)
                      OR LOWER(t.ticketCode) LIKE LOWER(:kw)
                      OR LOWER(t.cabinetCode) LIKE LOWER(:kw)
                      OR LOWER(t.techStaffName) LIKE LOWER(:kw))
                    """);
            q.params.put("kw", "%" + f.keyword().trim() + "%");
        }

        // Lọc theo loại ca (dữ liệu cũ chưa có loại coi như PHAN_ANH)
        if (f.types() != null && !f.types().isEmpty()) {
            if (f.types().contains(TicketType.PHAN_ANH)) {
                q.where.append("AND (t.ticketType IN :types OR t.ticketType IS NULL) ");
            } else {
                q.where.append("AND t.ticketType IN :types ");
            }
            q.params.put("types", f.types());
        }

        // Lọc theo Khu vực
        if (f.area() != null && !f.area().trim().isEmpty()) {
            q.where.append("AND UPPER(TRIM(t.area)) = UPPER(TRIM(:area)) ");
            q.params.put("area", f.area().trim());
        }

        // Lọc theo Đầu mã tủ (VD: DLK0412): mã tủ bắt đầu bằng chuỗi này
        if (f.cabinetPrefix() != null && !f.cabinetPrefix().trim().isEmpty()) {
            q.where.append("AND UPPER(TRIM(t.cabinetCode)) LIKE UPPER(:cabinetPrefix) ");
            q.params.put("cabinetPrefix", f.cabinetPrefix().trim() + "%");
        }

        // Lọc theo Mức độ ưu tiên
        if (f.priority() != null && !f.priority().trim().isEmpty()) {
            q.where.append("AND UPPER(TRIM(t.priorityLevel)) = UPPER(TRIM(:priority)) ");
            q.params.put("priority", f.priority().trim());
        }

        if (f.appStatus() != null && !f.appStatus().trim().isEmpty() && !"ALL".equalsIgnoreCase(f.appStatus().trim())) {
            q.where.append("AND t.appStatus = :appStatus ");
            q.params.put("appStatus", f.appStatus().trim());
        }

        if (Boolean.TRUE.equals(f.onlyPinned())) {
            q.where.append("AND t.isPinned = true ");
        }

        if (f.onlyDuplicates()) {
            appendOnlyDuplicates(q);
        }
        return q;
    }

    private QueryParts buildHistoryWhere(String assignedStaff, String keyword, LocalDateTime from, LocalDateTime toExclusive,
                                         HistoryTab tab) {
        QueryParts q = new QueryParts("WHERE t.archivedAt IS NOT NULL ");

        appendStaffFilter(q.where, q.params, assignedStaff);

        if (keyword != null && !keyword.trim().isEmpty()) {
            q.where.append("""
                    AND (LOWER(t.customerName) LIKE LOWER(:kw)
                      OR LOWER(t.subscriberAccount) LIKE LOWER(:kw)
                      OR LOWER(t.contactPhone) LIKE LOWER(:kw)
                      OR LOWER(t.reportPhone) LIKE LOWER(:kw)
                      OR LOWER(t.installAddress) LIKE LOWER(:kw)
                      OR LOWER(t.ticketCode) LIKE LOWER(:kw))
                    """);
            q.params.put("kw", "%" + keyword.trim() + "%");
        }
        // Lọc theo trang Lịch sử: chỉ lấy ca có lý do lưu trữ thuộc trang đó
        if (tab != null) {
            if (tab.isIncludesUnknownReason()) {
                // Ca lưu trữ từ dữ liệu cũ chưa ghi lý do coi như đã hoàn thành
                q.where.append("AND (t.archiveReason IN :reasons OR t.archiveReason IS NULL) ");
            } else {
                q.where.append("AND t.archiveReason IN :reasons ");
            }
            q.params.put("reasons", tab.reasonCodes());
        }
        if (from != null) {
            q.where.append("AND t.archivedAt >= :fromTime ");
            q.params.put("fromTime", from);
        }
        if (toExclusive != null) {
            q.where.append("AND t.archivedAt < :toTime ");
            q.params.put("toTime", toExclusive);
        }
        return q;
    }

    @Override
    public List<CustomerTicketEntity> findTicketsCustom(TicketFilter filter, boolean sortByAppointment, int offset, int limit) {
        QueryParts q = buildOpenTicketWhere(filter);

        // Ca đã ghim luôn lên đầu; t.id ở cuối để thứ tự ổn định khi phân trang
        String orderBy = sortByAppointment
                ? "ORDER BY COALESCE(t.isPinned, false) DESC, CASE WHEN t.appointmentStart IS NULL THEN 1 ELSE 0 END ASC, t.appointmentStart ASC, t.id ASC"
                : "ORDER BY COALESCE(t.isPinned, false) DESC, t.id ASC";

        TypedQuery<CustomerTicketEntity> query = entityManager.createQuery(
                "SELECT t FROM CustomerTicketEntity t " + q.where + orderBy, CustomerTicketEntity.class);
        q.params.forEach(query::setParameter);
        if (limit > 0) {
            query.setFirstResult(offset);
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    @Override
    public List<Long> findTicketIdsCustom(TicketFilter filter) {
        QueryParts q = buildOpenTicketWhere(filter);
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT t.id FROM CustomerTicketEntity t " + q.where + "ORDER BY t.id ASC", Long.class);
        q.params.forEach(query::setParameter);
        return query.getResultList();
    }

    // updated_at phải gán tay vì câu UPDATE hàng loạt không qua @PreUpdate của entity
    @Override
    public int markDoneByIds(Collection<Long> ids, LocalDateTime now) {
        if (ids == null || ids.isEmpty()) return 0;
        return entityManager.createQuery("""
                        UPDATE CustomerTicketEntity t
                        SET t.appStatus = :doneStatus, t.archivedAt = :now, t.archiveReason = :doneReason, t.updatedAt = :now
                        WHERE t.id IN :ids AND t.archivedAt IS NULL
                        """)
                .setParameter("doneStatus", TicketStatus.DA_XU_LY.name())
                .setParameter("doneReason", TicketStatus.DA_XU_LY.getArchiveReason().name())
                .setParameter("now", now)
                .setParameter("ids", ids)
                .executeUpdate();
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return entityManager.createQuery("DELETE FROM CustomerTicketEntity t WHERE t.id IN :ids")
                .setParameter("ids", ids)
                .executeUpdate();
    }

    @Override
    public long countTicketsCustom(TicketFilter filter) {
        QueryParts q = buildOpenTicketWhere(filter);
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(t) FROM CustomerTicketEntity t " + q.where, Long.class);
        q.params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    @Override
    public List<CustomerTicketEntity> findHistoryTickets(String assignedStaff, String keyword,
                                                         LocalDateTime from, LocalDateTime toExclusive,
                                                         HistoryTab tab,
                                                         int offset, int limit) {
        QueryParts q = buildHistoryWhere(assignedStaff, keyword, from, toExclusive, tab);
        TypedQuery<CustomerTicketEntity> query = entityManager.createQuery(
                "SELECT t FROM CustomerTicketEntity t " + q.where + "ORDER BY t.archivedAt DESC, t.id DESC",
                CustomerTicketEntity.class);
        q.params.forEach(query::setParameter);
        if (limit > 0) {
            query.setFirstResult(offset);
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    @Override
    public long countHistoryTickets(String assignedStaff, String keyword, LocalDateTime from, LocalDateTime toExclusive,
                                    HistoryTab tab) {
        QueryParts q = buildHistoryWhere(assignedStaff, keyword, from, toExclusive, tab);
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(t) FROM CustomerTicketEntity t " + q.where, Long.class);
        q.params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    @Override
    public Map<TicketType, Long> countOpenByType(String assignedStaff) {
        StringBuilder jpql = new StringBuilder(
                "SELECT t.ticketType, COUNT(t) FROM CustomerTicketEntity t WHERE t.archivedAt IS NULL "
                        + "AND (t.appStatus IS NULL OR t.appStatus NOT IN :appointed) ");
        Map<String, Object> params = new HashMap<>();
        params.put("appointed", TicketStatus.appointedCodes());
        appendStaffFilter(jpql, params, assignedStaff);
        jpql.append("GROUP BY t.ticketType");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);

        Map<TicketType, Long> result = new EnumMap<>(TicketType.class);
        for (Object[] row : query.getResultList()) {
            // Dòng cũ chưa có loại tính vào PHAN_ANH
            TicketType type = row[0] == null ? TicketType.PHAN_ANH : (TicketType) row[0];
            result.merge(type, ((Number) row[1]).longValue(), Long::sum);
        }

        // Loại bị gộp trùng: số trên tab = số thẻ thực sự hiện ra (mỗi account một thẻ, ca không có account tính riêng)
        List<TicketType> collapsing = DuplicateRule.collapsingTypes();
        if (!collapsing.isEmpty()) {
            Map<TicketType, Long> collapsed = new EnumMap<>(TicketType.class);
            collapseCount(collapsed, assignedStaff, collapsing, true);
            collapseCount(collapsed, assignedStaff, collapsing, false);
            for (TicketType t : collapsing) {
                result.put(t, collapsed.getOrDefault(t, 0L));
            }
        }
        return result;
    }

    // distinctAccounts = true: đếm số account khác nhau; false: đếm các ca không có account (không gộp được)
    private void collapseCount(Map<TicketType, Long> out, String assignedStaff, List<TicketType> types, boolean distinctAccounts) {
        StringBuilder jpql = new StringBuilder("SELECT t.ticketType, ")
                .append(distinctAccounts ? "COUNT(DISTINCT t.subscriberAccount) " : "COUNT(t) ")
                .append("FROM CustomerTicketEntity t WHERE t.archivedAt IS NULL ")
                .append("AND (t.appStatus IS NULL OR t.appStatus NOT IN :appointed) AND t.ticketType IN :types ")
                .append(distinctAccounts
                        ? "AND t.subscriberAccount IS NOT NULL AND TRIM(t.subscriberAccount) <> '' "
                        : "AND (t.subscriberAccount IS NULL OR TRIM(t.subscriberAccount) = '') ");
        Map<String, Object> params = new HashMap<>();
        params.put("appointed", TicketStatus.appointedCodes());
        params.put("types", types);
        appendStaffFilter(jpql, params, assignedStaff);
        jpql.append("GROUP BY t.ticketType");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);
        for (Object[] row : query.getResultList()) {
            out.merge((TicketType) row[0], ((Number) row[1]).longValue(), Long::sum);
        }
    }

    @Override
    public Map<String, Integer> countDuplicateSubscribers(Collection<String> subscriberAccounts) {
        if (subscriberAccounts == null || subscriberAccounts.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> validAccounts = subscriberAccounts.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (validAccounts.isEmpty()) {
            return Collections.emptyMap();
        }

        // Nhóm không gộp (Phản ánh): đếm cả ca đã lưu trữ vì khách phản ánh nhiều lần vẫn cần được nhìn thấy.
        // Nhóm gộp (Hạ tầng): chỉ đếm ca đang mở, xử lý xong thì không còn tính vào "Trùng xN".
        List<TicketType> collapsing = DuplicateRule.collapsingTypes();
        StringBuilder jpql = new StringBuilder("""
                SELECT t.ticketType, t.subscriberAccount, COUNT(t.id)
                FROM CustomerTicketEntity t
                WHERE t.subscriberAccount IN :accounts
                """);
        if (!collapsing.isEmpty()) {
            jpql.append("AND (t.ticketType IS NULL OR t.ticketType NOT IN :collapseTypes OR t.archivedAt IS NULL) ");
        }
        jpql.append("GROUP BY t.ticketType, t.subscriberAccount");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("accounts", validAccounts);
        if (!collapsing.isEmpty()) {
            query.setParameter("collapseTypes", collapsing);
        }

        Map<String, Integer> countMap = new HashMap<>();
        for (Object[] row : query.getResultList()) {
            TicketType type = (TicketType) row[0];   // null = ca cũ chưa có loại -> PHAN_ANH (gộp chung khóa)
            String acc = (String) row[1];
            Long count = (Long) row[2];
            if (acc != null && count != null) {
                countMap.merge(DuplicateRule.key(type, acc), count.intValue(), Integer::sum);
            }
        }
        return countMap;
    }

    @Override
    public List<String> findAllDistinctAssignedStaff() {
        String jpql = """
                SELECT DISTINCT t.assignedStaff
                FROM CustomerTicketEntity t
                WHERE t.assignedStaff IS NOT NULL AND TRIM(t.assignedStaff) <> ''
                ORDER BY t.assignedStaff ASC
                """;
        return entityManager.createQuery(jpql, String.class).getResultList();
    }

    @Override
    public List<String> findAllDistinctAreas() {
        String jpql = "SELECT DISTINCT t.area FROM CustomerTicketEntity t WHERE t.archivedAt IS NULL AND t.area IS NOT NULL ORDER BY t.area ASC";
        return entityManager.createQuery(jpql, String.class).getResultList();
    }

    @Override
    public List<String> findAllAreasByStaff(String assignedStaff) {
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT t.area FROM CustomerTicketEntity t WHERE t.archivedAt IS NULL AND t.area IS NOT NULL AND TRIM(t.area) <> '' ");
        Map<String, Object> params = new HashMap<>();

        appendStaffFilter(jpql, params, assignedStaff);
        jpql.append("ORDER BY t.area ASC");

        TypedQuery<String> query = entityManager.createQuery(jpql.toString(), String.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    @Override
    public List<String> findAllCabinetPrefixes(String assignedStaff, String area) {
        StringBuilder jpql = new StringBuilder(
                "SELECT DISTINCT t.cabinetCode FROM CustomerTicketEntity t " +
                        "WHERE t.archivedAt IS NULL AND t.cabinetCode IS NOT NULL AND TRIM(t.cabinetCode) <> '' ");
        Map<String, Object> params = new HashMap<>();

        appendStaffFilter(jpql, params, assignedStaff);

        // Chỉ lấy các tủ thuộc khu vực đang chọn
        if (area != null && !area.trim().isEmpty()) {
            jpql.append("AND UPPER(TRIM(t.area)) = UPPER(TRIM(:area)) ");
            params.put("area", area.trim());
        }

        TypedQuery<String> query = entityManager.createQuery(jpql.toString(), String.class);
        params.forEach(query::setParameter);

        return query.getResultList().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(code -> code.length() > 7 ? code.substring(0, 7) : code)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // Lọc theo nhân viên được giao (không phân biệt hoa thường & khoảng trắng); "ALL" hoặc rỗng là không lọc
    private void appendStaffFilter(StringBuilder jpql, Map<String, Object> params, String assignedStaff) {
        if (assignedStaff != null && !assignedStaff.trim().isEmpty() && !"ALL".equalsIgnoreCase(assignedStaff.trim())) {
            jpql.append("AND UPPER(TRIM(t.assignedStaff)) = UPPER(TRIM(:assignedStaff)) ");
            params.put("assignedStaff", assignedStaff.trim());
        }
    }
}
