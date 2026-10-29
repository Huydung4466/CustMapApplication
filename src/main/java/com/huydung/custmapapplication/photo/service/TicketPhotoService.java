package com.huydung.custmapapplication.photo.service;

import com.huydung.custmapapplication.photo.dto.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

public interface TicketPhotoService {

    // ===================== LƯU / XÓA (chỉ tài khoản có quyền Camera - ROLE_CAMERA) =====================

    // Lưu một ảnh JPEG vừa chụp cho ca (ADMIN: mọi ca; STAFF: chỉ ca được giao cho mình)
    TicketPhotoDTO upload(Long ticketId, MultipartFile file,
                          Double latitude, Double longitude, Double accuracyM,
                          String address, String addressSource);

    // Lưu ảnh chụp nhanh, không gắn với ca nào
    TicketPhotoDTO uploadFree(MultipartFile file,
                              Double latitude, Double longitude, Double accuracyM,
                              String address, String addressSource);

    // Xóa một ảnh (dòng database + file trên ổ đĩa)
    void deletePhoto(Long photoId);

    // ===================== XEM (mọi tài khoản đăng nhập) =====================

    // Danh sách ảnh của ca, mới nhất lên đầu
    List<TicketPhotoDTO> listPhotos(Long ticketId);

    // File ảnh của ca để trả về trình duyệt (có kiểm tra quyền theo ca)
    Resource loadPhotoFile(Long ticketId, Long photoId);

    // Trang Ảnh: tất cả ảnh, mới nhất lên đầu, lọc theo người chụp (null = tất cả), phân trang
    PhotoPageDTO listGallery(Long ownerUserId, int page);

    // Những người đã chụp ảnh và số ảnh của họ
    List<PhotoOwnerDTO> listOwners();

    // Xem ảnh đầy đủ / ảnh thu nhỏ trong trang Ảnh
    Resource loadGalleryFile(Long photoId);

    byte[] loadThumbnail(Long photoId);

    // Tải về: chỉ tài khoản có quyền Camera
    PhotoDownload loadForDownload(Long photoId);

    // Tải ảnh của ca với download=true: chỉ tài khoản có quyền Camera (ném AccessDeniedException nếu không có)
    void assertCanDownload();

    // ===================== TỰ XÓA ẢNH QUÁ HẠN =====================

    PhotoSettingsDTO getSettings();

    // Bật / tắt tự xóa (chỉ tài khoản có quyền Camera); cài đặt chung cho cả hệ thống
    PhotoSettingsDTO setAutoDeleteEnabled(boolean enabled);

    // Xóa các ảnh đã quá hạn (mỗi lần tối đa một lô); không làm gì nếu đang tắt tự xóa. Trả về số ảnh đã xóa.
    int purgeExpired();

    // Xóa toàn bộ ảnh của các ca (dòng trong database + file trên ổ đĩa), dùng khi xóa ca. Trả về số ảnh đã xóa.
    // File chỉ bị xóa sau khi giao dịch thành công nên nếu bị hủy giữa chừng thì ảnh vẫn còn nguyên.
    int deleteByTicketIds(Collection<Long> ticketIds);
}
