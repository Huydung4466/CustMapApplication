package com.huydung.custmapapplication.photo.service.impl;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;
import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.common.exception.ResourceNotFoundException;
import com.huydung.custmapapplication.photo.dto.*;
import com.huydung.custmapapplication.photo.entity.TicketPhotoEntity;
import com.huydung.custmapapplication.photo.repository.TicketPhotoRepository;
import com.huydung.custmapapplication.photo.service.TicketPhotoService;
import com.huydung.custmapapplication.setting.entity.AppSettingEntity;
import com.huydung.custmapapplication.setting.repository.AppSettingRepository;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ảnh hiện trường.
 * - LƯU, XÓA, TẢI VỀ: chỉ tài khoản có quyền Camera (ROLE_CAMERA). Quyền được kiểm tra ở đây (ngoài SecurityConfig).
 * - XEM (danh sách, ảnh, ảnh thu nhỏ): mọi tài khoản đăng nhập.
 * - Ảnh mới lưu ở: app.photo.dir/<tên đăng nhập người chụp>/yyyy/MM/dd/<uuid>.jpg. Ảnh cũ giữ nguyên chỗ cũ (database lưu đường dẫn tương đối).
 * - Ảnh mới có hạn tự xóa = giờ chụp + app.photo.retention-hours (72 giờ); ảnh cũ không có hạn nên không bị xóa.
 *   Việc tự xóa có thể bật / tắt bởi tài khoản Camera (cài đặt chung, lưu ở bảng app_settings).
 */
@Service
public class TicketPhotoServiceImpl implements TicketPhotoService {

    private static final Logger log = LoggerFactory.getLogger(TicketPhotoServiceImpl.class);

    private static final long MAX_FILE_BYTES = 8L * 1024 * 1024;   // giao diện gửi ~0,3-0,6 MB; 8 MB là trần an toàn
    private static final int MAX_DIMENSION = 6000;                 // px, chặn ảnh có kích thước bất thường
    private static final int MAX_ADDRESS_LENGTH = 500;
    private static final int GALLERY_PAGE_SIZE = 18;
    private static final int THUMB_MAX_EDGE = 480;                 // px, cạnh dài của ảnh thu nhỏ trong lưới
    private static final int PURGE_BATCH = 200;
    private static final String KEY_AUTO_DELETE = "photo.autoDelete";
    private static final Set<String> ADDRESS_SOURCES = Set.of("GEOCODE", "TICKET");
    private static final DateTimeFormatter DIR_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final TicketPhotoRepository photoRepository;
    private final TicketRepository ticketRepository;
    private final AppSettingRepository settingRepository;
    private final AuthService authService;
    private final Path baseDir;
    private final int maxPerTicket;
    private final int retentionHours;

    public TicketPhotoServiceImpl(TicketPhotoRepository photoRepository,
                                  TicketRepository ticketRepository,
                                  AppSettingRepository settingRepository,
                                  AuthService authService,
                                  @Value("${app.photo.dir:./data/photos}") String photoDir,
                                  @Value("${app.photo.max-per-ticket:10}") int maxPerTicket,
                                  @Value("${app.photo.retention-hours:72}") int retentionHours) {
        this.photoRepository = photoRepository;
        this.ticketRepository = ticketRepository;
        this.settingRepository = settingRepository;
        this.authService = authService;
        this.baseDir = Paths.get(photoDir).toAbsolutePath().normalize();
        this.maxPerTicket = Math.max(1, maxPerTicket);
        this.retentionHours = Math.max(1, retentionHours);
        log.info("Thư mục lưu ảnh hiện trường: {} (tự xóa sau {} giờ khi đang bật)", this.baseDir, this.retentionHours);
    }

    // ===================== LƯU ẢNH =====================

    @Override
    @Transactional
    public TicketPhotoDTO upload(Long ticketId, MultipartFile file,
                                 Double latitude, Double longitude, Double accuracyM,
                                 String address, String addressSource) {
        CurrentUserResponse user = requireCamera();
        CustomerTicketEntity ticket = findTicketAndCheckAccess(ticketId, user);
        if (photoRepository.countByTicketId(ticketId) >= maxPerTicket) {
            throw new IllegalArgumentException("Mỗi ca chỉ lưu tối đa " + maxPerTicket + " ảnh");
        }
        return store(ticket.getId(), file, latitude, longitude, accuracyM, address, addressSource, user);
    }

    @Override
    @Transactional
    public TicketPhotoDTO uploadFree(MultipartFile file,
                                     Double latitude, Double longitude, Double accuracyM,
                                     String address, String addressSource) {
        CurrentUserResponse user = requireCamera();
        return store(null, file, latitude, longitude, accuracyM, address, addressSource, user);
    }

    private TicketPhotoDTO store(Long ticketId, MultipartFile file,
                                 Double latitude, Double longitude, Double accuracyM,
                                 String address, String addressSource, CurrentUserResponse user) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Chưa có ảnh để lưu");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Ảnh quá lớn (tối đa 8 MB)");
        }
        validateGps(latitude, longitude, accuracyM);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Không đọc được dữ liệu ảnh tải lên", e);
            throw new IllegalStateException("Không đọc được ảnh tải lên, vui lòng thử lại");
        }
        validateJpeg(bytes);

        // Tên file do server tự sinh, không dùng tên file từ client; ảnh nằm trong thư mục riêng của người chụp
        LocalDateTime now = LocalDateTime.now();
        String relativePath = ownerFolder(user) + "/" + now.format(DIR_FMT) + "/" + UUID.randomUUID() + ".jpg";
        Path target = resolveSafe(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            log.error("Không ghi được file ảnh vào {}", target, e);
            throw new IllegalStateException("Không lưu được file ảnh, vui lòng thử lại");
        }
        deleteFileIfNotCommitted(target);

        String cleanAddress = cleanAddress(address);

        TicketPhotoEntity photo = new TicketPhotoEntity();
        photo.setTicketId(ticketId);
        photo.setFilePath(relativePath);
        photo.setFileSize((long) bytes.length);
        photo.setTakenAt(now);
        photo.setExpiresAt(now.plusHours(retentionHours));
        photo.setLatitude(latitude);
        photo.setLongitude(longitude);
        photo.setAccuracyM(latitude == null ? null : accuracyM);
        photo.setAddress(cleanAddress);
        photo.setAddressSource(cleanAddress == null ? null : normalizeSource(addressSource));
        photo.setTakenByUserId(user.getId());
        photo.setTakenByName(user.getFullName());
        photo.setTakenByUsername(user.getUsername());

        return toDTO(photoRepository.save(photo), now);
    }

    // ===================== XÓA ẢNH =====================

    @Override
    @Transactional
    public void deletePhoto(Long photoId) {
        requireCamera();
        TicketPhotoEntity photo = findPhoto(photoId);
        List<Path> files = new ArrayList<>();
        try {
            files.add(resolveSafe(photo.getFilePath()));
        } catch (IllegalStateException e) {
            log.warn("Bỏ qua file ảnh có đường dẫn không hợp lệ: {}", photo.getFilePath());
        }
        photoRepository.delete(photo);
        deleteFilesAfterCommit(files);
    }

    // ===================== XEM ẢNH =====================

    @Override
    @Transactional(readOnly = true)
    public List<TicketPhotoDTO> listPhotos(Long ticketId) {
        findTicketAndCheckAccess(ticketId, authService.getCurrentLoggedUser());
        LocalDateTime now = LocalDateTime.now();
        return photoRepository.findByTicketIdOrderByIdDesc(ticketId).stream()
                .map(p -> toDTO(p, now))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadPhotoFile(Long ticketId, Long photoId) {
        findTicketAndCheckAccess(ticketId, authService.getCurrentLoggedUser());
        TicketPhotoEntity photo = photoRepository.findByIdAndTicketId(photoId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));
        return fileOf(photo);
    }

    @Override
    @Transactional(readOnly = true)
    public PhotoPageDTO listGallery(Long ownerUserId, int page) {
        authService.getCurrentLoggedUser();   // yêu cầu đã đăng nhập
        Pageable pageable = PageRequest.of(Math.max(page, 0), GALLERY_PAGE_SIZE);
        Page<TicketPhotoEntity> result = ownerUserId == null
                ? photoRepository.findAllByOrderByIdDesc(pageable)
                : photoRepository.findByTakenByUserIdOrderByIdDesc(ownerUserId, pageable);
        LocalDateTime now = LocalDateTime.now();
        List<TicketPhotoDTO> items = result.getContent().stream().map(p -> toDTO(p, now)).collect(Collectors.toList());
        return new PhotoPageDTO(items, result.hasNext(), isAutoDeleteEnabled(), retentionHours);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoOwnerDTO> listOwners() {
        authService.getCurrentLoggedUser();
        List<PhotoOwnerDTO> owners = new ArrayList<>();
        for (Object[] row : photoRepository.countGroupedByOwner()) {
            owners.add(new PhotoOwnerDTO((Long) row[0], (String) row[1], (String) row[2], ((Number) row[3]).longValue()));
        }
        return owners;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadGalleryFile(Long photoId) {
        authService.getCurrentLoggedUser();
        return fileOf(findPhoto(photoId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] loadThumbnail(Long photoId) {
        authService.getCurrentLoggedUser();
        Path file = resolveSafe(findPhoto(photoId).getFilePath());
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("File ảnh không còn trên máy chủ");
        }
        try {
            BufferedImage src = ImageIO.read(file.toFile());
            if (src == null) {
                return Files.readAllBytes(file);    // không đọc được dạng ảnh: trả file gốc
            }
            int w = src.getWidth();
            int h = src.getHeight();
            int longest = Math.max(w, h);
            if (longest <= THUMB_MAX_EDGE) {
                return Files.readAllBytes(file);    // đã nhỏ sẵn
            }
            double scale = (double) THUMB_MAX_EDGE / longest;
            int tw = Math.max(1, (int) Math.round(w * scale));
            int th = Math.max(1, (int) Math.round(h * scale));
            // Thu nhỏ từng bước (mỗi bước tối đa một nửa) để ảnh mượt, không bị răng cưa
            BufferedImage cur = src;
            while (cur.getWidth() / 2 >= tw && cur.getHeight() / 2 >= th) {
                cur = resize(cur, cur.getWidth() / 2, cur.getHeight() / 2);
            }
            cur = resize(cur, tw, th);
            return encodeJpeg(cur, 0.8f);
        } catch (IOException e) {
            log.warn("Không tạo được ảnh thu nhỏ cho ảnh {}", photoId, e);
            throw new IllegalStateException("Không tạo được ảnh thu nhỏ");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PhotoDownload loadForDownload(Long photoId) {
        requireCamera();
        TicketPhotoEntity photo = findPhoto(photoId);
        String owner = photo.getTakenByUsername() != null ? photo.getTakenByUsername() : "anh";
        String name = safeFileName(owner) + "_" + photo.getTakenAt().format(STAMP_FMT) + "_" + photo.getId() + ".jpg";
        return new PhotoDownload(fileOf(photo), name);
    }

    @Override
    public void assertCanDownload() {
        requireCamera();
    }

    // ===================== TỰ XÓA ẢNH QUÁ HẠN =====================

    @Override
    @Transactional(readOnly = true)
    public PhotoSettingsDTO getSettings() {
        authService.getCurrentLoggedUser();
        return new PhotoSettingsDTO(isAutoDeleteEnabled(), retentionHours);
    }

    @Override
    @Transactional
    public PhotoSettingsDTO setAutoDeleteEnabled(boolean enabled) {
        requireCamera();
        AppSettingEntity entity = settingRepository.findBySettingKey(KEY_AUTO_DELETE).orElseGet(() -> {
            AppSettingEntity e = new AppSettingEntity();
            e.setSettingKey(KEY_AUTO_DELETE);
            return e;
        });
        entity.setSettingValue(String.valueOf(enabled));
        settingRepository.save(entity);
        return new PhotoSettingsDTO(enabled, retentionHours);
    }

    // Mặc định BẬT khi chưa có cài đặt nào
    private boolean isAutoDeleteEnabled() {
        return settingRepository.findBySettingKey(KEY_AUTO_DELETE)
                .map(e -> Boolean.parseBoolean(e.getSettingValue()))
                .orElse(true);
    }

    @Override
    @Transactional
    public int purgeExpired() {
        if (!isAutoDeleteEnabled()) {
            return 0;
        }
        List<TicketPhotoEntity> expired = photoRepository.findByExpiresAtBeforeOrderByIdAsc(
                LocalDateTime.now(), PageRequest.of(0, PURGE_BATCH));
        if (expired.isEmpty()) {
            return 0;
        }
        List<Path> files = new ArrayList<>();
        for (TicketPhotoEntity p : expired) {
            try {
                files.add(resolveSafe(p.getFilePath()));
            } catch (IllegalStateException e) {
                log.warn("Bỏ qua file ảnh có đường dẫn không hợp lệ: {}", p.getFilePath());
            }
        }
        photoRepository.deleteAllInBatch(expired);
        deleteFilesAfterCommit(files);
        return expired.size();
    }

    @Override
    @Transactional
    public int deleteByTicketIds(Collection<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) return 0;

        List<Long> all = new ArrayList<>(ticketIds);
        List<TicketPhotoEntity> photos = new ArrayList<>();
        for (int i = 0; i < all.size(); i += 1000) {
            photos.addAll(photoRepository.findByTicketIdInOrderByIdAsc(all.subList(i, Math.min(i + 1000, all.size()))));
        }
        if (photos.isEmpty()) return 0;

        // Lấy đường dẫn file trước khi xóa dòng database
        List<Path> files = new ArrayList<>();
        for (TicketPhotoEntity p : photos) {
            try {
                files.add(resolveSafe(p.getFilePath()));
            } catch (IllegalStateException e) {
                log.warn("Bỏ qua file ảnh có đường dẫn không hợp lệ: {}", p.getFilePath());
            }
        }

        for (int i = 0; i < photos.size(); i += 500) {
            photoRepository.deleteAllInBatch(photos.subList(i, Math.min(i + 500, photos.size())));
        }
        deleteFilesAfterCommit(files);
        return photos.size();
    }

    // ===================== HỖ TRỢ =====================

    // Chỉ tài khoản có quyền Camera được lưu / xóa / tải về ảnh
    private CurrentUserResponse requireCamera() {
        CurrentUserResponse user = authService.getCurrentLoggedUser();
        if (!user.isCamera()) {
            throw new AccessDeniedException("Chỉ tài khoản có quyền Camera mới thực hiện được thao tác này");
        }
        return user;
    }

    private TicketPhotoEntity findPhoto(Long photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));
    }

    private Resource fileOf(TicketPhotoEntity photo) {
        Path file = resolveSafe(photo.getFilePath());
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("File ảnh không còn trên máy chủ");
        }
        return new FileSystemResource(file);
    }

    // ADMIN được truy cập mọi ca; STAFF chỉ ca có "Giao nhân viên" trùng họ tên của mình
    private CustomerTicketEntity findTicketAndCheckAccess(Long ticketId, CurrentUserResponse user) {
        CustomerTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca phản ánh ID: " + ticketId));

        if (!user.isAdmin()) {
            String owner = ticket.getAssignedStaff() == null ? "" : ticket.getAssignedStaff().trim();
            String me = user.getFullName() == null ? "" : user.getFullName().trim();
            if (!owner.equalsIgnoreCase(me)) {
                throw new AccessDeniedException("Bạn không có quyền với ca phản ánh này");
            }
        }
        return ticket;
    }

    // Tên thư mục của người chụp: tên đăng nhập, chỉ giữ chữ, số, dấu chấm, gạch ngang, gạch dưới (chặn ký tự lạ / thoát thư mục)
    private String ownerFolder(CurrentUserResponse user) {
        String name = user.getUsername() == null ? "" : user.getUsername().trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.length() > 60) name = name.substring(0, 60);
        if (name.isEmpty() || name.startsWith(".")) name = "u_" + user.getId();
        return name;
    }

    private String safeFileName(String s) {
        String n = s == null ? "" : s.replaceAll("[^A-Za-z0-9._-]", "_");
        return n.isEmpty() ? "anh" : n;
    }

    // Chỉ nhận JPEG thật (kiểm tra byte đầu file và đọc kích thước từ header), không tin Content-Type của client
    private void validateJpeg(byte[] bytes) {
        boolean jpegMagic = bytes.length > 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        if (!jpegMagic) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPEG");
        }
        try (ImageInputStream iis = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("jpeg");
            if (!readers.hasNext()) {
                return; // máy chủ không có bộ đọc JPEG: bỏ qua bước kiểm tra kích thước
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w < 1 || h < 1 || w > MAX_DIMENSION || h > MAX_DIMENSION) {
                    throw new IllegalArgumentException("Kích thước ảnh không hợp lệ");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Ảnh không hợp lệ hoặc bị lỗi");
        }
    }

    private void validateGps(Double lat, Double lng, Double accuracy) {
        if ((lat == null) != (lng == null)) {
            throw new IllegalArgumentException("Thiếu kinh độ hoặc vĩ độ");
        }
        if (lat != null && (!Double.isFinite(lat) || lat < -90 || lat > 90
                || !Double.isFinite(lng) || lng < -180 || lng > 180)) {
            throw new IllegalArgumentException("Tọa độ không hợp lệ");
        }
        if (accuracy != null && (!Double.isFinite(accuracy) || accuracy < 0)) {
            throw new IllegalArgumentException("Độ chính xác GPS không hợp lệ");
        }
    }

    // Gom khoảng trắng/xuống dòng, bỏ ký tự điều khiển, cắt độ dài; rỗng thì trả null
    private String cleanAddress(String address) {
        if (address == null) return null;
        String s = address.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        if (s.isEmpty()) return null;
        return s.length() > MAX_ADDRESS_LENGTH ? s.substring(0, MAX_ADDRESS_LENGTH) : s;
    }

    private String normalizeSource(String source) {
        if (source == null) return null;
        String s = source.trim().toUpperCase();
        return ADDRESS_SOURCES.contains(s) ? s : null;
    }

    // Chỉ xóa file khi giao dịch đã commit thành công (nếu bị hủy thì ảnh vẫn còn đủ cả file lẫn dòng database)
    private void deleteFilesAfterCommit(List<Path> files) {
        Runnable deleteAll = () -> {
            for (Path f : files) {
                try {
                    Files.deleteIfExists(f);
                } catch (IOException e) {
                    log.warn("Không xóa được file ảnh {}", f, e);
                    continue;
                }
                removeEmptyParents(f);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteAll.run();
                }
            });
        } else {
            deleteAll.run();
        }
    }

    // Dọn các thư mục ngày / tháng / năm / người chụp đã trống sau khi xóa ảnh (dừng ở thư mục gốc hoặc thư mục còn file)
    private void removeEmptyParents(Path file) {
        Path dir = file.getParent();
        while (dir != null && dir.startsWith(baseDir) && !dir.equals(baseDir)) {
            try {
                Files.delete(dir);    // chỉ xóa được thư mục trống; thư mục còn file sẽ ném lỗi và dừng vòng lặp
            } catch (IOException e) {
                break;
            }
            dir = dir.getParent();
        }
    }

    // Ghép đường dẫn tương đối với thư mục gốc và chắc chắn kết quả không thoát ra ngoài thư mục gốc
    private Path resolveSafe(String relativePath) {
        Path p = baseDir.resolve(relativePath).normalize();
        if (!p.startsWith(baseDir)) {
            throw new IllegalStateException("Đường dẫn ảnh không hợp lệ");
        }
        return p;
    }

    // Nếu giao dịch DB bị hủy thì xóa luôn file vừa ghi để không để lại file mồ côi
    private void deleteFileIfNotCommitted(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException e) {
                        log.warn("Không xóa được file ảnh mồ côi {}", target, e);
                    }
                }
            }
        });
    }

    private BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private byte[] encodeJpeg(BufferedImage img, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(img, null, null), param);
            }
            return bos.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private TicketPhotoDTO toDTO(TicketPhotoEntity p, LocalDateTime now) {
        TicketPhotoDTO dto = new TicketPhotoDTO();
        dto.setId(p.getId());
        dto.setUpdatedAt(p.getUpdatedAt());
        dto.setTicketId(p.getTicketId());
        dto.setTakenAt(p.getTakenAt());
        dto.setLatitude(p.getLatitude());
        dto.setLongitude(p.getLongitude());
        dto.setAccuracyM(p.getAccuracyM());
        dto.setAddress(p.getAddress());
        dto.setAddressSource(p.getAddressSource());
        dto.setTakenByUserId(p.getTakenByUserId());
        dto.setTakenByName(p.getTakenByName());
        dto.setTakenByUsername(p.getTakenByUsername());
        dto.setFileSize(p.getFileSize());
        dto.setExpiresAt(p.getExpiresAt());
        dto.setRemainingSeconds(p.getExpiresAt() == null ? null : Duration.between(now, p.getExpiresAt()).getSeconds());
        dto.setUrl(p.getTicketId() == null ? "/api/photos/" + p.getId() + "/file"
                : "/api/tickets/" + p.getTicketId() + "/photos/" + p.getId() + "/file");
        dto.setFileUrl("/api/photos/" + p.getId() + "/file");
        dto.setThumbUrl("/api/photos/" + p.getId() + "/thumb");
        dto.setDownloadUrl("/api/photos/" + p.getId() + "/download");
        return dto;
    }
}
