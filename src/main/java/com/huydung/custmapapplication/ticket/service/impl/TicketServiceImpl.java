package com.huydung.custmapapplication.ticket.service.impl;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;
import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.common.dto.AbstractDTO;
import com.huydung.custmapapplication.common.exception.ImportConfirmationRequiredException;
import com.huydung.custmapapplication.common.exception.ResourceNotFoundException;
import com.huydung.custmapapplication.common.util.ExportMarker;
import com.huydung.custmapapplication.common.util.GeoUtils;
import com.huydung.custmapapplication.kmz.entity.KmzLocationEntity;
import com.huydung.custmapapplication.kmz.repository.KmzLocationRepository;
import com.huydung.custmapapplication.photo.repository.TicketPhotoRepository;
import com.huydung.custmapapplication.ticket.converter.TicketConverter;
import com.huydung.custmapapplication.ticket.dto.TicketImportResultDTO;
import com.huydung.custmapapplication.ticket.dto.TicketTypeInfoDTO;
import com.huydung.custmapapplication.ticket.dto.TicketResponseDTO;
import com.huydung.custmapapplication.ticket.dto.TicketSearchCriteria;
import com.huydung.custmapapplication.ticket.dto.UpdateTicketActionRequest;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.importer.ExcelHeader;
import com.huydung.custmapapplication.ticket.importer.RowKey;
import com.huydung.custmapapplication.ticket.importer.TicketFileFormat;
import com.huydung.custmapapplication.ticket.importer.TicketRowReader;
import com.huydung.custmapapplication.ticket.repository.TicketFilter;
import com.huydung.custmapapplication.ticket.repository.TicketRepository;
import com.huydung.custmapapplication.ticket.service.TicketService;
import com.huydung.custmapapplication.ticket.type.TicketGroup;
import com.huydung.custmapapplication.ticket.type.TicketType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements TicketService {

    private static final String STATUS_DONE = "DA_XU_LY";
    private static final String STATUS_PROCESSING = "DANG_XU_LY";
    private static final String STATUS_KLL = "KLL";
    private static final String REASON_DONE = "DONE";
    private static final String REASON_KLL = "KLL";
    private static final String REASON_NOT_IN_IMPORT = "NOT_IN_IMPORT";

    // Các trạng thái hợp lệ mà giao diện được phép gán cho ca (chặn giá trị tùy ý từ client)
    private static final Set<String> ALLOWED_STATUSES =
            Set.of("DANG_XU_LY", "HEN_GIO", "HEN_KHAC", "KLL", "DA_XU_LY");
    private static final int MAX_CALL_NOTE_LENGTH = 2000;

    // Nếu một lần import làm biến mất từ ngần này ca VÀ vượt quá tỉ lệ này so với số ca đang mở -> hỏi xác nhận
    private static final int ARCHIVE_CONFIRM_MIN_COUNT = 10;
    private static final double ARCHIVE_CONFIRM_RATIO = 0.5;

    // Phân trang: mặc định 10 ca/trang, tối đa 50
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private final TicketRepository ticketRepository;
    private final KmzLocationRepository kmzLocationRepository;
    private final AuthService authService;
    private final TicketConverter ticketConverter;
    private final TicketPhotoRepository ticketPhotoRepository;
    private final List<TicketFileFormat> fileFormats;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             KmzLocationRepository kmzLocationRepository,
                             AuthService authService,
                             TicketConverter ticketConverter,
                             TicketPhotoRepository ticketPhotoRepository,
                             List<TicketFileFormat> fileFormats) {
        this.ticketRepository = ticketRepository;
        this.kmzLocationRepository = kmzLocationRepository;
        this.authService = authService;
        this.ticketConverter = ticketConverter;
        this.ticketPhotoRepository = ticketPhotoRepository;
        this.fileFormats = fileFormats;
    }

    // ===================== IMPORT =====================

    @Override
    @Transactional
    public TicketImportResultDTO importExcelFile(MultipartFile file, boolean confirmArchive) {
        assertAdmin();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File Excel (.xlsx) không được để trống!");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "data.xlsx";
        int totalRead = 0, inserted = 0, updated = 0, restored = 0, duplicateInFile = 0;

        Map<String, CustomerTicketEntity> existingMap = ticketRepository.findAll().stream()
                .collect(Collectors.toMap(CustomerTicketEntity::getTicketCode, t -> t, (a, b) -> a));

        Set<String> codesInFile = new HashSet<>();
        List<CustomerTicketEntity> toSaveList = new ArrayList<>();
        Map<TicketType, Integer> rowsPerType = new EnumMap<>(TicketType.class);

        // Phạm vi của file: chỉ các loại ca mà định dạng file này bao phủ mới bị xét "không còn trong file"
        Set<TicketType> scope = Set.of();
        String formatName = "";
        List<String> warnings = List.of();
        long activeBefore = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // File báo cáo do hệ thống xuất có bố cục khác file dữ liệu gốc, import vào sẽ ghi sai dữ liệu
            if (ExportMarker.isExported(workbook)) {
                throw new IllegalArgumentException(
                        "Đây là file báo cáo do hệ thống xuất ra, không dùng để import. Hãy chọn file dữ liệu gốc.");
            }

            Sheet sheet = resolveTargetSheet(workbook);
            DataFormatter formatter = new DataFormatter();

            // Nhận diện định dạng file (phản ánh / hạ tầng...) từ dòng tiêu đề
            ExcelHeader header = ExcelHeader.from(sheet.getRow(0), formatter);
            TicketFileFormat format = null;
            for (TicketFileFormat f : fileFormats) {
                if (f.supports(header)) {
                    format = f;
                    break;
                }
            }
            if (format == null) {
                throw new IllegalArgumentException("Không nhận diện được định dạng file. File phản ánh cần có cột "
                        + "\"Mã phản ánh\"; file hạ tầng cần có cột \"Loại dữ liệu\" và \"Account / Thuê bao dịch vụ\".");
            }
            formatName = format.name();
            scope = format.coveredTypes();
            TicketRowReader reader = format.open(header, formatter);

            // Số ca đang mở thuộc phạm vi file, đếm TRƯỚC khi thêm ca mới (dùng cho cảnh báo import nhầm file)
            for (CustomerTicketEntity t : existingMap.values()) {
                if (t.getArchivedAt() == null && scope.contains(typeOf(t))) activeBefore++;
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                RowKey key = reader.readKey(row);
                if (key == null) continue;

                totalRead++;
                rowsPerType.merge(key.type(), 1, Integer::sum);
                String ticketCode = key.code();
                boolean firstOccurrence = codesInFile.add(ticketCode);
                if (!firstOccurrence) duplicateInFile++;

                CustomerTicketEntity entity = existingMap.get(ticketCode);
                boolean isNew = (entity == null);

                if (isNew) {
                    entity = new CustomerTicketEntity();
                    entity.setTicketCode(ticketCode);
                    entity.setAppStatus(STATUS_PROCESSING);
                    entity.setIsPinned(false);
                    inserted++;
                } else if (firstOccurrence) {
                    updated++;
                    // Ca từng bị lưu trữ vì "không còn trong file" nay xuất hiện lại -> đưa về danh sách chính
                    if (entity.getArchivedAt() != null && REASON_NOT_IN_IMPORT.equals(entity.getArchiveReason())) {
                        entity.setArchivedAt(null);
                        entity.setArchiveReason(null);
                        restored++;
                    }
                }

                // Giao cho bộ đọc của định dạng file map dữ liệu các cột Excel vào entity
                reader.fill(row, key, entity);
                if (firstOccurrence) toSaveList.add(entity);
                existingMap.put(ticketCode, entity);
            }
            warnings = reader.warnings();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi đọc file Excel (.xlsx): " + e.getMessage(), e);
        }

        // Tìm các ca cần chuyển sang Lịch sử: (1) đã bấm Đã xử lý hoặc KLL, (2) không còn trong file mới
        // (chỉ xét các ca cùng loại với file vừa import, ca loại khác giữ nguyên)
        List<CustomerTicketEntity> archiveDone = new ArrayList<>();
        List<CustomerTicketEntity> archiveMissing = new ArrayList<>();
        for (CustomerTicketEntity t : existingMap.values()) {
            if (t.getArchivedAt() != null) continue;
            if (STATUS_DONE.equals(t.getAppStatus()) || STATUS_KLL.equals(t.getAppStatus())) {
                archiveDone.add(t);
            } else if (scope.contains(typeOf(t)) && !codesInFile.contains(t.getTicketCode())) {
                archiveMissing.add(t);
            }
        }

        // Chống import nhầm file: nếu sắp ẩn quá nhiều ca thì dừng lại (transaction tự rollback) và hỏi xác nhận
        if (!confirmArchive
                && archiveMissing.size() >= ARCHIVE_CONFIRM_MIN_COUNT
                && archiveMissing.size() > activeBefore * ARCHIVE_CONFIRM_RATIO) {
            throw new ImportConfirmationRequiredException(archiveMissing.size(), (int) activeBefore);
        }

        LocalDateTime now = LocalDateTime.now();
        for (CustomerTicketEntity t : archiveDone) {
            t.setArchivedAt(now);
            t.setArchiveReason(STATUS_KLL.equals(t.getAppStatus()) ? REASON_KLL : REASON_DONE);
        }
        for (CustomerTicketEntity t : archiveMissing) {
            t.setArchivedAt(now);
            t.setArchiveReason(REASON_NOT_IN_IMPORT);
        }

        ticketRepository.saveAll(toSaveList);
        ticketRepository.saveAll(archiveDone);
        ticketRepository.saveAll(archiveMissing);

        int archived = archiveDone.size() + archiveMissing.size();
        StringBuilder msg = new StringBuilder(String.format(
                "Import thành công %s (%s): %d dòng (Mới: %d, Cập nhật: %d, Chuyển sang Lịch sử: %d, Khôi phục: %d).",
                fileName, formatName, totalRead, inserted, updated, archived, restored));
        if (rowsPerType.size() > 1 || (rowsPerType.size() == 1 && !rowsPerType.containsKey(TicketType.PHAN_ANH))) {
            msg.append(" Theo loại: ");
            msg.append(rowsPerType.entrySet().stream()
                    .map(e -> e.getKey().getLabel() + " " + e.getValue())
                    .collect(Collectors.joining(", ")));
            msg.append(".");
        }
        if (duplicateInFile > 0) {
            msg.append(" Có ").append(duplicateInFile).append(" dòng trùng mã trong file (chỉ giữ dòng cuối).");
        }
        if (!warnings.isEmpty()) {
            msg.append(" Cảnh báo: ").append(String.join(" | ", warnings)).append(".");
        }
        return new TicketImportResultDTO(fileName, totalRead, inserted, updated, archived, restored, msg.toString());
    }

    // Ca cũ chưa có loại coi như PHAN_ANH
    private static TicketType typeOf(CustomerTicketEntity t) {
        return t.getTicketType() != null ? t.getTicketType() : TicketType.PHAN_ANH;
    }

    // Từ tham số lọc của giao diện suy ra danh sách loại cần lấy: ticketType > ticketGroup > null (mọi loại)
    private List<TicketType> resolveTypes(TicketSearchCriteria criteria) {
        TicketType one = TicketType.fromCode(criteria.getTicketType());
        if (one != null) return List.of(one);
        TicketGroup group = TicketGroup.fromCode(criteria.getTicketGroup());
        if (group != null) return TicketType.ofGroup(group);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeInfoDTO> getTicketTypeSummary(String staffFilter) {
        Map<TicketType, Long> counts = ticketRepository.countOpenByType(resolveEffectiveStaffFilter(staffFilter));
        List<TicketTypeInfoDTO> result = new ArrayList<>();
        for (TicketType t : TicketType.values()) {
            result.add(new TicketTypeInfoDTO(t.name(), t.getLabel(), t.getGroup().name(),
                    t.getGroup().getLabel(), t.getGroup().getIcon(), t.getColor(), counts.getOrDefault(t, 0L)));
        }
        return result;
    }

    // ===================== DANH SÁCH CA (CÓ PHÂN TRANG) =====================

    @Override
    @Transactional(readOnly = true)
    public AbstractDTO<TicketResponseDTO> getTicketsForCurrentUser(TicketSearchCriteria criteria) {
        String sort = criteria.getSort() == null ? "DEFAULT" : criteria.getSort().trim().toUpperCase();
        boolean nearest = "NEAREST".equals(sort);
        boolean byAppointment = "APPOINTMENT".equals(sort);

        if (nearest) {
            Double lat = criteria.getLat();
            Double lng = criteria.getLng();
            if (lat == null || lat < -90 || lat > 90 || lng == null || lng < -180 || lng > 180) {
                throw new IllegalArgumentException("Thiếu hoặc sai vị trí hiện tại để sắp xếp theo khoảng cách");
            }
        }

        String effectiveStaff = resolveEffectiveStaffFilter(criteria.getStaffFilter());
        TicketFilter filter = new TicketFilter(
                effectiveStaff,
                criteria.getKeyword(),
                criteria.getAppStatus(),
                criteria.getOnlyPinned(),
                criteria.getArea(),
                criteria.getCabinetPrefix(),
                criteria.getPriority(),
                false,
                resolveTypes(criteria)
        );

        int size = normalizeSize(criteria.getSize());
        List<TicketResponseDTO> pageItems;
        int page;
        long totalItem;

        if (nearest) {
            // Khoảng cách tính trong bộ nhớ (toạ độ nằm ở bảng KMZ) nên lấy hết ca thỏa bộ lọc, sắp xếp rồi mới cắt trang
            List<CustomerTicketEntity> all = ticketRepository.findTicketsCustom(filter, false, 0, 0);
            List<TicketResponseDTO> dtos = toResponseDtos(all);

            double myLat = criteria.getLat();
            double myLng = criteria.getLng();
            for (TicketResponseDTO dto : dtos) {
                if (dto.getLatitude() != null && dto.getLongitude() != null) {
                    double km = GeoUtils.distanceKm(myLat, myLng, dto.getLatitude(), dto.getLongitude());
                    dto.setDistanceKm(Math.round(km * 10.0) / 10.0);
                }
            }
            // Ca đã ghim vẫn lên đầu, ca chưa có toạ độ xuống cuối
            dtos.sort(Comparator
                    .comparing((TicketResponseDTO d) -> !Boolean.TRUE.equals(d.getIsPinned()))
                    .thenComparing(d -> d.getDistanceKm() == null ? Double.MAX_VALUE : d.getDistanceKm())
                    .thenComparing(TicketResponseDTO::getId));

            totalItem = dtos.size();
            page = clampPage(criteria.getPage(), totalItem, size);
            int from = Math.min((page - 1) * size, dtos.size());
            int to = Math.min(from + size, dtos.size());
            pageItems = new ArrayList<>(dtos.subList(from, to));
        } else {
            totalItem = ticketRepository.countTicketsCustom(filter);
            page = clampPage(criteria.getPage(), totalItem, size);
            List<CustomerTicketEntity> entities = ticketRepository.findTicketsCustom(
                    filter, byAppointment, (page - 1) * size, size);
            pageItems = toResponseDtos(entities);
        }

        return AbstractDTO.of(pageItems, page, size, totalItem);
    }

    // ===================== DANH SÁCH CA ĐÃ HẸN (CÓ PHÂN TRANG) =====================

    @Override
    @Transactional(readOnly = true)
    public AbstractDTO<TicketResponseDTO> getAppointmentsForCurrentUser(TicketSearchCriteria criteria) {
        // Chỉ nhận lọc riêng theo HEN_GIO hoặc HEN_KHAC; giá trị khác coi như lấy cả hai loại
        String status = criteria.getAppStatus() == null ? null : criteria.getAppStatus().trim().toUpperCase();
        if (!"HEN_GIO".equals(status) && !"HEN_KHAC".equals(status)) {
            status = null;
        }

        TicketFilter filter = new TicketFilter(
                resolveEffectiveStaffFilter(criteria.getStaffFilter()),
                criteria.getKeyword(),
                status,
                false,
                criteria.getArea(),
                criteria.getCabinetPrefix(),
                criteria.getPriority(),
                true,
                resolveTypes(criteria)
        );

        int size = normalizeSize(criteria.getSize());
        long totalItem = ticketRepository.countTicketsCustom(filter);
        int page = clampPage(criteria.getPage(), totalItem, size);
        // Luôn xếp theo giờ hẹn (ca ghim lên đầu, ca chưa có giờ hẹn xuống cuối)
        List<CustomerTicketEntity> entities = ticketRepository.findTicketsCustom(filter, true, (page - 1) * size, size);

        return AbstractDTO.of(toResponseDtos(entities), page, size, totalItem);
    }

    // Chuyển entity sang DTO kèm số lần trùng thuê bao và toạ độ tủ (chỉ tra cứu cho các ca đang cần hiển thị)
    private List<TicketResponseDTO> toResponseDtos(List<CustomerTicketEntity> tickets) {
        if (tickets.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Map đếm trùng mã thuê bao (d500_gftth_...)
        Set<String> accounts = tickets.stream()
                .map(CustomerTicketEntity::getSubscriberAccount)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Integer> dupMap = ticketRepository.countDuplicateSubscribers(accounts);

        // 2. Map tọa độ KMZ theo Mã tủ
        Set<String> cabinets = tickets.stream()
                .map(CustomerTicketEntity::getCabinetCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, KmzLocationEntity> kmzMap = kmzLocationRepository.findMapByCabinetCodes(cabinets);

        // 3. Số ảnh hiện trường đã chụp của từng ca (hiện trên nút camera)
        Set<Long> ticketIds = tickets.stream()
                .map(CustomerTicketEntity::getId)
                .collect(Collectors.toSet());
        Map<Long, Integer> photoCounts = new HashMap<>();
        for (Object[] row : ticketPhotoRepository.countGroupedByTicket(ticketIds)) {
            photoCounts.put((Long) row[0], ((Number) row[1]).intValue());
        }

        // 4. Dùng TicketConverter chuyển đổi sang DTO
        return tickets.stream().map(entity -> {
            int dupCount = (entity.getSubscriberAccount() != null && typeOf(entity) == TicketType.PHAN_ANH)
                    ? dupMap.getOrDefault(entity.getSubscriberAccount().trim().toLowerCase(), 1) : 1;
            KmzLocationEntity kmz = (entity.getCabinetCode() != null)
                    ? kmzMap.get(entity.getCabinetCode().trim().toUpperCase()) : null;

            TicketResponseDTO dto = ticketConverter.toResponseDTO(entity, dupCount, kmz);
            dto.setPhotoCount(photoCounts.getOrDefault(entity.getId(), 0));
            return dto;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    // ===================== LỊCH SỬ (CÓ PHÂN TRANG) =====================

    @Override
    @Transactional(readOnly = true)
    public AbstractDTO<TicketResponseDTO> getHistoryForCurrentUser(String staffFilter, String keyword,
                                                                   LocalDate fromDate, LocalDate toDate,
                                                                   Integer pageParam, Integer sizeParam) {
        String effectiveStaff = resolveEffectiveStaffFilter(staffFilter);
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toExclusive = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;

        int size = normalizeSize(sizeParam);
        long totalItem = ticketRepository.countHistoryTickets(effectiveStaff, keyword, from, toExclusive);
        int page = clampPage(pageParam, totalItem, size);

        List<TicketResponseDTO> items = ticketRepository
                .findHistoryTickets(effectiveStaff, keyword, from, toExclusive, (page - 1) * size, size)
                .stream()
                .map(ticketConverter::toActionResponseDTO)
                .collect(Collectors.toList());

        return AbstractDTO.of(items, page, size, totalItem);
    }

    // Số ca mỗi trang: mặc định 10, giới hạn 1..50
    private int normalizeSize(Integer size) {
        if (size == null || size < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    // Đưa số trang về khoảng hợp lệ (1..tổng số trang). Nhờ vậy nếu đang ở trang 2 mà bộ lọc chỉ còn 3 ca,
    // kết quả tự về trang 1 thay vì trả danh sách rỗng
    private int clampPage(Integer page, long totalItem, int size) {
        int totalPage = Math.max(1, (int) Math.ceil((double) totalItem / size));
        int p = (page == null || page < 1) ? 1 : page;
        return Math.min(p, totalPage);
    }

    @Override
    @Transactional
    public TicketResponseDTO restoreTicket(Long ticketId) {
        CustomerTicketEntity entity = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca phản ánh ID: " + ticketId));
        assertCanModify(entity);

        if (entity.getArchivedAt() == null) {
            throw new IllegalArgumentException("Ca này đang nằm trong danh sách chính");
        }

        entity.setArchivedAt(null);
        entity.setArchiveReason(null);
        if (STATUS_DONE.equals(entity.getAppStatus()) || STATUS_KLL.equals(entity.getAppStatus())) {
            entity.setAppStatus(STATUS_PROCESSING);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return ticketConverter.toActionResponseDTO(ticketRepository.save(entity));
    }

    // ===================== CẬP NHẬT CA =====================

    @Override
    @Transactional
    public TicketResponseDTO updateTicketAction(Long ticketId, UpdateTicketActionRequest request) {
        CustomerTicketEntity entity = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca phản ánh ID: " + ticketId));

        assertCanModify(entity);

        if (entity.getArchivedAt() != null) {
            throw new IllegalArgumentException("Ca này đã nằm trong Lịch sử, hãy đưa về danh sách trước khi xử lý");
        }
        validateActionRequest(request);
        if (request.getAppointmentStart() != null && request.getAppointmentEnd() != null
                && !request.getAppointmentEnd().isAfter(request.getAppointmentStart())) {
            throw new IllegalArgumentException("Giờ kết thúc hẹn phải sau giờ bắt đầu");
        }

        ticketConverter.updateEntityFromRequest(request, entity);
        applyAppointment(request, entity);

        // Bấm "Đã xử lý" hoặc "KLL" (không liên lạc được) thì chuyển ca sang Lịch sử ngay
        if (entity.getArchivedAt() == null) {
            if (STATUS_DONE.equals(entity.getAppStatus())) {
                entity.setArchivedAt(LocalDateTime.now());
                entity.setArchiveReason(REASON_DONE);
            } else if (STATUS_KLL.equals(entity.getAppStatus())) {
                entity.setArchivedAt(LocalDateTime.now());
                entity.setArchiveReason(REASON_KLL);
            }
        }

        return ticketConverter.toActionResponseDTO(ticketRepository.save(entity));
    }

    // Xóa giờ hẹn nếu được yêu cầu; có giờ hẹn mới thì tự tạo chuỗi hiển thị cho thẻ
    private void applyAppointment(UpdateTicketActionRequest request, CustomerTicketEntity entity) {
        if (Boolean.TRUE.equals(request.getClearAppointment())) {
            entity.setAppointmentStart(null);
            entity.setAppointmentEnd(null);
            entity.setAppointmentTime("");
            return;
        }
        if (request.getAppointmentStart() != null) {
            // Gán trực tiếp cả giờ bắt đầu lẫn kết thúc: ModelMapper bỏ qua giá trị null nên nếu chỉ dựa vào nó,
            // giờ kết thúc cũ sẽ còn nguyên khi người dùng đặt lại giờ hẹn không có giờ kết thúc
            entity.setAppointmentStart(request.getAppointmentStart());
            entity.setAppointmentEnd(request.getAppointmentEnd());
            entity.setAppointmentTime(buildAppointmentLabel(request.getAppointmentStart(), request.getAppointmentEnd()));
        }
    }

    // Kiểm tra dữ liệu cập nhật ca: trạng thái phải thuộc danh sách cho phép, ghi chú không quá dài
    private void validateActionRequest(UpdateTicketActionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu cập nhật");
        }
        if (request.getAppStatus() != null) {
            String status = request.getAppStatus().trim().toUpperCase();
            if (status.isEmpty()) {
                request.setAppStatus(null);
            } else if (!ALLOWED_STATUSES.contains(status)) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ");
            } else {
                request.setAppStatus(status);
            }
        }
        if (request.getCallNote() != null && request.getCallNote().length() > MAX_CALL_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú quá dài (tối đa " + MAX_CALL_NOTE_LENGTH + " ký tự)");
        }
    }

    private String buildAppointmentLabel(LocalDateTime start, LocalDateTime end) {
        if (end == null) {
            return start.format(TIME_FMT) + " • " + start.format(DAY_FMT);
        }
        if (start.toLocalDate().equals(end.toLocalDate())) {
            return start.format(TIME_FMT) + " - " + end.format(TIME_FMT) + " • " + start.format(DAY_FMT);
        }
        return start.format(TIME_FMT) + " " + start.format(DAY_FMT) + " - " + end.format(TIME_FMT) + " " + end.format(DAY_FMT);
    }

    // ===================== DANH MỤC LỌC =====================

    @Override
    @Transactional(readOnly = true)
    public List<String> getAssignedStaffList() {
        assertAdmin();
        return ticketRepository.findAllDistinctAssignedStaff();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAreaListForStaff(String staffName) {
        // STAFF chỉ lấy được khu vực của chính mình, ADMIN chọn được từng nhân viên hoặc tất cả
        return ticketRepository.findAllAreasByStaff(resolveEffectiveStaffFilter(staffName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCabinetPrefixList(String staffName, String area) {
        return ticketRepository.findAllCabinetPrefixes(resolveEffectiveStaffFilter(staffName), area);
    }

    // ===================== HỖ TRỢ =====================

    // Xác định sheet chứa dữ liệu chi tiết trong file Excel
    private Sheet resolveTargetSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet("chi tiêt");
        if (sheet == null) sheet = workbook.getSheet("chi tiết");
        if (sheet == null) {
            sheet = workbook.getNumberOfSheets() > 1 ? workbook.getSheetAt(1) : workbook.getSheetAt(0);
        }
        return sheet;
    }

    // Xác định nhân viên cần lọc dựa trên Account đang đăng nhập
    private String resolveEffectiveStaffFilter(String staffFilter) {
        CurrentUserResponse currentUser = authService.getCurrentLoggedUser();
        String effectiveStaff = currentUser.getFullName();

        if (currentUser.isAdmin()) {
            if (staffFilter != null && !staffFilter.trim().isEmpty()) {
                effectiveStaff = staffFilter.trim();
            } else if ("admin".equalsIgnoreCase(currentUser.getUsername())) {
                effectiveStaff = "ALL";
            }
        }
        return effectiveStaff;
    }

    private void assertAdmin() {
        if (!authService.getCurrentLoggedUser().isAdmin()) {
            throw new AccessDeniedException("Chức năng này chỉ dành cho quản trị viên");
        }
    }

    // ADMIN được xử lý mọi ca; STAFF chỉ được xử lý ca có "Giao nhân viên" trùng họ tên của mình
    private void assertCanModify(CustomerTicketEntity entity) {
        CurrentUserResponse currentUser = authService.getCurrentLoggedUser();
        if (currentUser.isAdmin()) return;

        String owner = entity.getAssignedStaff() == null ? "" : entity.getAssignedStaff().trim();
        String me = currentUser.getFullName() == null ? "" : currentUser.getFullName().trim();
        if (!owner.equalsIgnoreCase(me)) {
            throw new AccessDeniedException("Bạn không có quyền xử lý ca phản ánh này");
        }
    }
}
