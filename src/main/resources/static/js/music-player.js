/*
 * music-player.js - Bộ phát nhạc DÙNG CHUNG cho mọi trang có thanh trên cùng (nạp bởi fragments/top-navbar.jsp, thuộc tính defer).
 *
 * Vì sao cần: web này là nhiều trang JSP, chuyển trang là trình duyệt tải lại cả trang nên âm thanh bị ngắt.
 * Trước đây nhạc phải mở ở TAB RIÊNG ("văng" khỏi ứng dụng). Giờ nhạc mở ngay trong ứng dụng, cùng một tab:
 *  - Trạng thái phát (danh sách phát, bài đang nghe, vị trí, chế độ lặp / ngẫu nhiên) lưu trong localStorage.
 *  - Khi chuyển trang, trang mới tự phát tiếp đúng bài, đúng giây (nếu trình duyệt cho phép tự phát; iPhone / Safari
 *    thường chặn thì thanh nhạc hiện sẵn nút ▶ để chạm phát tiếp).
 *  - Ở mọi trang (trừ trang Nghe nhạc) có thanh nhạc nhỏ nổi phía trên thanh điều hướng dưới: phát / dừng / chuyển bài / tắt.
 *  - Mở nhiều tab cùng lúc: tab nào bấm phát sau cùng giữ tiếng, tab còn lại tự dừng (không bị hai nguồn tiếng chồng nhau).
 *
 * Trang Nghe nhạc (js/music.js) dùng window.CustMapMusic do file này tạo ra.
 * Quyền nghe nhạc vẫn do SERVER kiểm tra ở mọi lệnh gọi /api/music/**; file này không quyết định quyền.
 */
(function () {
    'use strict';
    if (window.CustMapMusic) return;

    var nav = document.getElementById('appTopNav');
    var ctx = nav ? (nav.getAttribute('data-ctx') || '') : '';
    var API = ctx + '/api/music/tracks';
    var SESSION_KEY = 'custmap.music.session';   // bài đang nghe: chỉ số, giây, đang phát hay không (nhỏ, ghi mỗi giây)
    var QUEUE_KEY = 'custmap.music.queue';       // danh sách phát (có thể dài, chỉ ghi khi đổi danh sách)
    var PREFS_KEY = 'custmap.music.prefs.';      // + id tài khoản: yêu thích, gần đây, ngẫu nhiên, lặp, âm lượng
    var HANDOFF_MS = 20000;                      // trang mới chỉ tự phát tiếp nếu trang cũ vừa đóng trong khoảng này
    var SAVE_EVERY_MS = 1000;
    var tabId = Math.random().toString(36).slice(2);
    var onMusicPage = /\/music\/?$/.test(window.location.pathname);

    var audio = new Audio();
    audio.preload = 'none';   // không tải gì cho tới khi bấm phát (khôi phục bài cũ ở mỗi trang khỏi tốn một lượt gọi server)

    var uid = null;
    var queue = [];            // [{id, title}]
    var index = -1;
    var prefs = { fav: [], recent: [], shuffle: false, repeat: 'all', volume: 0.8 };
    var active = false;        // tab này đang là "chủ" của phiên phát; chỉ chủ mới ghi vào localStorage
    var blocked = false;       // trình duyệt chặn tự phát, đang chờ người dùng chạm
    var pendingSeek = 0;
    var resumeBase = null;     // {pos, at}: vị trí đã lưu lúc trang trước đóng; cộng thêm thời gian trôi qua khi bài thật sự sẵn sàng
    var inited = false;
    var UID_KEY = 'custmap.music.uid';   // id tài khoản lần trước: để khởi động ngay, khỏi chờ /api/auth/me
    var errorStreak = 0;
    var lastSave = 0;
    var leaving = false;       // trang đang bị rời đi (pagehide đã chạy): mọi lần ghi sau đó vẫn giữ cờ "chuyển trang"
    var library = null;
    var listeners = {};
    var resolveReady, rejectReady;
    var ready = new Promise(function (res, rej) { resolveReady = res; rejectReady = rej; });
    ready.catch(function () { /* lỗi đã xử lý ở nơi gọi */ });

    /* ---------------------------------------------------------------- tiện ích */

    function readJson(key) {
        try { var v = window.localStorage.getItem(key); return v ? JSON.parse(v) : null; } catch (e) { return null; }
    }
    function writeJson(key, value) {
        try { window.localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* hết dung lượng / chế độ riêng tư */ }
    }
    function removeKey(key) {
        try { window.localStorage.removeItem(key); } catch (e) { /* bỏ qua */ }
    }
    function on(name, fn) { (listeners[name] = listeners[name] || []).push(fn); }
    function emit(name, arg) {
        (listeners[name] || []).slice().forEach(function (fn) {
            try { fn(arg); } catch (e) { console.error(e); }
        });
    }
    function fmt(sec) {
        sec = Number.isFinite(sec) && sec > 0 ? sec : 0;
        var m = Math.floor(sec / 60);
        var s = Math.floor(sec % 60);
        return m + ':' + (s < 10 ? '0' : '') + s;
    }
    function trackSrc(id) { return API + '/' + encodeURIComponent(id) + '/audio'; }
    function current() { return index >= 0 && index < queue.length ? queue[index] : null; }
    function isPlaying() { return !audio.paused && !audio.ended; }

    function hashOf(str) {
        var h = 5381;
        str = String(str || '');
        for (var i = 0; i < str.length; i++) h = ((h << 5) + h + str.charCodeAt(i)) | 0;
        return h >>> 0;
    }
    function hsl(h, s, l) {       // HSL -> #rrggbb (dùng cho SVG để chắc chắn mọi trình duyệt hiểu)
        s /= 100; l /= 100;
        var k = function (n) { return (n + h / 30) % 12; };
        var a = s * Math.min(l, 1 - l);
        var f = function (n) { return l - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1))); };
        var hex = function (x) { var v = Math.round(x * 255).toString(16); return v.length < 2 ? '0' + v : v; };
        return '#' + hex(f(0)) + hex(f(8)) + hex(f(4));
    }
    // Ảnh bìa phong cảnh (mặt trời + đồi) tự vẽ từ mã bài: mỗi bài một bìa cố định, không cần file ảnh
    var coverCache = {};
    function coverBackground(id) {
        var key = String(id || '');
        if (coverCache[key]) return coverCache[key];
        var n = hashOf(key);
        var h = n % 360;
        var h2 = (h + 35 + ((n >>> 9) % 50)) % 360;
        var sx = 90 + ((n >>> 3) % 320), sy = 90 + ((n >>> 11) % 120), sr = 40 + ((n >>> 5) % 50);
        var svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500"><defs>'
            + '<linearGradient id="g" x1="0" x2="1" y1="0" y2="1"><stop stop-color="' + hsl(h, 62, 74) + '"/><stop offset="1" stop-color="' + hsl(h2, 48, 40) + '"/></linearGradient>'
            + '<radialGradient id="s"><stop stop-color="#fff5df" stop-opacity=".95"/><stop offset="1" stop-color="#fff5df" stop-opacity="0"/></radialGradient></defs>'
            + '<rect width="500" height="500" fill="url(#g)"/>'
            + '<circle cx="' + sx + '" cy="' + sy + '" r="' + Math.round(sr * 2.2) + '" fill="url(#s)"/>'
            + '<circle cx="' + sx + '" cy="' + sy + '" r="' + Math.round(sr * 0.7) + '" fill="#ffd7a8" opacity=".85"/>'
            + '<path d="M0 355Q94 300 183 366T370 360T500 355V500H0Z" fill="' + hsl(h2, 40, 34) + '" opacity=".6"/>'
            + '<path d="M0 420Q130 378 250 431T500 406V500H0Z" fill="' + hsl(h2, 45, 26) + '" opacity=".7"/>'
            + '<path d="M0 460H500V500H0Z" fill="' + hsl(h2, 50, 20) + '" opacity=".6"/></svg>';
        coverCache[key] = "url('data:image/svg+xml;utf8," + encodeURIComponent(svg) + "') center / cover no-repeat";
        return coverCache[key];
    }

    // Tên file dạng "Nghệ sĩ - Tên bài" thì tách ra; không có thì nghệ sĩ là "CustMap Music"
    function meta(t) {
        var title = String((t && t.title) || '');
        var m = /^(.+?)\s+-\s+(.+)$/.exec(title);
        return m ? { artist: m[1].trim(), title: m[2].trim() } : { artist: 'CustMap Music', title: title };
    }

    // Thời lượng từng bài: chỉ biết sau khi trình duyệt tải được bài đó, nhớ lại để lần sau hiện luôn
    var DUR_KEY = 'custmap.music.durations';
    var durations = readJson(DUR_KEY) || {};
    function durationOf(id) { return Number(durations[id]) || 0; }

    /* ---------------------------------------------------------------- tùy chọn theo tài khoản */

    function loadPrefs() {
        var p = readJson(PREFS_KEY + uid) || {};
        prefs.fav = Array.isArray(p.fav) ? p.fav.filter(function (x) { return typeof x === 'string'; }) : [];
        prefs.recent = Array.isArray(p.recent) ? p.recent.filter(function (x) { return typeof x === 'string'; }).slice(0, 20) : [];
        prefs.shuffle = p.shuffle === true;
        prefs.repeat = (p.repeat === 'one' || p.repeat === 'off') ? p.repeat : 'all';
        prefs.volume = (typeof p.volume === 'number' && p.volume >= 0 && p.volume <= 1) ? p.volume : 0.8;
    }
    function savePrefs() { if (uid !== null) writeJson(PREFS_KEY + uid, prefs); }

    /* ---------------------------------------------------------------- lưu / khôi phục phiên phát */

    function writeQueue() {
        if (uid === null) return;
        writeJson(QUEUE_KEY, { uid: uid, items: queue });
    }
    function readQueue() {
        var q = readJson(QUEUE_KEY);
        if (!q || q.uid !== uid || !Array.isArray(q.items)) return null;
        return q.items.filter(function (t) { return t && typeof t.id === 'string' && /^[0-9a-f]{64}$/.test(t.id); })
            .map(function (t) { return { id: t.id, title: String(t.title || '') }; });
    }
    function persist(force, handoff) {
        if (!active || uid === null || !queue.length) return;
        var now = Date.now();
        if (!force && now - lastSave < SAVE_EVERY_MS) return;
        lastSave = now;
        writeJson(SESSION_KEY, {
            uid: uid, owner: tabId, index: index, position: audio.currentTime || 0,
            playing: isPlaying(), handoff: !!handoff || leaving, savedAt: now
        });
    }

    /* ---------------------------------------------------------------- phát nhạc */

    function load(i, startAt) {
        index = i;
        pendingSeek = startAt > 0 ? startAt : 0;
        audio.src = trackSrc(queue[i].id);
        if (pendingSeek > 0) { try { audio.currentTime = pendingSeek; } catch (e) { /* chưa đặt được, loadedmetadata sẽ đặt */ } }   // đặt sớm để trình duyệt xin đúng đoạn cần phát
        emit('track');
        updateMediaSession();
    }

    function startPlayback(auto) {
        audio.preload = 'auto';   // đã định phát thì tải ngay, khỏi chờ
        active = true;   // tab này giữ phiên phát; nếu bị chặn tự phát (auto) thì trả lại ở dưới
        blocked = false;
        var p = audio.play();
        if (!p || !p.catch) return Promise.resolve();
        return p.catch(function (e) {
            if (e && e.name === 'NotAllowedError') {
                blocked = true;
                if (auto) active = false;
                emit('state');
                emit('blocked');
            }
            // AbortError: vừa đổi bài nên lần phát cũ bị hủy, bỏ qua. Lỗi giải mã đi qua sự kiện 'error' của thẻ audio.
        });
    }

    function playIndex(i) {
        var n = queue.length;
        if (!n) return Promise.resolve();
        i = ((i % n) + n) % n;
        load(i, 0);
        var id = queue[i].id;
        prefs.recent = [id].concat(prefs.recent.filter(function (x) { return x !== id; })).slice(0, 20);
        savePrefs();
        return startPlayback(false);
    }

    function playQueue(tracks, startIndex) {
        var items = (tracks || []).map(function (t) { return { id: t.id, title: String(t.title || '') }; });
        if (!items.length) return Promise.resolve();
        queue = items;
        active = true;
        writeQueue();
        return playIndex(startIndex || 0);
    }

    function toggle() {
        if (index < 0) return Promise.resolve();
        if (audio.paused) return startPlayback(false);
        active = true;
        audio.pause();
        return Promise.resolve();
    }

    function next(auto) {
        var n = queue.length;
        if (!n) return Promise.resolve();
        if (prefs.shuffle && n > 1) {
            var j;
            do { j = Math.floor(Math.random() * n); } while (j === index);
            return playIndex(j);
        }
        var i = index + 1;
        if (i >= n) {
            if (auto && prefs.repeat === 'off') {      // hết danh sách và không lặp: dừng ở bài cuối
                active = true;
                persist(true);
                emit('state');
                return Promise.resolve();
            }
            i = 0;
        }
        return playIndex(i);
    }

    function prev() {
        if (index < 0) return Promise.resolve();
        if (audio.currentTime > 3) {
            active = true;
            audio.currentTime = 0;
            persist(true);
            return Promise.resolve();
        }
        return playIndex(index - 1);
    }

    function seekRatio(r) {
        if (!Number.isFinite(audio.duration) || audio.duration <= 0) return;
        active = true;
        audio.currentTime = Math.min(Math.max(r, 0), 1) * audio.duration;
        persist(true);
    }

    function setVolume(v) {
        v = Math.min(Math.max(Number(v) || 0, 0), 1);
        audio.volume = v;
        prefs.volume = v;
        savePrefs();
    }
    function setShuffle(on_) { prefs.shuffle = !!on_; savePrefs(); emit('state'); }
    function toggleShuffle() { setShuffle(!prefs.shuffle); return prefs.shuffle; }
    function cycleRepeat() {
        prefs.repeat = prefs.repeat === 'all' ? 'one' : (prefs.repeat === 'one' ? 'off' : 'all');
        savePrefs();
        emit('state');
        return prefs.repeat;
    }

    function isFav(id) { return prefs.fav.indexOf(id) !== -1; }
    function toggleFav(id) {
        var at = prefs.fav.indexOf(id);
        if (at === -1) prefs.fav.push(id); else prefs.fav.splice(at, 1);
        savePrefs();
        return at === -1;
    }

    function clearLocal() {
        active = false;
        queue = [];
        index = -1;
        pendingSeek = 0;
        audio.pause();
        audio.removeAttribute('src');
        audio.load();
        emit('track');
        emit('state');
    }
    function stop() {
        clearLocal();
        removeKey(SESSION_KEY);
        removeKey(QUEUE_KEY);
        if ('mediaSession' in navigator) {
            try { navigator.mediaSession.metadata = null; } catch (e) { /* bỏ qua */ }
        }
    }

    /* ---------------------------------------------------------------- sự kiện của thẻ audio */

    audio.addEventListener('loadedmetadata', function () {
        var cur = current();
        if (cur && Number.isFinite(audio.duration) && audio.duration > 0 && durations[cur.id] !== Math.round(audio.duration)) {
            durations[cur.id] = Math.round(audio.duration);
            writeJson(DUR_KEY, durations);
            emit('duration', cur.id);
        }
        if (Number.isFinite(audio.duration)) {
            var want = pendingSeek;
            if (resumeBase) { want = resumeBase.pos + Math.max(0, Date.now() - resumeBase.at) / 1000; resumeBase = null; }   // bù đúng khoảng im lặng khi chuyển trang
            want = Math.min(want, Math.max(0, audio.duration - 0.5));
            if (want > 0 && Math.abs(audio.currentTime - want) > 0.15) audio.currentTime = want;
        }
        pendingSeek = 0;
        emit('time');
    });
    audio.addEventListener('play', function () { blocked = false; persist(true); emit('state'); setPlaybackState(); });
    audio.addEventListener('pause', function () { persist(true); emit('state'); setPlaybackState(); });
    audio.addEventListener('playing', function () { errorStreak = 0; });
    audio.addEventListener('timeupdate', function () { persist(false); emit('time'); updatePositionState(); });
    audio.addEventListener('ended', function () {
        if (prefs.repeat === 'one') { audio.currentTime = 0; startPlayback(false); return; }
        next(true);
    });
    audio.addEventListener('error', function () {
        if (index < 0 || !audio.getAttribute('src')) return;
        errorStreak++;
        emit('error', 'Không phát được bài này. Hãy kiểm tra kết nối và quyền nghe nhạc.');
        // Bài hỏng: tự sang bài kế, nhưng dừng nếu cả danh sách đều lỗi (tránh vòng lặp vô hạn)
        if (active && queue.length > 1 && errorStreak < queue.length) {
            window.setTimeout(function () { next(true); }, 800);
        }
    });

    /* ---------------------------------------------------------------- điều khiển từ màn hình khóa / tai nghe */

    function registerMediaSession() {
        if (!('mediaSession' in navigator)) return;
        function set(action, handler) { try { navigator.mediaSession.setActionHandler(action, handler); } catch (e) { /* không hỗ trợ */ } }
        set('play', function () { startPlayback(false); });
        set('pause', function () { active = true; audio.pause(); });
        set('previoustrack', function () { prev(); });
        set('nexttrack', function () { next(false); });
        set('seekto', function (e) { if (Number.isFinite(e.seekTime)) { active = true; audio.currentTime = e.seekTime; } });
    }
    function updateMediaSession() {
        var t = current();
        if (!('mediaSession' in navigator) || !t || typeof MediaMetadata === 'undefined') return;
        try { navigator.mediaSession.metadata = new MediaMetadata({ title: meta(t).title, artist: meta(t).artist, album: 'CustMap Music' }); }
        catch (e) { /* bỏ qua */ }
    }
    function setPlaybackState() {
        if (!('mediaSession' in navigator)) return;
        try { navigator.mediaSession.playbackState = audio.paused ? 'paused' : 'playing'; } catch (e) { /* bỏ qua */ }
    }
    function updatePositionState() {
        if (!('mediaSession' in navigator) || !navigator.mediaSession.setPositionState) return;
        if (!Number.isFinite(audio.duration) || audio.duration <= 0) return;
        try {
            navigator.mediaSession.setPositionState({
                duration: audio.duration, playbackRate: audio.playbackRate || 1,
                position: Math.min(audio.currentTime, audio.duration)
            });
        } catch (e) { /* bỏ qua */ }
    }

    /* ---------------------------------------------------------------- thư viện (danh sách bài trên server) */

    function loadLibrary(force) {
        if (library && !force) return Promise.resolve(library);
        return fetch(API, { headers: { 'Accept': 'application/json' } }).then(function (res) {
            if (res.redirected && res.url.indexOf('/login') !== -1) {
                window.location.href = ctx + '/login';
                throw new Error('Phiên đăng nhập đã hết hạn');
            }
            if (res.status === 401 || res.status === 403) throw new Error('Tài khoản chưa có quyền nghe nhạc.');
            if (!res.ok) throw new Error('Không tải được thư viện nhạc (mã ' + res.status + ').');
            return res.json();
        }).then(function (list) {
            library = Array.isArray(list) ? list : [];
            var ids = {};
            library.forEach(function (t) { ids[t.id] = true; });
            var f = prefs.fav.filter(function (id) { return ids[id]; });
            var r = prefs.recent.filter(function (id) { return ids[id]; });
            if (f.length !== prefs.fav.length || r.length !== prefs.recent.length) { prefs.fav = f; prefs.recent = r; savePrefs(); }
            return library;
        });
    }

    /* ---------------------------------------------------------------- đồng bộ giữa các tab */

    function adopt(sess) {
        var items = readQueue();
        if (items) queue = items;
        if (!queue.length) { clearLocal(); return; }
        var i = Math.min(Math.max(sess.index | 0, 0), queue.length - 1);
        var cur = current();
        if (!cur || cur.id !== queue[i].id) load(i, sess.position || 0);
        else { index = i; emit('track'); }
        emit('state');
    }
    window.addEventListener('storage', function (e) {
        if (uid === null || (e.key !== SESSION_KEY && e.key !== QUEUE_KEY)) return;
        var sess = readJson(SESSION_KEY);
        if (!sess || sess.uid !== uid) { if (!active) clearLocal(); return; }   // phiên bị tắt ở tab khác
        if (sess.owner === tabId) return;
        if (sess.playing && isPlaying()) { active = false; audio.pause(); }    // tab khác vừa phát: nhường tiếng
        if (!active) adopt(sess);
    });
    window.addEventListener('pagehide', function () { leaving = true; persist(true, true); });
    window.addEventListener('pageshow', function (e) { if (e.persisted) { leaving = false; persist(true, false); } });
    document.addEventListener('visibilitychange', function () { if (document.hidden) persist(true, false); });

    /* ---------------------------------------------------------------- thanh nhạc nhỏ (ở mọi trang, trừ trang Nghe nhạc) */

    var dock = null;
    var dockEls = null;

    function buildDock() {
        if (dock || !document.body) return;
        dock = document.createElement('div');
        dock.className = 'mdock';
        dock.id = 'musicDock';
        dock.setAttribute('role', 'region');
        dock.setAttribute('aria-label', 'Trình phát nhạc');
        dock.innerHTML =
            '<a class="mdock-info" title="Mở trang Nghe nhạc">' +
                '<span class="mdock-cover"></span>' +
                '<span class="mdock-text"><b class="mdock-title"></b><small class="mdock-sub"></small></span>' +
            '</a>' +
            '<button type="button" class="mdock-btn" data-act="prev" aria-label="Bài trước"><i class="fa-solid fa-backward-step"></i></button>' +
            '<button type="button" class="mdock-btn mdock-play" data-act="play" aria-label="Phát"><i class="fa-solid fa-play"></i></button>' +
            '<button type="button" class="mdock-btn" data-act="next" aria-label="Bài tiếp theo"><i class="fa-solid fa-forward-step"></i></button>' +
            '<button type="button" class="mdock-btn mdock-close" data-act="close" aria-label="Tắt trình phát"><i class="fa-solid fa-xmark"></i></button>' +
            '<div class="mdock-bar"><span></span></div>';
        dockEls = {
            link: dock.querySelector('.mdock-info'),
            cover: dock.querySelector('.mdock-cover'),
            title: dock.querySelector('.mdock-title'),
            sub: dock.querySelector('.mdock-sub'),
            play: dock.querySelector('.mdock-play i'),
            bar: dock.querySelector('.mdock-bar span')
        };
        dockEls.link.setAttribute('href', ctx + '/music');
        dock.addEventListener('click', function (e) {
            if (onMusicPage && e.target.closest('.mdock-info')) { e.preventDefault(); emit('openplayer'); return; }   // đang ở trang nhạc: mở màn Đang phát, không tải lại trang
            var b = e.target.closest('[data-act]');
            if (!b) return;
            var act = b.getAttribute('data-act');
            if (act === 'play') toggle();
            else if (act === 'next') next(false);
            else if (act === 'prev') prev();
            else if (act === 'close') stop();
        });
        document.body.appendChild(dock);
        document.body.classList.add('has-music-dock');
    }
    function removeDock() {
        if (!dock) return;
        dock.remove();
        dock = null;
        dockEls = null;
        document.body.classList.remove('has-music-dock');
    }
    function updateDock() {
        var t = current();
        if (!t) { removeDock(); return; }
        buildDock();
        if (!dock) return;
        dockEls.title.textContent = meta(t).title;
        dockEls.cover.style.background = coverBackground(t.id);
        refreshDockState();
        refreshDockTime();
    }
    function refreshDockState() {
        if (!dock) return;
        var playing = isPlaying();
        dockEls.play.className = 'fa-solid ' + (playing ? 'fa-pause' : 'fa-play');
        dockEls.play.parentNode.setAttribute('aria-label', playing ? 'Tạm dừng' : 'Phát');
        var t = current();
        dockEls.sub.textContent = blocked ? 'Chạm ▶ để phát tiếp' : ((t ? meta(t).artist : '') + ' · ' + (playing ? 'Đang phát' : 'Tạm dừng'));
    }
    function refreshDockTime() {
        if (!dock) return;
        var pct = Number.isFinite(audio.duration) && audio.duration > 0 ? (audio.currentTime / audio.duration) * 100 : 0;
        dockEls.bar.style.width = Math.min(100, Math.max(0, pct)) + '%';
    }
    on('track', updateDock);
    on('state', refreshDockState);
    on('time', refreshDockTime);

    /* ---------------------------------------------------------------- khởi động */

    function init(me) {
        uid = me.id;
        loadPrefs();
        audio.volume = prefs.volume;
        registerMediaSession();

        var sess = readJson(SESSION_KEY);
        var items = readQueue();
        if (sess && sess.uid === uid && items && items.length) {
            queue = items;
            var i = Math.min(Math.max(sess.index | 0, 0), queue.length - 1);
            var elapsed = Math.max(0, Date.now() - (sess.savedAt || 0));
            // Chỉ tự phát tiếp khi trang trước vừa đóng để chuyển sang trang này (không phải tab khác đang phát)
            var resume = sess.playing === true && sess.handoff === true && elapsed < HANDOFF_MS;
            load(i, (sess.position || 0) + (resume ? elapsed / 1000 : 0));
            if (resume) {
                resumeBase = { pos: sess.position || 0, at: sess.savedAt || Date.now() };
                startPlayback(true);
            }
        }
        inited = true;
        resolveReady();
        emit('ready');
    }

    // Đăng nhập tài khoản khác (id khác với bản lưu): bỏ phiên nhạc cũ, không phát tiếp nhạc của người trước
    function switchUser(id) {
        clearLocal();
        removeKey(SESSION_KEY);
        removeKey(QUEUE_KEY);
        uid = id;
        loadPrefs();
        audio.volume = prefs.volume;
        emit('state');
    }

    window.CustMapMusic = {
        ready: ready,
        ctx: ctx,
        onMusicPage: onMusicPage,
        loadLibrary: loadLibrary,
        playQueue: playQueue,
        playIndex: playIndex,
        toggle: toggle,
        next: function () { return next(false); },
        prev: prev,
        seekRatio: seekRatio,
        setVolume: setVolume,
        setShuffle: setShuffle,
        toggleShuffle: toggleShuffle,
        cycleRepeat: cycleRepeat,
        stop: stop,
        current: current,
        queue: function () { return queue.slice(); },
        index: function () { return index; },
        meta: meta,
        durationOf: durationOf,
        isPlaying: isPlaying,
        isBlocked: function () { return blocked; },
        currentTime: function () { return audio.currentTime || 0; },
        duration: function () { return Number.isFinite(audio.duration) ? audio.duration : 0; },
        volume: function () { return audio.volume; },
        shuffle: function () { return prefs.shuffle; },
        repeat: function () { return prefs.repeat; },
        isFav: isFav,
        toggleFav: toggleFav,
        favIds: function () { return prefs.fav.slice(); },
        recentIds: function () { return prefs.recent.slice(); },
        coverBackground: coverBackground,
        hideDock: function (hidden) { document.body.classList.toggle('mdock-hidden', !!hidden); },
        fmt: fmt,
        on: on
    };

    // Lấy tài khoản đang đăng nhập (AppNav do app-nav.js tạo, gọi /api/auth/me đúng một lần cho cả trang)
    // Có id lần trước thì khởi động NGAY (phát tiếp liền khi chuyển trang), rồi mới đối chiếu với server
    var cachedUid = readJson(UID_KEY);
    if (typeof cachedUid === 'number') init({ id: cachedUid });
    if (window.AppNav && typeof window.AppNav.getMe === 'function') {
        window.AppNav.getMe().then(function (me) {
            if (me && me.id !== undefined && me.id !== null) writeJson(UID_KEY, me.id);
            if (!inited) init(me);
            else if (me.id !== uid) switchUser(me.id);
        }).catch(function (e) { if (!inited) rejectReady(e); });
    } else if (!inited) {
        rejectReady(new Error('Thiếu AppNav'));
    }
})();
