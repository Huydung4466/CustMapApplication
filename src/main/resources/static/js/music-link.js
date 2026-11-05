/*
 * music-link.js - Tải file nhạc (MP3, M4A, WAV, FLAC...) từ link được chia sẻ (Google Drive, Dropbox, link .mp3 trực tiếp...) NGAY TRÊN TRÌNH DUYỆT,
 * rồi trả về File để music.js đưa lên /api/music/tracks giống như chọn file từ máy.
 * Lưu ý: trình duyệt chỉ tải được khi trang nguồn cho phép (CORS). Trang không cho phép thì báo lỗi để người dùng tải file về máy.
 */
(function () {
    var MAX_BYTES = 45 * 1024 * 1024;   // trùng giới hạn của server (MusicLibrary.MAX_BYTES)
    var FALLBACK_TITLE = 'Bài hát từ link';

    // Lấy mọi link http(s) (và URI spotify:loai:ma) trong đoạn chữ dán vào (ứng dụng khác hay chia sẻ dạng "Tên bài https://...")
    function extractUrls(text) {
        var found = String(text || '').match(/https?:\/\/[^\s<>"'`]+|spotify:[a-z]+:[A-Za-z0-9]{22}/gi) || [];
        var seen = {};
        return found.map(function (u) { return u.replace(/[).,;!?\]]+$/, ''); })
            .filter(function (u) { if (seen[u]) return false; seen[u] = true; return true; });
    }

    // Đổi link "trang xem" của các dịch vụ chia sẻ phổ biến thành link tải thẳng file
    function toDirectUrl(raw) {
        var url;
        try { url = new URL(raw); } catch (e) { return raw; }
        var host = url.hostname.toLowerCase();
        if (host === 'drive.google.com' || host === 'docs.google.com') {
            var m = url.pathname.match(/\/file\/d\/([A-Za-z0-9_-]+)/);
            var id = m ? m[1] : url.searchParams.get('id');
            if (id) return 'https://drive.usercontent.google.com/download?id=' + encodeURIComponent(id) + '&export=download&confirm=t';
        }
        if (host === 'www.dropbox.com' || host === 'dropbox.com') {
            url.hostname = 'dl.dropboxusercontent.com';
            url.searchParams.delete('dl');
            return url.toString();
        }
        return url.toString();
    }

    // Đuôi file nhạc được nhận (server vẫn nhận diện định dạng thật theo nội dung)
    var AUDIO_EXT = /\.(mp3|m4a|mp4|aac|ogg|oga|opus|wav|flac|webm|weba)$/i;

    // Đoán định dạng từ 12 byte đầu; trả về đuôi file hoặc null nếu không giống file nhạc. Server kiểm tra kỹ lại.
    function detectAudio(b) {
        function s(from, len) { return String.fromCharCode.apply(null, Array.prototype.slice.call(b, from, from + len)); }
        if (b.length < 4) return null;
        if (s(0, 3) === 'ID3') return 'mp3';             // thẻ ID3 (thường đứng trước MP3)
        if (s(0, 4) === 'fLaC') return 'flac';
        if (s(0, 4) === 'OggS') return 'ogg';
        if (b.length >= 12 && s(0, 4) === 'RIFF' && s(8, 4) === 'WAVE') return 'wav';
        if (b[0] === 0x1A && b[1] === 0x45 && b[2] === 0xDF && b[3] === 0xA3) return 'webm';
        if (b.length >= 8 && s(4, 4) === 'ftyp') return 'm4a';
        if (b[0] === 0xFF && (b[1] & 0xF6) === 0xF0) return 'aac';
        if (b[0] === 0xFF && (b[1] & 0xE0) === 0xE0) return 'mp3';
        return null;
    }

    function cleanTitle(name) {
        var t = String(name || '').replace(AUDIO_EXT, '').replace(/[\/:*?"<>|\u0000-\u001f]+/g, ' ').replace(/\s+/g, ' ').trim();
        return t || FALLBACK_TITLE;
    }

    // Tên bài: ưu tiên Content-Disposition (nếu trang nguồn cho đọc), sau đó tên file nhạc trong đường dẫn. ext = đuôi đã nhận diện
    function fileNameOf(contentDisposition, urls, ext) {
        var suffix = '.' + (ext || 'mp3');
        var cd = contentDisposition || '';
        var star = cd.match(/filename\*\s*=\s*(?:UTF-8|utf-8)''([^;]+)/);
        if (star) { try { return cleanTitle(decodeURIComponent(star[1].trim())) + suffix; } catch (e) { /* bỏ qua */ } }
        var plain = cd.match(/filename\s*=\s*"?([^";]+)"?/i);
        if (plain && AUDIO_EXT.test(plain[1].trim())) return cleanTitle(plain[1].trim()) + suffix;
        for (var i = 0; i < urls.length; i++) {
            try {
                var seg = new URL(urls[i]).pathname.split('/').filter(Boolean).pop() || '';
                seg = decodeURIComponent(seg);
                if (AUDIO_EXT.test(seg)) return cleanTitle(seg) + suffix;
            } catch (e) { /* bỏ qua */ }
        }
        return FALLBACK_TITLE + suffix;
    }

    // Đọc nội dung, dừng ngay khi vượt giới hạn để không tải hết file quá lớn
    async function readCapped(response) {
        if (!response.body || !response.body.getReader) {
            var whole = await response.blob();
            if (whole.size > MAX_BYTES) throw new Error('File lớn hơn 45 MB.');
            return whole;
        }
        var reader = response.body.getReader();
        var chunks = [];
        var total = 0;
        for (;;) {
            var part = await reader.read();
            if (part.done) break;
            total += part.value.length;
            if (total > MAX_BYTES) {
                try { await reader.cancel(); } catch (e) { /* bỏ qua */ }
                throw new Error('File lớn hơn 45 MB.');
            }
            chunks.push(part.value);
        }
        return new Blob(chunks);
    }

    // Tải một link về thành File nhạc; lỗi thì ném Error với thông báo dễ hiểu
    async function download(link) {
        var direct = toDirectUrl(link);
        if (window.location.protocol === 'https:' && /^http:/i.test(direct)) {
            throw new Error('Link dùng http, trình duyệt chặn tải trên trang https. Hãy dùng link https hoặc tải file về máy.');
        }
        var response;
        try {
            response = await fetch(direct, { credentials: 'omit', redirect: 'follow', referrerPolicy: 'no-referrer', cache: 'no-store' });
        } catch (e) {
            throw new Error('Trang chia sẻ không cho trình duyệt tải trực tiếp. Hãy tải file về máy rồi chọn file.');
        }
        if (!response.ok) {
            throw new Error(response.status === 404 ? 'Link không tồn tại hoặc đã hết hạn.'
                : (response.status === 401 || response.status === 403) ? 'Link cần đăng nhập hoặc chưa mở quyền chia sẻ.'
                : 'Trang nguồn trả lỗi (mã ' + response.status + ').');
        }
        var declared = Number(response.headers.get('Content-Length'));
        if (declared > MAX_BYTES) throw new Error('File lớn hơn 45 MB.');
        var blob = await readCapped(response);
        if (!blob.size) throw new Error('Link không có dữ liệu.');
        var head = new Uint8Array(await blob.slice(0, 12).arrayBuffer());
        var ext = detectAudio(head);
        if (!ext) {
            throw new Error('Link không trỏ tới file nhạc (có thể là trang web hoặc chưa mở quyền chia sẻ công khai).');
        }
        var name = fileNameOf(response.headers.get('Content-Disposition'), [response.url, direct, link], ext);
        return new File([blob], name);
    }

    // ---------------------------------------------------------------- nhạc nghe trực tuyến (nhúng khung phát chính thức)

    function hostOf(link) {
        try { return new URL(link).hostname.toLowerCase().replace(/^(www|m)\./, ''); } catch (e) { return ''; }
    }

    // Link của dịch vụ nghe nhạc trực tuyến: không tải MP3 mà nhúng khung phát (server kiểm tra lại kỹ)
    function streamingProvider(link) {
        if (/^spotify:/i.test(link)) return 'SPOTIFY';
        var h = hostOf(link);
        if (/(^|\.)spotify\.(com|link)$|^spotify\.app\.link$/.test(h)) return 'SPOTIFY';
        if (/^(youtube\.com|music\.youtube\.com|youtu\.be|youtube-nocookie\.com)$/.test(h)) return 'YOUTUBE';
        if (/(^|\.)soundcloud\.com$/.test(h)) return 'SOUNDCLOUD';
        return null;
    }

    // Tên bài / playlist lấy qua oEmbed của chính dịch vụ; lỗi hoặc quá 5 giây thì bỏ qua (server tự đặt tên mặc định)
    async function fetchTitle(link) {
        var provider = streamingProvider(link);
        var page = /^spotify:/i.test(link) ? 'https://open.spotify.com/' + link.split(':').slice(1).join('/') : link;
        var api = provider === 'SPOTIFY' ? 'https://open.spotify.com/oembed?url='
            : provider === 'YOUTUBE' ? 'https://www.youtube.com/oembed?format=json&url='
            : provider === 'SOUNDCLOUD' ? 'https://soundcloud.com/oembed?format=json&url=' : null;
        if (!api) return '';
        var ctrl = typeof AbortController === 'function' ? new AbortController() : null;
        var timer = ctrl ? setTimeout(function () { ctrl.abort(); }, 5000) : null;
        try {
            var res = await fetch(api + encodeURIComponent(page), { credentials: 'omit', signal: ctrl ? ctrl.signal : undefined });
            if (!res.ok) return '';
            var data = await res.json();
            return typeof data.title === 'string' ? data.title.slice(0, 150) : '';
        } catch (e) {
            return '';
        } finally {
            if (timer) clearTimeout(timer);
        }
    }

    window.MusicLink = {
        MAX_BYTES: MAX_BYTES,
        extractUrls: extractUrls,
        toDirectUrl: toDirectUrl,
        detectAudio: detectAudio,
        fileNameOf: fileNameOf,
        download: download,
        streamingProvider: streamingProvider,
        fetchTitle: fetchTitle
    };
})();
