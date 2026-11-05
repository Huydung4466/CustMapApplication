package com.huydung.custmapapplication.music;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class MusicController {
    private final MusicLibrary library;

    private final MusicAccess access;

    private final MusicEmbeds embeds;

    public MusicController(MusicLibrary library, MusicAccess access, MusicEmbeds embeds) {
        this.access = access;
        this.library = library;
        this.embeds = embeds;
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    @ResponseBody
    public ResponseEntity<java.util.Map<String, String>> musicError(org.springframework.web.server.ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(java.util.Map.of("message", error.getReason()));
    }

    @GetMapping("/music")
    public String page(Authentication authentication, Model model, jakarta.servlet.http.HttpServletResponse response) {
        var roles = access.currentRoles(authentication);
        if (roles.contains("ROLE_MUSIC")) {
            model.addAttribute("canUploadMusic", roles.contains("ROLE_ADMIN"));
            return "music";
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return "music-denied";
    }

    @GetMapping("/api/music/tracks")
    @ResponseBody
    public List<MusicLibrary.Track> tracks() throws IOException {
        return library.list();
    }

    @GetMapping("/api/music/tracks/{id}/audio")
    public ResponseEntity<Resource> audio(@PathVariable String id) throws IOException {
        MusicLibrary.Audio audio = library.open(id);
        return ResponseEntity.ok()
                // Mã bài là SHA-256 của nội dung nên file không bao giờ đổi: cho trình duyệt (riêng tư, không qua proxy) lưu lại,
                // trang kế tiếp phát tiếp từ cache thay vì tải lại từ server (giảm độ trễ khi chuyển trang)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePrivate())
                .contentType(MediaType.parseMediaType(audio.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(audio.resource());
    }

    @PostMapping(value = "/api/music/tracks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MusicLibrary.Track> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(library.store(file));
    }

    // ---- Nhạc nghe trực tuyến nhúng (Spotify / YouTube / SoundCloud). Xem: người có quyền Nhạc; thêm / xóa: như tải MP3 lên

    public record EmbedRequest(String url, String title) {}

    @GetMapping("/api/music/embeds")
    @ResponseBody
    public List<MusicEmbeds.Embed> embeds() {
        return embeds.list();
    }

    @PostMapping("/api/music/embeds")
    public ResponseEntity<MusicEmbeds.Embed> addEmbed(@RequestBody EmbedRequest request, Authentication authentication) {
        requireUpload(authentication);
        MusicEmbeds.AddResult result = embeds.add(request == null ? null : request.url(), request == null ? null : request.title());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.embed());
    }

    @DeleteMapping("/api/music/embeds/{id}")
    public ResponseEntity<Void> deleteEmbed(@PathVariable Long id, Authentication authentication) {
        requireUpload(authentication);
        embeds.delete(id);
        return ResponseEntity.noContent().build();
    }

    // SecurityConfig đã chặn; kiểm tra lại ở đây để không phụ thuộc thứ tự luật
    private void requireUpload(Authentication authentication) {
        if (!access.canUpload(authentication)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thêm nhạc.");
        }
    }
}
