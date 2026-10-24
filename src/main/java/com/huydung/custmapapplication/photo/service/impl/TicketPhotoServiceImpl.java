package com.huydung.custmapapplication.photo.service.impl;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;
import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.common.exception.ResourceNotFoundException;
import com.huydung.custmapapplication.photo.dto.TicketPhotoDTO;
import com.huydung.custmapapplication.photo.entity.TicketPhotoEntity;
import com.huydung.custmapapplication.photo.repository.TicketPhotoRepository;
import com.huydung.custmapapplication.photo.service.TicketPhotoService;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketPhotoServiceImpl implements TicketPhotoService {

    private static final Logger log = LoggerFactory.getLogger(TicketPhotoServiceImpl.class);

    private static final long MAX_FILE_BYTES = 8L * 1024 * 1024;   // giao diện gửi ~0,3-0,6 MB; 8 MB là trần an toàn
    private static final int MAX_DIMENSION = 6000;                 // px, chặn ảnh có kích thước bất thường
    private static final int MAX_ADDRESS_LENGTH = 500;
    private static final Set<String> ADDRESS_SOURCES = Set.of("GEOCODE", "TICKET");
    private static final DateTimeFormatter DIR_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final TicketPhotoRepository photoRepository;
    private final TicketRepository ticketRepository;
    private final AuthService authService;
    private final Path baseDir;
    private final int maxPerTicket;

    public TicketPhotoServiceImpl(TicketPhotoRepository photoRepository,
                                  TicketRepository ticketRepository,
                                  AuthService authService,
                                  @Value("${app.photo.dir:./data/photos}") String photoDir,
                                  @Value("${app.photo.max-per-ticket:10}") int maxPerTicket) {
        this.photoRepository = photoRepository;
        this.ticketRepository = ticketRepository;
        this.authService = authService;
        this.baseDir = Paths.get(photoDir).toAbsolutePath().normalize();
        this.maxPerTicket = Math.max(1, maxPerTicket);
        log.info("Thư mục lưu ảnh hiện trường: {}", this.baseDir);
    }

    // ===================== LƯU ẢNH =====================

    @Override
    @Transactional
    public TicketPhotoDTO upload(Long ticketId, MultipartFile file,
                                 Double latitude, Double longitude, Double accuracyM,
                                 String address, String addressSource) {
        CustomerTicketEntity ticket = findTicketAndCheckAccess(ticketId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Chưa có ảnh để lưu");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Ảnh quá lớn (tối đa 8 MB)");
        }
        validateGps(latitude, longitude, accuracyM);
        if (photoRepository.countByTicketId(ticketId) >= maxPerTicket) {
            throw new IllegalArgumentException("Mỗi ca chỉ lưu tối đa " + maxPerTicket + " ảnh");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Không đọc được dữ liệu ảnh tải lên", e);
            throw new IllegalStateException("Không đọc được ảnh tải lên, vui lòng thử lại");
        }
        validateJpeg(bytes);

        // Tên file do server tự sinh, không dùng tên file từ client
        LocalDateTime now = LocalDateTime.now();
        String relativePath = now.format(DIR_FMT) + "/" + UUID.randomUUID() + ".jpg";
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
        CurrentUserResponse user = authService.getCurrentLoggedUser();

        TicketPhotoEntity photo = new TicketPhotoEntity();
        photo.setTicketId(ticket.getId());
        photo.setFilePath(relativePath);
        photo.setFileSize((long) bytes.length);
        photo.setTakenAt(now);
        photo.setLatitude(latitude);
        photo.setLongitude(longitude);
        photo.setAccuracyM(latitude == null ? null : accuracyM);
        photo.setAddress(cleanAddress);
        photo.setAddressSource(cleanAddress == null ? null : normalizeSource(addressSource));
        photo.setTakenByUserId(user.getId());
        photo.setTakenByName(user.getFullName());

        return toDTO(photoRepository.save(photo));
    }

    // ===================== XEM ẢNH =====================

    @Override
    @Transactional(readOnly = true)
    public List<TicketPhotoDTO> listPhotos(Long ticketId) {
        findTicketAndCheckAccess(ticketId);
        return photoRepository.findByTicketIdOrderByIdDesc(ticketId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadPhotoFile(Long ticketId, Long photoId) {
        findTicketAndCheckAccess(ticketId);
        TicketPhotoEntity photo = photoRepository.findByIdAndTicketId(photoId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));

        Path file = resolveSafe(photo.getFilePath());
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("File ảnh không còn trên máy chủ");
        }
        return new FileSystemResource(file);
    }

    // ===================== HỖ TRỢ =====================

    // ADMIN được truy cập mọi ca; STAFF chỉ ca có "Giao nhân viên" trùng họ tên của mình
    private CustomerTicketEntity findTicketAndCheckAccess(Long ticketId) {
        CustomerTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca phản ánh ID: " + ticketId));

        CurrentUserResponse user = authService.getCurrentLoggedUser();
        if (!user.isAdmin()) {
            String owner = ticket.getAssignedStaff() == null ? "" : ticket.getAssignedStaff().trim();
            String me = user.getFullName() == null ? "" : user.getFullName().trim();
            if (!owner.equalsIgnoreCase(me)) {
                throw new AccessDeniedException("Bạn không có quyền với ca phản ánh này");
            }
        }
        return ticket;
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

    // Chỉ xóa file khi giao dịch đã commit thành công (nếu bị hủy thì ảnh vẫn còn đủ cả file lẫn dòng database)
    private void deleteFilesAfterCommit(List<Path> files) {
        Runnable deleteAll = () -> {
            for (Path f : files) {
                try {
                    Files.deleteIfExists(f);
                } catch (IOException e) {
                    log.warn("Không xóa được file ảnh {}", f, e);
                }
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

    private TicketPhotoDTO toDTO(TicketPhotoEntity p) {
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
        dto.setTakenByName(p.getTakenByName());
        dto.setFileSize(p.getFileSize());
        dto.setUrl("/api/tickets/" + p.getTicketId() + "/photos/" + p.getId() + "/file");
        return dto;
    }
}
