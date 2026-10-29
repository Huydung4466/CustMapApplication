package com.huydung.custmapapplication.photo.api;

import com.huydung.custmapapplication.photo.dto.PhotoSettingsDTO;
import com.huydung.custmapapplication.photo.dto.TicketPhotoDTO;
import com.huydung.custmapapplication.photo.service.TicketPhotoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * API THAY ĐỔI ảnh hiện trường, chỉ dành cho tài khoản có quyền Camera (ROLE_CAMERA):
 * lưu ảnh chụp nhanh (không gắn ca), xóa ảnh, bật / tắt tự xóa. Quyền được chặn ở SecurityConfig và kiểm tra lại trong service.
 */
@RestController
@RequestMapping("/api/photos")
public class PhotoGalleryApiController {

    private final TicketPhotoService photoService;

    public PhotoGalleryApiController(TicketPhotoService photoService) {
        this.photoService = photoService;
    }

    // Lưu ảnh chụp nhanh (multipart): file + toạ độ + độ chính xác GPS + địa chỉ in trên ảnh
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketPhotoDTO> uploadFree(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "accuracy", required = false) Double accuracy,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "addressSource", required = false) String addressSource) {
        return ResponseEntity.ok(photoService.uploadFree(file, latitude, longitude, accuracy, address, addressSource));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable("id") Long id) {
        photoService.deletePhoto(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa ảnh"));
    }

    // Bật / tắt tự xóa ảnh quá hạn (cài đặt chung cho cả hệ thống)
    @PutMapping("/settings")
    public ResponseEntity<PhotoSettingsDTO> updateSettings(@RequestBody PhotoSettingsDTO request) {
        if (request == null || request.getAutoDeleteEnabled() == null) {
            throw new IllegalArgumentException("Thiếu giá trị bật / tắt tự xóa");
        }
        return ResponseEntity.ok(photoService.setAutoDeleteEnabled(request.getAutoDeleteEnabled()));
    }
}
