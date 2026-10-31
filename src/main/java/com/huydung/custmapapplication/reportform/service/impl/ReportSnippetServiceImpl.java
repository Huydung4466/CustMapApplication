package com.huydung.custmapapplication.reportform.service.impl;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;
import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.common.exception.ResourceNotFoundException;
import com.huydung.custmapapplication.notification.enums.NotificationType;
import com.huydung.custmapapplication.notification.service.NotificationService;
import com.huydung.custmapapplication.reportform.ReportFormConfig;
import com.huydung.custmapapplication.reportform.dto.*;
import com.huydung.custmapapplication.reportform.entity.ReportSnippetEntity;
import com.huydung.custmapapplication.reportform.entity.ReportSnippetHistoryEntity;
import com.huydung.custmapapplication.reportform.enums.*;
import com.huydung.custmapapplication.reportform.repository.ReportSnippetHistoryRepository;
import com.huydung.custmapapplication.reportform.repository.ReportSnippetRepository;
import com.huydung.custmapapplication.reportform.service.ReportSnippetService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Quy tắc mẫu báo cáo dùng chung:
 * - Admin thêm / sửa / xóa: có hiệu lực ngay.
 * - Nhân viên thêm: mẫu ở trạng thái PENDING (chỉ người tạo và admin thấy) cho tới khi admin duyệt.
 * - Nhân viên sửa / xóa mẫu đã duyệt: ghi thành yêu cầu chờ duyệt (pendingChange); mẫu cũ vẫn dùng bình thường.
 *   Admin chưa xử lý thì yêu cầu cứ "đọng" ở trạng thái chờ.
 * - Mẫu mới chưa duyệt: người tạo tự sửa / xóa được (chưa ai khác dùng nên không cần duyệt).
 * - Admin từ chối phải ghi lý do: mẫu mới chuyển sang REJECTED (người tạo xem lý do, sửa và gửi lại hoặc xóa);
 *   yêu cầu sửa / xóa bị hủy, mẫu cũ giữ nguyên.
 * - Admin có thể chỉnh nội dung trước khi duyệt.
 * - Mọi thao tác ghi vào lịch sử và gửi thông báo cho đúng người, cùng một giao dịch với việc đổi trạng thái.
 *   Hai admin duyệt cùng lúc: khóa lạc quan (version) khiến người đến sau nhận báo "đã được xử lý".
 */
@Service
public class ReportSnippetServiceImpl implements ReportSnippetService {

    private static final int MAX_REASON_LENGTH = 300;

    private final ReportSnippetRepository repository;
    private final ReportSnippetHistoryRepository historyRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final ReportFormConfig formConfig;

    public ReportSnippetServiceImpl(ReportSnippetRepository repository,
                                    ReportSnippetHistoryRepository historyRepository,
                                    AuthService authService,
                                    NotificationService notificationService,
                                    ReportFormConfig formConfig) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.authService = authService;
        this.notificationService = notificationService;
        this.formConfig = formConfig;
    }

    // ------------------------------------------------------------------ đọc

    @Override
    public ReportFormMetaDTO getMeta() {
        return new ReportFormMetaDTO(
                formConfig.formUrl(),
                formConfig.taskGroupEntry(),
                ReportFormConfig.MAX_CONTENT_LENGTH,
                Arrays.stream(ReportTaskGroup.values())
                        .map(g -> new ReportFormMetaDTO.TaskGroupInfo(g.name(), g.getLabel())).toList(),
                Arrays.stream(ReportField.values())
                        .map(f -> new ReportFormMetaDTO.FieldInfo(f.name(), f.getLabel(), formConfig.fieldEntry(f), f.getHint())).toList(),
                formConfig.infoEntries(),
                ReportTaskGroup.suggestionByTicketType(),
                Arrays.stream(SnippetState.values())
                        .map(s -> new ReportFormMetaDTO.StateInfo(s.name(), s.getLabel(), s.getIcon(), s.getBadgeClass())).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportSnippetDTO> listUsable(String field) {
        CurrentUserResponse me = authService.getCurrentLoggedUser();
        ReportField f = ReportField.fromCode(field);
        return repository.findVisible(f.name(), me.getId()).stream()
                // Mẫu gắn nhóm xếp trước mẫu dùng chung; trong mỗi nhóm xếp theo chữ cái
                .sorted(Comparator.comparing((ReportSnippetEntity s) -> s.getTaskGroup() == null ? 1 : 0)
                        .thenComparing(ReportSnippetEntity::getContent, String.CASE_INSENSITIVE_ORDER))
                .map(s -> toDto(s, me))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportSnippetDTO> listForManage() {
        CurrentUserResponse me = authService.getCurrentLoggedUser();
        List<ReportSnippetEntity> list = me.isAdmin() ? repository.findAllForAdmin() : repository.findForManage(me.getId());
        return list.stream().map(s -> toDto(s, me)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportSnippetDTO> listAllForAdmin() {
        CurrentUserResponse me = requireAdmin();
        return repository.findAllForAdmin().stream().map(s -> toDto(s, me)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAwaitingApproval() {
        requireAdmin();
        return repository.countAwaitingApproval();
    }

    // ------------------------------------------------------------------ thêm / sửa / xóa

    @Override
    @Transactional
    public SnippetActionResult create(ReportSnippetRequest request) {
        CurrentUserResponse me = authService.getCurrentLoggedUser();
        ReportField field = ReportField.fromCode(request.getField());
        ReportTaskGroup group = ReportTaskGroup.fromCodeOrNull(request.getTaskGroup());
        String content = cleanContent(request.getContent());
        assertNotDuplicate(field, group, content, null);

        LocalDateTime now = LocalDateTime.now();
        ReportSnippetEntity e = new ReportSnippetEntity();
        e.setFieldCode(field.name());
        e.setTaskGroup(group == null ? null : group.name());
        e.setContent(content);
        e.setCreatedById(me.getId());
        e.setCreatedByName(me.getFullName());
        e.setCreatedAt(now);
        if (me.isAdmin()) {
            e.setStatus(SnippetStatus.APPROVED.name());
            e.setApprovedByName(me.getFullName());
            e.setApprovedAt(now);
        } else {
            e.setStatus(SnippetStatus.PENDING.name());
        }
        e = repository.save(e);

        if (me.isAdmin()) {
            logHistory(SnippetAction.CREATE_APPROVED, e, me, null, content, null);
        } else {
            logHistory(SnippetAction.CREATE, e, me, null, content, null);
            notificationService.notifyAdmins(NotificationType.REQUEST_PENDING,
                    nameOf(me) + " đã thêm mẫu " + quote(content) + ", đang chờ admin duyệt.", e.getId(), me.getId());
        }
        return new SnippetActionResult(me.isAdmin(),
                me.isAdmin() ? "Đã lưu mẫu" : "Đã gửi mẫu, đang chờ admin duyệt (còn đọng)",
                toDto(e, me));
    }

    @Override
    @Transactional
    public SnippetActionResult requestEdit(Long id, String rawContent) {
        CurrentUserResponse me = authService.getCurrentLoggedUser();
        ReportSnippetEntity e = find(id);
        String content = cleanContent(rawContent);
        ReportField field = ReportField.fromCode(e.getFieldCode());
        ReportTaskGroup group = ReportTaskGroup.fromCodeOrNull(e.getTaskGroup());

        if (SnippetStatus.PENDING.is(e.getStatus())) {
            // Mẫu mới chưa duyệt: chỉ người tạo hoặc admin sửa, sửa thẳng (vẫn chờ duyệt; admin xem nội dung mới ở danh sách chờ)
            assertOwnerOrAdmin(e, me);
            assertNotDuplicate(field, group, content, e.getId());
            String before = e.getContent();
            e.setContent(content);
            e = repository.save(e);
            logHistory(SnippetAction.EDIT_NEW, e, me, before, content, null);
            return new SnippetActionResult(true, "Đã cập nhật mẫu (vẫn chờ admin duyệt)", toDto(e, me));
        }

        if (SnippetStatus.REJECTED.is(e.getStatus())) {
            // Mẫu bị từ chối: chỉ người tạo sửa lại và gửi lại cho admin duyệt
            if (!Objects.equals(e.getCreatedById(), me.getId())) {
                throw new IllegalArgumentException("Chỉ người tạo mới sửa và gửi lại được mẫu bị từ chối");
            }
            assertNotDuplicate(field, group, content, e.getId());
            String before = e.getContent();
            e.setContent(content);
            e.setStatus(SnippetStatus.PENDING.name());
            e.setRejectReason(null);
            e.setRejectedByName(null);
            e.setRejectedAt(null);
            e = repository.save(e);
            logHistory(SnippetAction.RESUBMIT, e, me, before, content, null);
            notificationService.notifyAdmins(NotificationType.REQUEST_PENDING,
                    nameOf(me) + " đã sửa lại và gửi lại mẫu " + quote(content) + ", đang chờ admin duyệt.", e.getId(), me.getId());
            return new SnippetActionResult(false, "Đã gửi lại mẫu, đang chờ admin duyệt (còn đọng)", toDto(e, me));
        }

        if (me.isAdmin()) {
            assertNotDuplicate(field, group, content, e.getId());
            String before = e.getContent();
            Long requesterId = e.getChangeById();
            boolean hadRequest = e.getPendingChange() != null;
            e.setContent(content);
            clearPending(e);   // sửa trực tiếp thì mọi yêu cầu đang chờ trên mẫu này không còn nghĩa
            e = repository.save(e);
            logHistory(SnippetAction.EDIT_DIRECT, e, me, before, content, null);
            if (hadRequest) {
                notificationService.resolveAdminRequests(e.getId());
                if (!Objects.equals(requesterId, me.getId())) {
                    notificationService.notifyUser(requesterId, NotificationType.REQUEST_CANCELLED,
                            "Yêu cầu của bạn trên mẫu " + quote(before) + " đã bị thay thế vì admin "
                                    + nameOf(me) + " đã sửa trực tiếp mẫu này.", e.getId());
                }
            }
            return new SnippetActionResult(true, "Đã sửa mẫu", toDto(e, me));
        }

        assertNoPendingChange(e);
        if (normalizeKey(content).equals(normalizeKey(e.getContent()))) {
            throw new IllegalArgumentException("Nội dung chưa thay đổi so với mẫu hiện tại");
        }
        assertNotDuplicate(field, group, content, e.getId());
        e.setPendingChange(SnippetChange.EDIT.name());
        e.setPendingContent(content);
        markChangeBy(e, me);
        e = repository.save(e);
        logHistory(SnippetAction.REQUEST_EDIT, e, me, e.getContent(), content, null);
        notificationService.notifyAdmins(NotificationType.REQUEST_PENDING,
                nameOf(me) + " đề nghị sửa mẫu " + quote(e.getContent()) + " thành " + quote(content) + ", đang chờ admin duyệt.",
                e.getId(), me.getId());
        return new SnippetActionResult(false, "Đã gửi yêu cầu sửa, đang chờ admin duyệt (còn đọng)", toDto(e, me));
    }

    @Override
    @Transactional
    public SnippetActionResult requestDelete(Long id) {
        CurrentUserResponse me = authService.getCurrentLoggedUser();
        ReportSnippetEntity e = find(id);

        // Mẫu mới chưa duyệt / bị từ chối: người tạo (hoặc admin) xóa thẳng
        if (!SnippetStatus.APPROVED.is(e.getStatus())) {
            assertOwnerOrAdmin(e, me);
            deleteNow(e, me);
            return new SnippetActionResult(true, "Đã xóa mẫu", null);
        }

        if (me.isAdmin()) {
            deleteNow(e, me);
            return new SnippetActionResult(true, "Đã xóa mẫu", null);
        }

        assertNoPendingChange(e);
        e.setPendingChange(SnippetChange.DELETE.name());
        e.setPendingContent(null);
        markChangeBy(e, me);
        e = repository.save(e);
        logHistory(SnippetAction.REQUEST_DELETE, e, me, e.getContent(), null, null);
        notificationService.notifyAdmins(NotificationType.REQUEST_PENDING,
                nameOf(me) + " đề nghị xóa mẫu " + quote(e.getContent()) + ", đang chờ admin duyệt.", e.getId(), me.getId());
        return new SnippetActionResult(false, "Đã gửi yêu cầu xóa, đang chờ admin duyệt (còn đọng)", toDto(e, me));
    }

    // Xóa thật một mẫu (admin xóa, hoặc người tạo xóa mẫu chưa duyệt / bị từ chối); báo cho những người liên quan
    private void deleteNow(ReportSnippetEntity e, CurrentUserResponse me) {
        Long snippetId = e.getId();
        Long creatorId = e.getCreatedById();
        Long requesterId = e.getChangeById();
        boolean hadRequest = e.getPendingChange() != null;
        String content = e.getContent();
        logHistory(SnippetAction.DELETE_DIRECT, e, me, content, null, null);
        repository.delete(e);
        notificationService.resolveAdminRequests(snippetId);

        if (!me.isAdmin()) return;   // người tạo tự xóa mẫu của mình thì không cần báo ai
        if (hadRequest && requesterId != null && !Objects.equals(requesterId, me.getId())) {
            notificationService.notifyUser(requesterId, NotificationType.REQUEST_CANCELLED,
                    "Yêu cầu của bạn trên mẫu " + quote(content) + " đã bị hủy vì admin " + nameOf(me) + " đã xóa mẫu này.", snippetId);
        }
        if (creatorId != null && !Objects.equals(creatorId, me.getId()) && !Objects.equals(creatorId, requesterId)) {
            notificationService.notifyUser(creatorId, NotificationType.SNIPPET_REMOVED,
                    "Mẫu " + quote(content) + " đã bị admin " + nameOf(me) + " xóa.", snippetId);
        }
    }

    // ------------------------------------------------------------------ duyệt / từ chối

    @Override
    @Transactional
    public SnippetActionResult approve(Long id, String editedContent) {
        CurrentUserResponse me = requireAdmin();
        ReportSnippetEntity e = find(id);
        ReportField field = ReportField.fromCode(e.getFieldCode());
        ReportTaskGroup group = ReportTaskGroup.fromCodeOrNull(e.getTaskGroup());
        LocalDateTime now = LocalDateTime.now();
        boolean wantsEdit = editedContent != null && !editedContent.isBlank();
        String adminName = nameOf(me);

        if (SnippetStatus.PENDING.is(e.getStatus())) {
            String proposed = e.getContent();
            String finalContent = wantsEdit ? cleanContent(editedContent) : proposed;
            boolean edited = !normalizeKey(finalContent).equals(normalizeKey(proposed));
            if (edited) assertNotDuplicate(field, group, finalContent, e.getId());
            Long creatorId = e.getCreatedById();
            e.setContent(finalContent);
            e.setStatus(SnippetStatus.APPROVED.name());
            e.setApprovedByName(me.getFullName());
            e.setApprovedAt(now);
            e = repository.save(e);
            logHistory(SnippetAction.APPROVE_ADD, e, me, proposed, finalContent, edited ? "Admin chỉnh sửa nội dung trước khi duyệt" : null);
            finishApproval(e.getId(), creatorId, me, finalContent, edited, adminName);
            return new SnippetActionResult(true, edited ? "Đã chỉnh sửa và duyệt mẫu" : "Đã duyệt mẫu", toDto(e, me));
        }

        if (SnippetChange.EDIT.is(e.getPendingChange())) {
            String before = e.getContent();
            String proposed = e.getPendingContent();
            String finalContent = wantsEdit ? cleanContent(editedContent) : proposed;
            boolean edited = !normalizeKey(finalContent).equals(normalizeKey(proposed));
            if (edited) assertNotDuplicate(field, group, finalContent, e.getId());
            Long requesterId = e.getChangeById();
            e.setContent(finalContent);
            clearPending(e);
            e = repository.save(e);
            logHistory(SnippetAction.APPROVE_EDIT, e, me, before, finalContent, edited ? "Admin chỉnh sửa nội dung trước khi duyệt" : null);
            finishApproval(e.getId(), requesterId, me, finalContent, edited, adminName);
            return new SnippetActionResult(true, edited ? "Đã chỉnh sửa và duyệt yêu cầu sửa" : "Đã duyệt yêu cầu sửa", toDto(e, me));
        }

        if (SnippetChange.DELETE.is(e.getPendingChange())) {
            Long snippetId = e.getId();
            Long requesterId = e.getChangeById();
            String content = e.getContent();
            logHistory(SnippetAction.APPROVE_DELETE, e, me, content, null, null);
            repository.delete(e);
            notificationService.resolveAdminRequests(snippetId);
            if (requesterId != null && !Objects.equals(requesterId, me.getId())) {
                notificationService.notifyUser(requesterId, NotificationType.DELETE_APPROVED,
                        "Yêu cầu xóa mẫu " + quote(content) + " đã được admin " + adminName + " chấp thuận.", snippetId);
            }
            return new SnippetActionResult(true, "Đã duyệt yêu cầu xóa", null);
        }

        if (SnippetStatus.REJECTED.is(e.getStatus())) {
            throw new IllegalArgumentException("Mẫu này đã bị từ chối, đang chờ người tạo sửa lại");
        }
        throw new IllegalArgumentException("Mẫu này không có yêu cầu nào đang chờ duyệt (có thể đã được admin khác xử lý)");
    }

    // Sau khi duyệt mẫu mới / yêu cầu sửa: đóng các thông báo "chờ duyệt" và báo kết quả cho người gửi
    private void finishApproval(Long snippetId, Long requesterId, CurrentUserResponse me, String content, boolean edited, String adminName) {
        notificationService.resolveAdminRequests(snippetId);
        if (requesterId == null || Objects.equals(requesterId, me.getId())) return;
        if (edited) {
            notificationService.notifyUser(requesterId, NotificationType.APPROVED_EDITED,
                    "Mẫu " + quote(content) + " đã được admin " + adminName + " chấp thuận và chỉnh sửa lại cho phù hợp.", snippetId);
        } else {
            notificationService.notifyUser(requesterId, NotificationType.APPROVED,
                    "Mẫu " + quote(content) + " đã được admin " + adminName + " chấp thuận.", snippetId);
        }
    }

    @Override
    @Transactional
    public SnippetActionResult reject(Long id, String rawReason) {
        CurrentUserResponse me = requireAdmin();
        ReportSnippetEntity e = find(id);
        String reason = cleanReason(rawReason);
        String adminName = nameOf(me);

        if (SnippetStatus.PENDING.is(e.getStatus())) {
            Long creatorId = e.getCreatedById();
            e.setStatus(SnippetStatus.REJECTED.name());
            e.setRejectReason(reason);
            e.setRejectedByName(me.getFullName());
            e.setRejectedAt(LocalDateTime.now());
            e = repository.save(e);
            logHistory(SnippetAction.REJECT_ADD, e, me, e.getContent(), null, reason);
            notificationService.resolveAdminRequests(e.getId());
            if (creatorId != null && !Objects.equals(creatorId, me.getId())) {
                notificationService.notifyUser(creatorId, NotificationType.REJECTED,
                        "Mẫu " + quote(e.getContent()) + " bị admin " + adminName + " từ chối. Lý do: " + reason, e.getId());
            }
            return new SnippetActionResult(true, "Đã từ chối mẫu", toDto(e, me));
        }

        if (e.getPendingChange() != null) {
            boolean isEdit = SnippetChange.EDIT.is(e.getPendingChange());
            Long requesterId = e.getChangeById();
            String proposed = e.getPendingContent();
            clearPending(e);
            e = repository.save(e);
            logHistory(isEdit ? SnippetAction.REJECT_EDIT : SnippetAction.REJECT_DELETE, e, me, e.getContent(), proposed, reason);
            notificationService.resolveAdminRequests(e.getId());
            if (requesterId != null && !Objects.equals(requesterId, me.getId())) {
                notificationService.notifyUser(requesterId, NotificationType.REJECTED,
                        "Yêu cầu " + (isEdit ? "sửa" : "xóa") + " mẫu " + quote(e.getContent()) + " bị admin "
                                + adminName + " từ chối. Lý do: " + reason, e.getId());
            }
            return new SnippetActionResult(true, "Đã từ chối yêu cầu " + (isEdit ? "sửa" : "xóa"), toDto(e, me));
        }

        if (SnippetStatus.REJECTED.is(e.getStatus())) {
            throw new IllegalArgumentException("Mẫu này đã bị từ chối trước đó");
        }
        throw new IllegalArgumentException("Mẫu này không có yêu cầu nào đang chờ duyệt (có thể đã được admin khác xử lý)");
    }

    // ------------------------------------------------------------------ lịch sử

    @Override
    @Transactional(readOnly = true)
    public List<SnippetHistoryDTO> getHistory(Long snippetId) {
        CurrentUserResponse me = authService.getCurrentLoggedUser();
        if (!me.isAdmin()) {
            // Nhân viên chỉ xem lịch sử mẫu do mình tạo; mẫu của người khác coi như không tồn tại
            ReportSnippetEntity e = find(snippetId);
            if (!Objects.equals(e.getCreatedById(), me.getId())) {
                throw new ResourceNotFoundException("Không tìm thấy mẫu ID: " + snippetId);
            }
        }
        return historyRepository.findBySnippetIdOrderByCreatedAtDescIdDesc(snippetId).stream().map(this::toHistoryDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SnippetHistoryDTO> listRecentHistory(int limit) {
        requireAdmin();
        int safe = Math.min(Math.max(limit, 1), 200);
        return historyRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, safe)).stream().map(this::toHistoryDto).toList();
    }

    private void logHistory(SnippetAction action, ReportSnippetEntity e, CurrentUserResponse me, String before, String after, String note) {
        ReportSnippetHistoryEntity h = new ReportSnippetHistoryEntity();
        h.setSnippetId(e.getId());
        h.setFieldCode(e.getFieldCode());
        h.setTaskGroup(e.getTaskGroup());
        h.setAction(action.name());
        h.setActorId(me.getId());
        h.setActorName(me.getFullName());
        h.setContentBefore(before);
        h.setContentAfter(after);
        h.setNote(note);
        h.setCreatedAt(LocalDateTime.now());
        historyRepository.save(h);
    }

    private SnippetHistoryDTO toHistoryDto(ReportSnippetHistoryEntity h) {
        SnippetAction action = Arrays.stream(SnippetAction.values())
                .filter(a -> a.name().equals(h.getAction())).findFirst().orElse(SnippetAction.CREATE);
        SnippetState state = action.getResultState();
        ReportTaskGroup group = ReportTaskGroup.fromCodeOrNull(h.getTaskGroup());
        String fieldLabel;
        try {
            fieldLabel = ReportField.fromCode(h.getFieldCode()).getLabel();
        } catch (IllegalArgumentException ex) {
            fieldLabel = "";
        }
        return new SnippetHistoryDTO(h.getId(), h.getSnippetId(), fieldLabel,
                group == null ? null : group.getLabel(),
                action.name(), action.getLabel(), action.getIcon(),
                state.name(), state.getLabel(), state.getBadgeClass(),
                h.getActorName(), h.getContentBefore(), h.getContentAfter(), h.getNote(), h.getCreatedAt());
    }

    // ------------------------------------------------------------------ tiện ích

    private CurrentUserResponse requireAdmin() {
        CurrentUserResponse me = authService.getCurrentLoggedUser();
        if (!me.isAdmin()) {
            throw new AccessDeniedException("Chỉ admin mới có quyền thực hiện thao tác này");
        }
        return me;
    }

    private ReportSnippetEntity find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mẫu ID: " + id));
    }

    private void assertOwnerOrAdmin(ReportSnippetEntity e, CurrentUserResponse me) {
        if (me.isAdmin()) return;
        if (e.getCreatedById() == null || !e.getCreatedById().equals(me.getId())) {
            // Mẫu chờ duyệt / bị từ chối của người khác: coi như không tồn tại với người này
            throw new ResourceNotFoundException("Không tìm thấy mẫu ID: " + e.getId());
        }
    }

    private void assertNoPendingChange(ReportSnippetEntity e) {
        if (e.getPendingChange() != null) {
            throw new IllegalArgumentException(
                    "Mẫu này đang có yêu cầu chờ admin duyệt, vui lòng đợi xử lý xong rồi gửi tiếp");
        }
    }

    private void clearPending(ReportSnippetEntity e) {
        e.setPendingChange(null);
        e.setPendingContent(null);
        e.setChangeById(null);
        e.setChangeByName(null);
        e.setChangeAt(null);
    }

    private void markChangeBy(ReportSnippetEntity e, CurrentUserResponse me) {
        e.setChangeById(me.getId());
        e.setChangeByName(me.getFullName());
        e.setChangeAt(LocalDateTime.now());
    }

    // Gộp khoảng trắng / xuống dòng (ô trong form là một dòng), kiểm tra rỗng và độ dài
    private String cleanContent(String raw) {
        String c = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
        if (c.isEmpty()) {
            throw new IllegalArgumentException("Nội dung mẫu không được để trống");
        }
        if (c.length() > ReportFormConfig.MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung mẫu tối đa " + ReportFormConfig.MAX_CONTENT_LENGTH + " ký tự");
        }
        return c;
    }

    // Lý do từ chối: bắt buộc, để người gửi biết cần sửa gì
    private String cleanReason(String raw) {
        String c = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
        if (c.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối");
        }
        if (c.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Lý do từ chối tối đa " + MAX_REASON_LENGTH + " ký tự");
        }
        return c;
    }

    private static String normalizeKey(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String nameOf(CurrentUserResponse me) {
        return me.getFullName() != null && !me.getFullName().isBlank() ? me.getFullName() : me.getUsername();
    }

    // Trích nội dung mẫu đưa vào câu thông báo: cắt ngắn, đặt trong dấu “ ”
    private static String quote(String content) {
        String t = content == null ? "" : content;
        if (t.length() > 60) t = t.substring(0, 60) + "…";
        return "“" + t + "”";
    }

    // Không cho trùng nội dung trong cùng mục + cùng nhóm (kể cả nội dung đang đề xuất sửa); mẫu bị từ chối không tính
    private void assertNotDuplicate(ReportField field, ReportTaskGroup group, String content, Long excludeId) {
        List<ReportSnippetEntity> same = group == null
                ? repository.findByFieldCodeAndTaskGroupIsNull(field.name())
                : repository.findByFieldCodeAndTaskGroup(field.name(), group.name());
        String key = normalizeKey(content);
        for (ReportSnippetEntity s : same) {
            if (excludeId != null && excludeId.equals(s.getId())) continue;
            if (SnippetStatus.REJECTED.is(s.getStatus())) continue;
            if (key.equals(normalizeKey(s.getContent())) || key.equals(normalizeKey(s.getPendingContent()))) {
                throw new IllegalArgumentException("Mẫu này đã có trong danh sách");
            }
        }
    }

    private ReportSnippetDTO toDto(ReportSnippetEntity e, CurrentUserResponse me) {
        ReportField field = ReportField.fromCode(e.getFieldCode());
        ReportTaskGroup group = ReportTaskGroup.fromCodeOrNull(e.getTaskGroup());
        SnippetState state = SnippetState.resolve(e.getStatus(), e.getPendingChange());

        // Loại yêu cầu đang chờ: mẫu mới = ADD; mẫu đã duyệt có yêu cầu = EDIT / DELETE; còn lại không có
        SnippetChange request = null;
        if (SnippetStatus.PENDING.is(e.getStatus())) {
            request = SnippetChange.ADD;
        } else if (SnippetChange.EDIT.is(e.getPendingChange())) {
            request = SnippetChange.EDIT;
        } else if (SnippetChange.DELETE.is(e.getPendingChange())) {
            request = SnippetChange.DELETE;
        }

        boolean approved = SnippetStatus.APPROVED.is(e.getStatus());
        boolean rejected = SnippetStatus.REJECTED.is(e.getStatus());
        boolean owner = me.getId() != null && me.getId().equals(e.getCreatedById());
        boolean mine = owner || (me.getId() != null && me.getId().equals(e.getChangeById()));
        // Có hiệu lực ngay (không qua duyệt): admin (trừ mẫu bị từ chối: chờ người tạo sửa), hoặc người tạo với mẫu chưa duyệt
        boolean directEdit = (me.isAdmin() && !rejected) || (owner && !approved);
        boolean directDelete = me.isAdmin() || (owner && !approved);

        return new ReportSnippetDTO(e.getId(), field.name(), field.getLabel(),
                group == null ? null : group.name(), group == null ? null : group.getLabel(),
                e.getContent(), e.getStatus(), e.getPendingChange(), e.getPendingContent(),
                state.name(), state.getLabel(), state.getIcon(), state.getBadgeClass(),
                request == null ? null : request.getWaitingNote(),
                request == null ? null : request.name(),
                request == null ? null : request.getLabel(),
                request == null ? null : request.getPillClass(),
                e.getRejectReason(), e.getRejectedByName(), e.getRejectedAt(),
                e.getApprovedByName(), e.getApprovedAt(),
                e.getCreatedByName(), e.getCreatedAt(), e.getChangeByName(), e.getChangeAt(),
                mine, owner, directEdit, directDelete);
    }
}
