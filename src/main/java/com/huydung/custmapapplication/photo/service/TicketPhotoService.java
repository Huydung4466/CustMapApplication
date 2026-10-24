package com.huydung.custmapapplication.photo.service;

import com.huydung.custmapapplication.photo.dto.TicketPhotoDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TicketPhotoService {

    // Lưu một ảnh JPEG vừa chụp cho ca (ADMIN: mọi ca; STAFF: chỉ ca được giao cho mình)
    TicketPhotoDTO upload(Long ticketId, MultipartFile file,
                          Double latitude, Double longitude, Double accuracyM,
                          String address, String addressSource);

    // Danh sách ảnh của ca, mới nhất lên đầu
    List<TicketPhotoDTO> listPhotos(Long ticketId);

    // File ảnh để trả về trình duyệt
    Resource loadPhotoFile(Long ticketId, Long photoId);

    // Xóa toàn bộ ảnh của các ca (dòng trong database + file trên ổ đĩa), dùng khi xóa ca. Trả về số ảnh đã xóa.
    // File chỉ bị xóa sau khi giao dịch thành công nên nếu bị hủy giữa chừng thì ảnh vẫn còn nguyên.
    int deleteByTicketIds(java.util.Collection<Long> ticketIds);
}
