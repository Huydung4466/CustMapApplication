package com.huydung.custmapapplication.music;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;
import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Nhạc nghe trực tuyến nhúng vào thư viện chung: dán link chia sẻ của Spotify / YouTube / SoundCloud,
 * trang Nhạc hiện khung phát CHÍNH THỨC của dịch vụ đó (nhạc vẫn phát từ dịch vụ, không tải file về server).
 * Link được phân tích thành (dịch vụ, loại, mã) với kiểm tra chặt; đường link iframe luôn do server dựng lại.
 */
@Service
public class MusicEmbeds {

    public enum Provider { SPOTIFY, YOUTUBE, SOUNDCLOUD }

    /** Kết quả phân tích một link. */
    public record Parsed(Provider provider, String kind, String ref) {
    }

    /** Dữ liệu trả cho giao diện. height = 0: khung video, giao diện tự giữ tỉ lệ 16:9. */
    public record Embed(Long id, String provider, String providerLabel, String kind, String kindLabel,
                        String title, String embedUrl, String sourceUrl, int height, String createdByName) {
    }

    /** Kết quả thêm: created = false nếu link này đã có trong thư viện. */
    public record AddResult(boolean created, Embed embed) {
    }

    private static final int MAX_TITLE = 150;
    private static final int MAX_URL = 2000;

    private static final Pattern SPOTIFY_ID = Pattern.compile("[A-Za-z0-9]{22}");
    private static final Set<String> SPOTIFY_KINDS = Set.of("track", "album", "playlist", "episode", "show", "artist");
    private static final Pattern YOUTUBE_VIDEO = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern YOUTUBE_LIST = Pattern.compile("[A-Za-z0-9_-]{10,64}");
    private static final Pattern SOUNDCLOUD_PART = Pattern.compile("[A-Za-z0-9_-]{1,100}");
    private static final Set<String> SOUNDCLOUD_RESERVED = Set.of("discover", "search", "you", "stream", "upload", "charts",
            "pages", "settings", "messages", "notifications", "people", "terms-of-use", "jobs", "imprint", "mobile");

    private static final Map<String, String> KIND_LABELS = Map.of(
            "track", "Bài hát", "album", "Album", "playlist", "Playlist", "episode", "Tập podcast",
            "show", "Podcast", "artist", "Nghệ sĩ", "video", "Video");

    private final MusicEmbedRepository repository;
    private final AuthService authService;

    public MusicEmbeds(MusicEmbedRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<Embed> list() {
        return repository.findAllByOrderByIdDesc().stream().map(MusicEmbeds::toEmbed).toList();
    }

    @Transactional
    public AddResult add(String url, String title) {
        Parsed p = parse(url);
        Optional<MusicEmbedEntity> existing = repository.findByProviderAndRef(p.provider().name(), p.ref());
        if (existing.isPresent()) {
            return new AddResult(false, toEmbed(existing.get()));
        }
        CurrentUserResponse me = authService.getCurrentLoggedUser();
        MusicEmbedEntity e = new MusicEmbedEntity();
        e.setProvider(p.provider().name());
        e.setKind(p.kind());
        e.setRef(p.ref());
        e.setTitle(cleanTitle(title, p));
        e.setCreatedById(me.getId());
        e.setCreatedByName(me.getFullName());
        e.setCreatedAt(LocalDateTime.now());
        return new AddResult(true, toEmbed(repository.save(e)));
    }

    @Transactional
    public void delete(Long id) {
        MusicEmbedEntity e = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục nhạc nhúng"));
        repository.delete(e);
    }

    // ------------------------------------------------------------------ phân tích link

    /** Phân tích link chia sẻ; link không hỗ trợ thì ném IllegalArgumentException kèm hướng dẫn. */
    public static Parsed parse(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Chưa có link");
        if (s.length() > MAX_URL) throw new IllegalArgumentException("Link quá dài");

        // URI kiểu "spotify:playlist:<mã>" (Spotify trên máy tính: Chia sẻ → Sao chép URI)
        if (s.regionMatches(true, 0, "spotify:", 0, 8)) {
            String[] parts = s.split(":");
            if (parts.length == 3) return spotify(parts[1], parts[2]);
            throw unsupported();
        }

        URI uri;
        try {
            uri = new URI(s);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Link không hợp lệ");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) throw new IllegalArgumentException("Link phải bắt đầu bằng https://");
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) host = host.substring(4);
        if (host.startsWith("m.")) host = host.substring(2);
        List<String> seg = segments(uri.getRawPath());
        String query = uri.getRawQuery();

        switch (host) {
            case "open.spotify.com", "play.spotify.com" -> {
                // Bỏ phần ngôn ngữ (/intl-vi/...) và /embed/ nếu có
                while (!seg.isEmpty() && (seg.get(0).startsWith("intl-") || seg.get(0).equals("embed"))) seg.remove(0);
                if (seg.size() >= 2) return spotify(seg.get(0), seg.get(1));
                throw unsupported();
            }
            case "spotify.link", "spotify.app.link" ->
                    throw new IllegalArgumentException("Link rút gọn của Spotify chưa dùng được. Hãy mở link trong trình duyệt rồi sao chép link open.spotify.com");
            case "youtube.com", "music.youtube.com", "youtube-nocookie.com" -> {
                String v = param(query, "v");
                if (v != null) return youtubeVideo(v);
                if (!seg.isEmpty() && Set.of("shorts", "embed", "live", "v").contains(seg.get(0)) && seg.size() >= 2) {
                    if (seg.get(0).equals("embed") && seg.get(1).equals("videoseries")) return youtubeList(param(query, "list"));
                    return youtubeVideo(seg.get(1));
                }
                String list = param(query, "list");
                if (list != null) return youtubeList(list);
                throw unsupported();
            }
            case "youtu.be" -> {
                if (seg.size() == 1) return youtubeVideo(seg.get(0));
                throw unsupported();
            }
            case "soundcloud.com" -> {
                return soundcloud(seg);
            }
            case "on.soundcloud.com" ->
                    throw new IllegalArgumentException("Link rút gọn của SoundCloud chưa dùng được. Hãy mở link rồi sao chép link soundcloud.com/...");
            default -> throw unsupported();
        }
    }

    private static Parsed spotify(String kind, String id) {
        String k = kind.toLowerCase(Locale.ROOT);
        if (!SPOTIFY_KINDS.contains(k) || !SPOTIFY_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Link Spotify không hợp lệ (cần link bài hát, album, playlist hoặc podcast)");
        }
        return new Parsed(Provider.SPOTIFY, k, id);
    }

    private static Parsed youtubeVideo(String id) {
        if (id == null || !YOUTUBE_VIDEO.matcher(id).matches()) throw new IllegalArgumentException("Link YouTube không hợp lệ");
        return new Parsed(Provider.YOUTUBE, "video", id);
    }

    private static Parsed youtubeList(String list) {
        if (list == null || !YOUTUBE_LIST.matcher(list).matches()) throw new IllegalArgumentException("Link playlist YouTube không hợp lệ");
        return new Parsed(Provider.YOUTUBE, "playlist", list);
    }

    private static Parsed soundcloud(List<String> seg) {
        boolean set = seg.size() == 3 && seg.get(1).equals("sets");
        boolean track = seg.size() == 2 && !seg.get(1).equals("sets");
        if ((!set && !track) || SOUNDCLOUD_RESERVED.contains(seg.get(0).toLowerCase(Locale.ROOT))
                || !seg.stream().allMatch(p -> SOUNDCLOUD_PART.matcher(p).matches())) {
            throw new IllegalArgumentException("Link SoundCloud không hợp lệ (cần link một bài hoặc một playlist)");
        }
        return new Parsed(Provider.SOUNDCLOUD, set ? "playlist" : "track", String.join("/", seg).toLowerCase(Locale.ROOT));
    }

    private static IllegalArgumentException unsupported() {
        return new IllegalArgumentException("Chưa hỗ trợ link này. Dán link bài hát / playlist của Spotify, YouTube, SoundCloud hoặc link file .mp3");
    }

    private static List<String> segments(String rawPath) {
        List<String> out = new ArrayList<>();
        if (rawPath == null) return out;
        Arrays.stream(rawPath.split("/")).filter(p -> !p.isEmpty()).forEach(out::add);
        return out;
    }

    private static String param(String rawQuery, String name) {
        if (rawQuery == null) return null;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) return pair.substring(eq + 1);
        }
        return null;
    }

    // ------------------------------------------------------------------ dựng khung phát

    static String embedUrl(Provider p, String kind, String ref) {
        return switch (p) {
            case SPOTIFY -> "https://open.spotify.com/embed/" + kind + "/" + ref;
            case YOUTUBE -> kind.equals("playlist")
                    ? "https://www.youtube-nocookie.com/embed/videoseries?list=" + ref
                    : "https://www.youtube-nocookie.com/embed/" + ref;
            case SOUNDCLOUD -> "https://w.soundcloud.com/player/?url=https%3A//soundcloud.com/" + ref
                    + "&visual=false&show_comments=false";
        };
    }

    static String sourceUrl(Provider p, String kind, String ref) {
        return switch (p) {
            case SPOTIFY -> "https://open.spotify.com/" + kind + "/" + ref;
            case YOUTUBE -> kind.equals("playlist")
                    ? "https://www.youtube.com/playlist?list=" + ref
                    : "https://www.youtube.com/watch?v=" + ref;
            case SOUNDCLOUD -> "https://soundcloud.com/" + ref;
        };
    }

    static int height(Provider p, String kind) {
        return switch (p) {
            case SPOTIFY -> kind.equals("track") || kind.equals("episode") ? 152 : 352;
            case YOUTUBE -> 0;
            case SOUNDCLOUD -> kind.equals("playlist") ? 450 : 166;
        };
    }

    static String providerLabel(Provider p) {
        return switch (p) {
            case SPOTIFY -> "Spotify";
            case YOUTUBE -> "YouTube";
            case SOUNDCLOUD -> "SoundCloud";
        };
    }

    private static String cleanTitle(String title, Parsed p) {
        String t = title == null ? "" : title.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        if (t.isEmpty()) t = providerLabel(p.provider()) + " · " + KIND_LABELS.getOrDefault(p.kind(), p.kind());
        return t.length() > MAX_TITLE ? t.substring(0, MAX_TITLE) : t;
    }

    private static Embed toEmbed(MusicEmbedEntity e) {
        Provider p = Provider.valueOf(e.getProvider());
        return new Embed(e.getId(), p.name(), providerLabel(p), e.getKind(), KIND_LABELS.getOrDefault(e.getKind(), e.getKind()),
                e.getTitle(), embedUrl(p, e.getKind(), e.getRef()), sourceUrl(p, e.getKind(), e.getRef()),
                height(p, e.getKind()), e.getCreatedByName());
    }
}
