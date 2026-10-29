package com.huydung.custmapapplication.photo.controller;

import com.huydung.custmapapplication.photo.dto.PhotoDownload;
import com.huydung.custmapapplication.photo.dto.PhotoOwnerDTO;
import com.huydung.custmapapplication.photo.dto.PhotoPageDTO;
import com.huydung.custmapapplication.photo.dto.PhotoSettingsDTO;
import com.huydung.custmapapplication.photo.service.TicketPhotoService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * API HIỂN THỊ cho trang Ảnh: mọi tài khoản đăng nhập xem được danh sách, ảnh và ảnh thu nhỏ.
 * Chỉ riêng /download cần quyền Camera (chặn ở SecurityConfig và kiểm tra lại trong service).
 * API lưu / xóa / đổi cấu hình nằm ở package photo.api.
 */
@RestController
@RequestMapping("/api/photos")
public class PhotoGalleryController {

    private final TicketPhotoService photoService;

    public PhotoGalleryController(TicketPhotoService photoService) {
        this.photoService = photoService;
    }

    // Tất cả ảnh, mới nhất lên đầu; owner = id người chụp (bỏ trống = tất cả)
    @GetMapping
    public ResponseEntity<PhotoPageDTO> list(@RequestParam(value = "owner", required = false) Long owner,
                                             @RequestParam(value = "page", defaultValue = "0") int page) {
        return ResponseEntity.ok(photoService.listGallery(owner, page));
    }

    // Những người đã chụp ảnh kèm số ảnh
    @GetMapping("/owners")
    public ResponseEntity<List<PhotoOwnerDTO>> owners() {
        return ResponseEntity.ok(photoService.listOwners());
    }

    // Cấu hình tự xóa (đang bật hay tắt, số giờ giữ ảnh)
    @GetMapping("/settings")
    public ResponseEntity<PhotoSettingsDTO> settings() {
        return ResponseEntity.ok(photoService.getSettings());
    }

    // Xem ảnh đầy đủ (hiện trực tiếp trong trình duyệt)
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable("id") Long id) {
        Resource resource = photoService.loadGalleryFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("anh-" + id + ".jpg").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(resource);
    }

    // Ảnh thu nhỏ cho lưới ảnh (nhẹ hơn nhiều so với ảnh đầy đủ)
    @GetMapping("/{id}/thumb")
    public ResponseEntity<byte[]> thumb(@PathVariable("id") Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(photoService.loadThumbnail(id));
    }

    // Tải về: chỉ tài khoản có quyền Camera
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) {
        PhotoDownload d = photoService.loadForDownload(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.fileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(d.resource());
    }
}
