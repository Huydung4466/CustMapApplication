package com.huydung.custmapapplication.ticket.repository;

import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<CustomerTicketEntity, Long>, CustomTicketRepository {
    Optional<CustomerTicketEntity> findByTicketCode(String ticketCode);
}
