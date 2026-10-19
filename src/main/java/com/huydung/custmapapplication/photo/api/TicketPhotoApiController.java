package com.huydung.custmapapplication.photo.api;

import com.huydung.custmapapplication.photo.dto.TicketPhotoDTO;
import com.huydung.custmapapplication.photo.service.TicketPhotoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** API THAY ĐỔI dữ liệu của ảnh hiện trường: lưu ảnh vừa chụp vào ca. */
@RestController
@RequestMapping("/api/tickets/{ticketId}/photos")
public class TicketPhotoApiController {

    private final TicketPhotoService photoService;

    public TicketPhotoApiController(TicketPhotoService photoService) {
        this.photoService = photoService;
    }

    // Lưu ảnh vừa chụp (multipart): file + toạ độ + độ chính xác GPS + địa chỉ in trên ảnh
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketPhotoDTO> upload(
            @PathVariable("ticketId") Long ticketId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "accuracy", required = false) Double accuracy,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "addressSource", required = false) String addressSource) {
        return ResponseEntity.ok(photoService.upload(ticketId, file, latitude, longitude, accuracy, address, addressSource));
    }
}
