package com.huydung.custmapapplication.notification.repository;

import com.huydung.custmapapplication.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByRecipientIdOrderByCreatedAtDescIdDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    Optional<NotificationEntity> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.readAt = :now WHERE n.recipientId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // Yêu cầu đã được xử lý thì các thông báo "chờ duyệt" của mẫu đó tự chuyển thành đã đọc (với mọi admin)
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.readAt = :now WHERE n.snippetId = :snippetId AND n.type = :type AND n.readAt IS NULL")
    int markReadBySnippetAndType(@Param("snippetId") Long snippetId, @Param("type") String type, @Param("now") LocalDateTime now);
}
