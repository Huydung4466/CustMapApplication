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

import com.huydung.custmapapplication.ticket.type.TicketType;

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
        // Chỉ lấy ca đang mở (chưa nằm trong Lịch sử)
        QueryParts q = new QueryParts("WHERE t.archivedAt IS NULL ");

        appendStaffFilter(q.where, q.params, f.assignedStaff());

        // Ca đã hẹn (Hẹn giờ / Hẹn khác) chuyển sang trang "Đã hẹn", không hiện ở danh sách chính
        if (f.appointed()) {
            q.where.append("AND t.appStatus IN ('HEN_GIO', 'HEN_KHAC') ");
        } else {
            q.where.append("AND (t.appStatus IS NULL OR t.appStatus NOT IN ('HEN_GIO', 'HEN_KHAC')) ");
        }

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
        return q;
    }

    private QueryParts buildHistoryWhere(String assignedStaff, String keyword, LocalDateTime from, LocalDateTime toExclusive) {
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
                                                         int offset, int limit) {
        QueryParts q = buildHistoryWhere(assignedStaff, keyword, from, toExclusive);
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
    public long countHistoryTickets(String assignedStaff, String keyword, LocalDateTime from, LocalDateTime toExclusive) {
        QueryParts q = buildHistoryWhere(assignedStaff, keyword, from, toExclusive);
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(t) FROM CustomerTicketEntity t " + q.where, Long.class);
        q.params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    @Override
    public Map<TicketType, Long> countOpenByType(String assignedStaff) {
        StringBuilder jpql = new StringBuilder(
                "SELECT t.ticketType, COUNT(t) FROM CustomerTicketEntity t WHERE t.archivedAt IS NULL "
                        + "AND (t.appStatus IS NULL OR t.appStatus NOT IN ('HEN_GIO', 'HEN_KHAC')) ");
        Map<String, Object> params = new HashMap<>();
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
        return result;
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

        // Đếm cả ca đã lưu trữ: khách phản ánh nhiều lần vẫn cần được nhìn thấy
        String jpql = """
                SELECT t.subscriberAccount, COUNT(t.id)
                FROM CustomerTicketEntity t
                WHERE t.subscriberAccount IN :accounts
                  AND (t.ticketType = :phanAnh OR t.ticketType IS NULL)
                GROUP BY t.subscriberAccount
                """;

        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class)
                .setParameter("accounts", validAccounts)
                .setParameter("phanAnh", TicketType.PHAN_ANH)
                .getResultList();

        Map<String, Integer> countMap = new HashMap<>();
        for (Object[] row : rows) {
            String acc = (String) row[0];
            Long count = (Long) row[1];
            if (acc != null && count != null) {
                countMap.put(acc.trim().toLowerCase(), count.intValue());
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
