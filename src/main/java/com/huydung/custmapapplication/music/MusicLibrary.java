package com.huydung.custmapapplication.music;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Thư viện nhạc dùng chung, lưu thẳng trên ổ đĩa: <app.music.dir>/<sha256>_<tên bài>.<đuôi>.
 * Nhận các định dạng trình duyệt phát được: MP3, M4A / MP4 (AAC, ALAC), AAC, OGG / OPUS, WAV, FLAC, WEBM.
 * Định dạng được nhận diện theo NỘI DUNG file (không tin đuôi file hay Content-Type của client),
 * file được lưu với đuôi chuẩn của định dạng thật và phát ra với Content-Type tương ứng.
 */
@Service
public class MusicLibrary {
    // multipart tối đa 50 MB (application.properties) nên để file nhạc 45 MB cho dư phần đầu yêu cầu
    static final long MAX_BYTES = 45L * 1024 * 1024;
    private static final Pattern ID = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern TRACK = Pattern.compile("[0-9a-f]{64}_.+\\.(mp3|m4a|aac|ogg|wav|flac|webm)");
    /** Đuôi file được chọn để tải lên (định dạng thật vẫn kiểm tra theo nội dung). */
    static final Set<String> UPLOAD_EXTENSIONS = Set.of("mp3", "m4a", "mp4", "aac", "ogg", "oga", "opus", "wav", "flac", "webm", "weba");
    // Nhãn "ftyp" của file MP4 chứa âm thanh; loại ảnh HEIC/AVIF (cũng là ftyp) và M4P (có DRM, không phát được)
    private static final Set<String> MP4_BRANDS = Set.of("M4A ", "M4B ", "mp41", "mp42", "isom", "iso2", "iso4", "iso5", "iso6",
            "dash", "mp4 ", "avc1", "MSNV", "3gp4", "3gp5", "3gp6", "f4a ", "f4v ");

    /** Định dạng âm thanh: đuôi khi lưu + Content-Type khi phát. */
    public enum AudioFormat {
        MP3("mp3", "audio/mpeg"),
        M4A("m4a", "audio/mp4"),
        AAC("aac", "audio/aac"),
        OGG("ogg", "audio/ogg"),
        WAV("wav", "audio/wav"),
        FLAC("flac", "audio/flac"),
        WEBM("webm", "audio/webm");

        private final String extension;
        private final String contentType;

        AudioFormat(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public String extension() { return extension; }

        public String contentType() { return contentType; }

        static AudioFormat ofExtension(String ext) {
            return Arrays.stream(values()).filter(f -> f.extension.equals(ext)).findFirst().orElse(MP3);
        }
    }

    private final Path root;

    public record Track(String id, String title, String format) {}

    /** File nhạc để phát kèm Content-Type đúng định dạng. */
    public record Audio(Resource resource, String contentType) {}

    public MusicLibrary(@Value("${app.music.dir:./data/music}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public List<Track> list() throws IOException {
        if (!Files.exists(root)) return List.of();
        try (var entries = Files.list(root)) {
            return entries.filter(MusicLibrary::isTrack)
                    .map(MusicLibrary::describe)
                    .sorted(Comparator.comparing(Track::title, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(Track::id))
                    .toList();
        }
    }

    public Audio open(String id) throws IOException {
        if (!ID.matcher(id).matches()) throw missing();
        Path path = find(id);
        if (path == null) throw missing();
        return new Audio(new FileSystemResource(path), formatOf(path).contentType());
    }

    public Track store(MultipartFile file) throws IOException {
        String title = titleOf(file.getOriginalFilename());
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File nhạc phải có dung lượng từ 1 byte đến 45 MB.");
        }
        Files.createDirectories(root);
        Path temporary = Files.createTempFile(root, ".upload-", ".tmp");
        try {
            MessageDigest digest = sha256();
            try (var stream = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(stream, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            AudioFormat format = detect(temporary);
            if (format == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không nhận ra file nhạc (cần MP3, M4A, AAC, OGG, OPUS, WAV, FLAC hoặc WEBM).");
            }
            return publish(temporary, HexFormat.of().formatHex(digest.digest()), title, format);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private synchronized Track publish(Path temporary, String id, String title, AudioFormat format) throws IOException {
        Path existing = find(id);
        if (existing != null) return describe(existing);
        Path path = root.resolve(id + "_" + title + "." + format.extension());
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
        return describe(path);
    }

    private Path find(String id) throws IOException {
        if (!Files.exists(root)) return null;
        try (var entries = Files.newDirectoryStream(root, id + "_*")) {
            for (Path path : entries) {
                if (isTrack(path)) return path;
            }
        }
        return null;
    }

    private static boolean isTrack(Path path) {
        return TRACK.matcher(path.getFileName().toString()).matches()
                && Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS);
    }

    private static AudioFormat formatOf(Path path) {
        String name = path.getFileName().toString();
        return AudioFormat.ofExtension(name.substring(name.lastIndexOf('.') + 1));
    }

    private static Track describe(Path path) {
        String name = path.getFileName().toString();
        return new Track(name.substring(0, 64), name.substring(65, name.lastIndexOf('.')), formatOf(path).name());
    }

    private static String titleOf(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0 || !UPLOAD_EXTENSIONS.contains(lower.substring(dot + 1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ nhận file nhạc: .mp3, .m4a, .mp4, .aac, .ogg, .opus, .wav, .flac, .webm.");
        }
        String basename = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        int ext = basename.lastIndexOf('.');
        String title = basename.substring(0, Math.max(0, ext))
                .replaceAll("[^\\p{L}\\p{M}\\p{N} ._()\\-]", "_").strip();
        if (title.isBlank()) return "Bài hát";
        int end = title.offsetByCodePoints(0, Math.min(45, title.codePointCount(0, title.length())));
        return title.substring(0, end);
    }

    // ------------------------------------------------------------------ nhận diện định dạng theo nội dung

    /** Định dạng thật của file, hoặc null nếu không phải file nhạc được hỗ trợ. */
    static AudioFormat detect(Path file) throws IOException {
        try (var input = new RandomAccessFile(file.toFile(), "r")) {
            long length = input.length();
            if (length < 12) return null;
            byte[] head = new byte[12];
            input.readFully(head);
            if (ascii(head, 0, 4).equals("OggS")) return AudioFormat.OGG;
            if (ascii(head, 0, 4).equals("RIFF") && ascii(head, 8, 4).equals("WAVE")) return AudioFormat.WAV;
            if ((head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45 && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3) {
                return AudioFormat.WEBM;
            }
            if (ascii(head, 4, 4).equals("ftyp")) return MP4_BRANDS.contains(ascii(head, 8, 4)) ? AudioFormat.M4A : null;

            // Thẻ ID3 có thể đứng trước MP3, AAC hoặc FLAC: bỏ qua phần thẻ
            long start = 0;
            if (ascii(head, 0, 3).equals("ID3")) {
                long size = 0;
                for (int i = 6; i < 10; i++) {
                    int b = head[i] & 0xFF;
                    if (b > 127) return null;
                    size = (size << 7) | b;
                }
                start = size + 10;
                if (start + 4 > length) return null;
            }
            input.seek(start);
            byte[] at = new byte[4];
            input.readFully(at);
            if (ascii(at, 0, 4).equals("fLaC")) return AudioFormat.FLAC;
            // AAC thô (ADTS): 12 bit đồng bộ, lớp = 00
            if ((at[0] & 0xFF) == 0xFF && (at[1] & 0xF6) == 0xF0) return AudioFormat.AAC;

            long end = Math.min(length - 4, start + 4096);
            for (long offset = start; offset <= end; offset++) {
                input.seek(offset);
                int header = input.readInt();
                int bitrate = (header >>> 12) & 15;
                if ((header & 0xffe00000) == 0xffe00000 && ((header >>> 19) & 3) != 1
                        && ((header >>> 17) & 3) == 1 && bitrate > 0 && bitrate < 15
                        && ((header >>> 10) & 3) != 3) return AudioFormat.MP3;
            }
            return null;
        }
    }

    private static String ascii(byte[] b, int from, int len) {
        return new String(b, from, len, StandardCharsets.ISO_8859_1);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài hát.");
    }
}
