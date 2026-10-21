package com.huydung.custmapapplication.export.service.impl;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;
import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.common.exception.ResourceNotFoundException;
import com.huydung.custmapapplication.export.enums.ExportStatus;
import com.huydung.custmapapplication.export.builder.TicketWorkbookBuilder;
import com.huydung.custmapapplication.export.dto.ExportDownload;
import com.huydung.custmapapplication.export.dto.ExportFileDTO;
import com.huydung.custmapapplication.export.dto.ExportRequest;
import com.huydung.custmapapplication.export.service.ExportService;
import com.huydung.custmapapplication.export.storage.ExportFileStorage;
import com.huydung.custmapapplication.photo.entity.TicketPhotoEntity;
import com.huydung.custmapapplication.photo.repository.TicketPhotoRepository;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.repository.TicketRepository;
import com.huydung.custmapapplication.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);
    private static final int CHUNK = 1000;   // chia nhỏ danh sách ID khi truy vấn IN (...) để không vượt giới hạn tham số

    private final TicketRepository ticketRepository;
    private final TicketPhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final TicketWorkbookBuilder workbookBuilder;
    private final ExportFileStorage storage;

    public ExportServiceImpl(TicketRepository ticketRepository,
                             TicketPhotoRepository photoRepository,
                             UserRepository userRepository,
                             AuthService authService,
                             TicketWorkbookBuilder workbookBuilder,
                             ExportFileStorage storage) {
        this.ticketRepository = ticketRepository;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.workbookBuilder = workbookBuilder;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public ExportFileDTO exportTickets(ExportRequest request) {
        CurrentUserResponse me = requireAdmin();

        String staff = request == null ? null : request.getStaff();
        staff = (staff == null || staff.isBlank() || "ALL".equalsIgnoreCase(staff.trim())) ? null : staff.trim();
        boolean onlyChanged = request != null && "CHANGED".equalsIgnoreCase(request.getScope());

        if (storage.list(me.getId()).size() >= storage.getMaxPerUser()) {
            throw new IllegalArgumentException("Đã có " + storage.getMaxPerUser()
                    + " file đang được lưu. Hãy xóa bớt hoặc đợi file cũ tự xóa rồi xuất tiếp.");
        }

        LocalDateTime now = LocalDateTime.now();
        final String staffFilter = staff;

        // Lọc theo nhân viên và phạm vi ca; giữ thứ tự nhập (theo ID) để dòng trong file giống thứ tự file import
        List<CustomerTicketEntity> tickets = new ArrayList<>();
        for (CustomerTicketEntity t : ticketRepository.findAll()) {
            if (staffFilter != null && (t.getAssignedStaff() == null
                    || !t.getAssignedStaff().trim().equalsIgnoreCase(staffFilter))) {
                continue;
            }
            if (onlyChanged && !ExportStatus.of(t, now).isChanged()) {
                continue;
            }
            tickets.add(t);
        }
        tickets.sort(Comparator.comparing(CustomerTicketEntity::getId));

        // Số ảnh của mọi ca; ảnh chụp gần nhất chỉ cần cho ca đã xử lý / KLL
        List<Long> allIds = new ArrayList<>();
        List<Long> processedIds = new ArrayList<>();
        for (CustomerTicketEntity t : tickets) {
            allIds.add(t.getId());
            if (ExportStatus.of(t, now).isProcessed()) processedIds.add(t.getId());
        }
        Map<Long, Integer> photoCounts = new HashMap<>();
        for (List<Long> part : chunk(allIds)) {
            for (Object[] row : photoRepository.countGroupedByTicket(part)) {
                photoCounts.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
            }
        }
        Map<Long, TicketPhotoEntity> lastPhotos = new HashMap<>();
        for (List<Long> part : chunk(processedIds)) {
            for (TicketPhotoEntity p : photoRepository.findByTicketIdInOrderByIdAsc(part)) {
                lastPhotos.put(p.getTicketId(), p);   // ảnh có ID lớn nhất (mới nhất) ghi đè cuối cùng
            }
        }

        TicketWorkbookBuilder.ExportContext ctx = new TicketWorkbookBuilder.ExportContext(
                tickets, photoCounts, lastPhotos, userRepository.findAll(),
                me.getFullName(), now, staffFilter, onlyChanged, storage.getTtlMinutes());

        ExportFileStorage.Stored target;
        try {
            target = storage.newFile(me.getId());
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục lưu file xuất", e);
        }
        try (OutputStream out = Files.newOutputStream(target.path())) {
            workbookBuilder.write(ctx, out);
        } catch (IOException | RuntimeException e) {
            storage.discard(target);
            log.error("Lỗi khi xuất file Excel", e);
            throw new IllegalStateException("Không tạo được file Excel: " + e.getMessage(), e);
        }
        log.info("{} đã xuất file Excel {} ({} ca)", me.getUsername(), target.id(), tickets.size());

        return storage.find(me.getId(), target.id())
                .map(this::toDto)
                .orElseThrow(() -> new IllegalStateException("Không đọc lại được file vừa tạo"));
    }

    @Override
    public List<ExportFileDTO> listMyExports() {
        CurrentUserResponse me = requireAdmin();
        List<ExportFileDTO> result = new ArrayList<>();
        for (ExportFileStorage.Stored s : storage.list(me.getId())) {
            result.add(toDto(s));
        }
        return result;
    }

    @Override
    public ExportDownload openDownload(String id) {
        CurrentUserResponse me = requireAdmin();
        ExportFileStorage.Stored s = storage.find(me.getId(), id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File không còn tồn tại hoặc đã hết hạn (file chỉ được lưu " + storage.getTtlMinutes() + " phút)"));
        return new ExportDownload(s.path(), downloadName(s.id()), s.size());
    }

    @Override
    public void deleteExport(String id) {
        CurrentUserResponse me = requireAdmin();
        storage.delete(me.getId(), id);
    }

    // ---------- nội bộ ----------

    private CurrentUserResponse requireAdmin() {
        CurrentUserResponse me = authService.getCurrentLoggedUser();
        if (!me.isAdmin()) {
            throw new AccessDeniedException("Chỉ quản trị viên được xuất báo cáo Excel");
        }
        return me;
    }

    private ExportFileDTO toDto(ExportFileStorage.Stored s) {
        ExportFileDTO dto = new ExportFileDTO();
        dto.setId(s.id());
        dto.setFileName(downloadName(s.id()));
        dto.setSizeBytes(s.size());
        dto.setCreatedAt(s.createdAt());
        dto.setExpiresAt(s.expiresAt());
        dto.setSecondsLeft(Math.max(0, Duration.between(LocalDateTime.now(), s.expiresAt()).getSeconds()));
        dto.setTtlMinutes(storage.getTtlMinutes());
        return dto;
    }

    // yyyyMMdd_HHmmss_xxxxxxxx -> DanhSachCa_yyyyMMdd_HHmmss.xlsx
    private String downloadName(String id) {
        return "DanhSachCa_" + id.substring(0, 15) + ".xlsx";
    }

    private static List<List<Long>> chunk(List<Long> ids) {
        List<List<Long>> parts = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += CHUNK) {
            parts.add(ids.subList(i, Math.min(ids.size(), i + CHUNK)));
        }
        return parts;
    }
}
