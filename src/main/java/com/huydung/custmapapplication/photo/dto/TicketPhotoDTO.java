package com.huydung.custmapapplication.photo.dto;

import com.huydung.custmapapplication.common.dto.BaseDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TicketPhotoDTO extends BaseDTO {
    private Long ticketId;           // null = ảnh chụp nhanh không gắn ca
    private LocalDateTime takenAt;
    private Double latitude;
    private Double longitude;
    private Double accuracyM;
    private String address;
    private String addressSource; // GEOCODE | TICKET
    private Long takenByUserId;
    private String takenByName;
    private String takenByUsername;
    private Long fileSize;

    // Hạn tự xóa; null = ảnh không có hạn (ảnh cũ). remainingSeconds là số giây còn lại TẠI LÚC gửi phản hồi (âm = đã quá hạn).
    private LocalDateTime expiresAt;
    private Long remainingSeconds;

    // Đường dẫn (chưa gồm context path). url giữ như cũ cho ảnh gắn ca; fileUrl / thumbUrl / downloadUrl dùng cho trang Ảnh.
    private String url;
    private String fileUrl;       // xem ảnh đầy đủ (mọi tài khoản)
    private String thumbUrl;      // ảnh thu nhỏ cho lưới (mọi tài khoản)
    private String downloadUrl;   // tải về (chỉ tài khoản có quyền Camera)
}
