package com.huydung.custmapapplication.photo.repository;

import com.huydung.custmapapplication.photo.entity.TicketPhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketPhotoRepository extends JpaRepository<TicketPhotoEntity, Long> {

    long countByTicketId(Long ticketId);

    List<TicketPhotoEntity> findByTicketIdOrderByIdDesc(Long ticketId);

    Optional<TicketPhotoEntity> findByIdAndTicketId(Long id, Long ticketId);

    // Đếm số ảnh của nhiều ca trong một câu truy vấn: mỗi dòng là [ticketId, số ảnh]
    @Query("SELECT p.ticketId, COUNT(p) FROM TicketPhotoEntity p WHERE p.ticketId IN :ticketIds GROUP BY p.ticketId")
    List<Object[]> countGroupedByTicket(@Param("ticketIds") Collection<Long> ticketIds);
}
