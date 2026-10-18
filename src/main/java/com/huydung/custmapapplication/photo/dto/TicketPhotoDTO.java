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
    private Long ticketId;
    private LocalDateTime takenAt;
    private Double latitude;
    private Double longitude;
    private Double accuracyM;
    private String address;
    private String addressSource; // GEOCODE | TICKET
    private String takenByName;
    private Long fileSize;

    // Đường dẫn tải ảnh (chưa gồm context path), VD: /api/tickets/12/photos/5/file
    private String url;
}
