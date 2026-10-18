package com.huydung.custmapapplication.photo.controller;

import com.huydung.custmapapplication.photo.dto.TicketPhotoDTO;
import com.huydung.custmapapplication.photo.service.TicketPhotoService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/photos")
public class TicketPhotoController {

    private final TicketPhotoService photoService;

    public TicketPhotoController(TicketPhotoService photoService) {
        this.photoService = photoService;
    }

    // 1. Lưu ảnh vừa chụp (multipart): file + toạ độ + độ chính xác GPS + địa chỉ in trên ảnh
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

    // 2. Danh sách ảnh của ca (thông tin lúc chụp, chưa gồm nội dung ảnh)
    @GetMapping
    public ResponseEntity<List<TicketPhotoDTO>> list(@PathVariable("ticketId") Long ticketId) {
        return ResponseEntity.ok(photoService.listPhotos(ticketId));
    }

    // 3. Tải nội dung ảnh (có kiểm tra quyền); ?download=true để tải về thay vì xem trực tiếp
    @GetMapping("/{photoId}/file")
    public ResponseEntity<Resource> file(@PathVariable("ticketId") Long ticketId,
                                         @PathVariable("photoId") Long photoId,
                                         @RequestParam(value = "download", defaultValue = "false") boolean download) {
        Resource resource = photoService.loadPhotoFile(ticketId, photoId);
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("anh-" + ticketId + "-" + photoId + ".jpg")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(resource);
    }
}
