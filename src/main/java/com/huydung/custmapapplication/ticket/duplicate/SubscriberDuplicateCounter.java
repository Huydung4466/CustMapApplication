package com.huydung.custmapapplication.ticket.duplicate;

import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.repository.TicketRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Đếm số ca trùng account cho các ca đang hiển thị (dùng chung cho Phản ánh và Hạ tầng).
 * Kết quả dùng để gắn nhãn "Trùng xN" trên thẻ ca.
 */
@Component
public class SubscriberDuplicateCounter {

    private final TicketRepository ticketRepository;

    public SubscriberDuplicateCounter(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /** Số ca trong từng nhóm trùng, khóa theo {@link DuplicateRule#key}. Chỉ truy vấn các account có trong danh sách. */
    public Map<String, Integer> countFor(Collection<CustomerTicketEntity> tickets) {
        Set<String> accounts = tickets.stream()
                .map(CustomerTicketEntity::getSubscriberAccount)
                .filter(DuplicateRule::hasAccount)
                .collect(Collectors.toSet());
        return accounts.isEmpty() ? Map.of() : ticketRepository.countDuplicateSubscribers(accounts);
    }

    /** Số ca trùng của một ca (tối thiểu 1). */
    public int of(Map<String, Integer> counts, CustomerTicketEntity ticket) {
        if (!DuplicateRule.hasAccount(ticket.getSubscriberAccount())) return 1;
        return counts.getOrDefault(DuplicateRule.key(ticket.getTicketType(), ticket.getSubscriberAccount()), 1);
    }
}
