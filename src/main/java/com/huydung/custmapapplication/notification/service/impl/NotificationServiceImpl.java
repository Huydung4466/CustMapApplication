package com.huydung.custmapapplication.notification.service.impl;

import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.common.exception.ResourceNotFoundException;
import com.huydung.custmapapplication.notification.dto.NotificationDTO;
import com.huydung.custmapapplication.notification.dto.NotificationPageDTO;
import com.huydung.custmapapplication.notification.entity.NotificationEntity;
import com.huydung.custmapapplication.notification.enums.NotificationType;
import com.huydung.custmapapplication.notification.repository.NotificationRepository;
import com.huydung.custmapapplication.notification.service.NotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @PersistenceContext
    private EntityManager em;

    private final NotificationRepository repository;
    private final AuthService authService;

    public NotificationServiceImpl(NotificationRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public void notifyAdmins(NotificationType type, String message, Long snippetId, Long excludeUserId) {
        List<Long> adminIds = em.createQuery(
                "SELECT DISTINCT u.id FROM UserEntity u JOIN u.roles r WHERE r.code = 'ROLE_ADMIN' AND u.status = 1",
                Long.class).getResultList();
        LocalDateTime now = LocalDateTime.now();
        List<NotificationEntity> rows = new ArrayList<>();
        for (Long adminId : adminIds) {
            if (adminId.equals(excludeUserId)) continue;
            rows.add(build(adminId, type, message, snippetId, now));
        }
        repository.saveAll(rows);
    }

    @Override
    @Transactional
    public void notifyUser(Long userId, NotificationType type, String message, Long snippetId) {
        if (userId == null) return;
        repository.save(build(userId, type, message, snippetId, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void resolveAdminRequests(Long snippetId) {
        if (snippetId == null) return;
        repository.markReadBySnippetAndType(snippetId, NotificationType.REQUEST_PENDING.name(), LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPageDTO list(int page) {
        Long me = currentUserId();
        Page<NotificationEntity> result = repository.findByRecipientIdOrderByCreatedAtDescIdDesc(
                me, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        List<NotificationDTO> items = result.getContent().stream().map(this::toDto).toList();
        return new NotificationPageDTO(items, repository.countByRecipientIdAndReadAtIsNull(me), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByRecipientIdAndReadAtIsNull(currentUserId());
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        // Tìm theo cả người nhận: không ai đánh dấu được thông báo của người khác
        NotificationEntity n = repository.findByIdAndRecipientId(id, currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo ID: " + id));
        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now());
            repository.save(n);
        }
    }

    @Override
    @Transactional
    public void markAllRead() {
        repository.markAllRead(currentUserId(), LocalDateTime.now());
    }

    private Long currentUserId() {
        return authService.getCurrentLoggedUser().getId();
    }

    private NotificationEntity build(Long recipientId, NotificationType type, String message, Long snippetId, LocalDateTime now) {
        NotificationEntity n = new NotificationEntity();
        n.setRecipientId(recipientId);
        n.setType(type.name());
        n.setMessage(message.length() > 600 ? message.substring(0, 597) + "..." : message);
        n.setSnippetId(snippetId);
        n.setCreatedAt(now);
        return n;
    }

    private NotificationDTO toDto(NotificationEntity e) {
        NotificationType t = NotificationType.fromCode(e.getType());
        String link = e.getSnippetId() == null ? null : "/report-snippets?focus=" + e.getSnippetId();
        return new NotificationDTO(e.getId(), t.name(), t.getTitle(), t.getIcon(), t.getColorClass(),
                e.getMessage(), e.getSnippetId(), link, e.getReadAt() != null, e.getCreatedAt());
    }
}
