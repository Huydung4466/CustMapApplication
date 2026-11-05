/*
 * music.js - Giao diện trang Nghe nhạc (music.jsp), dựng theo bản mẫu music_spotify_style_demo.html.
 * Ba màn trong cùng một trang: Trang chủ nhạc (home), Chi tiết playlist (playlist), Đang phát (player).
 * Việc phát nhạc, nhớ bài đang nghe, phát tiếp khi chuyển trang do js/music-player.js (window.CustMapMusic) đảm nhiệm;
 * file này chỉ vẽ giao diện và gọi bộ phát. Cần app-common.js (escapeHtml, showToast).
 * Thư viện chỉ có MỘT playlist ("Thư viện chung") gồm mọi bài trên server.
 */
(function () {
    'use strict';
    var M = window.CustMapMusic;
    var root = document.getElementById('music');
    if (!M || !root) return;

    function $(id) { return document.getElementById(id); }
    function toast(message, isError) { if (typeof showToast === 'function') showToast(message, !!isError); }
    function esc(s) { return typeof escapeHtml === 'function' ? escapeHtml(s) : String(s); }
    function icon(id, cls) { return '<svg class="ic ' + (cls || '') + '" aria-hidden="true"><use href="#i-' + id + '"/></svg>'; }
    function cover(id) { return 'style="background:' + M.coverBackground(id) + '"'; }

    var content = $('muContent');
    var sheet = $('muSheet');
    var sheetBody = $('muSheetBody');
    var admin = $('muAdmin');

    var FILTERS = ['Dành cho bạn', 'Playlist', 'Trực tuyến', 'Gần đây', 'Yêu thích'];
    var state = { view: 'home', filter: 'Dành cho bạn', query: '', tracks: [], embeds: [], embedId: null, embedReturn: 'home',
        loaded: false, error: '', playerReturn: 'home' };
    var canManage = !!$('musicImport');   // hộp Nhập nhạc chỉ có với người được thêm nhạc
    var seeking = false;

    function norm(s) {   // bỏ dấu để tìm "bai hat" ra "Bài hát"
        return String(s || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd');
    }
    function byId() { var m = {}; state.tracks.forEach(function (t) { m[t.id] = t; }); return m; }
    function trackOf(id) { return byId()[id] || null; }
    function searchMatches() {
        var q = norm(state.query).trim();
        if (!q) return [];
        return state.tracks.filter(function (t) { var m = M.meta(t); return norm(m.title + ' ' + m.artist).indexOf(q) !== -1; });
    }
    // Danh sách bài của từng khu vực (để biết phát hàng chờ nào khi bấm vào một bài)
    function listFor(kind) {
        if (kind === 'fav') { var a = byId(); return M.favIds().map(function (id) { return a[id]; }).filter(Boolean); }
        if (kind === 'recent') { var b = byId(); return M.recentIds().map(function (id) { return b[id]; }).filter(Boolean); }
        if (kind === 'search') return searchMatches();
        return state.tracks;
    }
    function libraryCover() { return state.tracks.length ? state.tracks[0].id : 'library'; }

    /* ---------------------------------------------------------------- các khối giao diện */

    function songRow(t, i) {
        var cur = M.current();
        var playing = !!cur && cur.id === t.id;
        var m = M.meta(t);
        var d = M.durationOf(t.id);
        var fav = M.isFav(t.id);
        return '<div class="track-row' + (playing ? ' playing' : '') + '" data-id="' + t.id + '">'
            + '<span class="track-number" data-n="' + (i + 1) + '">' + (playing && M.isPlaying() ? icon('wave', 'sm') : (i + 1)) + '</span>'
            + '<button type="button" data-act="play" data-id="' + t.id + '" class="mz-row gap8" style="flex:1;min-width:0;background:none;border:0;padding:0;text-align:left">'
            + '<span class="cover" ' + cover(t.id) + '></span>'
            + '<span class="track-info"><span class="track-title">' + esc(m.title) + '</span><span class="track-subtitle">' + esc(m.artist) + '</span></span></button>'
            + '<span class="track-duration" data-dur="' + t.id + '">' + (d ? M.fmt(d) : '') + '</span>'
            + '<button type="button" class="track-fav' + (fav ? ' on' : '') + '" data-act="fav" data-id="' + t.id + '" aria-pressed="' + fav + '" aria-label="' + (fav ? 'Bỏ yêu thích ' : 'Yêu thích ') + esc(m.title) + '">' + icon('heart') + '</button>'
            + '<button type="button" class="track-menu" data-act="trackmenu" data-id="' + t.id + '" aria-label="Thêm tùy chọn cho ' + esc(m.title) + '">' + icon('more') + '</button></div>';
    }
    function trackList(items, kind) {
        return '<div class="track-list" data-list="' + kind + '">' + items.map(songRow).join('') + '</div>';
    }
    function playlistCard() {
        return '<button type="button" class="playlist-card" data-act="openplaylist" aria-label="Mở Thư viện chung">'
            + '<span class="cover" ' + cover(libraryCover()) + '><span class="cover-play">' + icon('play', 'sm') + '</span></span>'
            + '<strong>Thư viện chung</strong><span class="subtle">' + state.tracks.length + ' bài hát</span></button>';
    }

    /* ---- nhạc nghe trực tuyến (Spotify / YouTube / SoundCloud nhúng khung phát chính thức) */

    function embedOf(id) { return state.embeds.filter(function (e) { return String(e.id) === String(id); })[0] || null; }
    function embedMatches() {
        var q = norm(state.query).trim();
        if (!q) return [];
        return state.embeds.filter(function (e) { return norm(e.title + ' ' + e.providerLabel).indexOf(q) !== -1; });
    }
    function embedCard(e) {
        return '<button type="button" class="embed-card" data-act="openembed" data-id="' + esc(e.id) + '">'
            + '<span class="embed-badge embed-' + esc(String(e.provider).toLowerCase()) + '">' + esc(e.providerLabel) + '</span>'
            + '<span class="embed-info"><strong>' + esc(e.title) + '</strong><span class="subtle">' + esc(e.kindLabel) + '</span></span>'
            + '<span class="embed-go">' + icon('play', 'sm') + '</span></button>';
    }
    function embedList(items) {
        return '<div class="embed-list">' + items.map(embedCard).join('') + '</div>';
    }
    function embedHtml() {
        var e = embedOf(state.embedId);
        if (!e) return homeHtml();
        var frameStyle = e.height > 0 ? 'height:' + Number(e.height) + 'px' : 'aspect-ratio:16/9';
        return '<div class="page-enter">'
            + '<div class="backline"><button type="button" class="backbtn" data-act="embedback" aria-label="Quay lại">' + icon('back') + '</button><h2>Nghe trực tuyến</h2></div>'
            + '<div class="embed-head"><span class="embed-badge embed-' + esc(String(e.provider).toLowerCase()) + '">' + esc(e.providerLabel) + '</span>'
            + '<h2>' + esc(e.title) + '</h2><span class="subtle">' + esc(e.kindLabel) + (e.createdByName ? ' · thêm bởi ' + esc(e.createdByName) : '') + '</span></div>'
            + '<iframe class="embed-frame" style="' + frameStyle + '" src="' + esc(e.embedUrl) + '" title="' + esc(e.title) + '" loading="lazy"'
            + ' allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture" allowfullscreen'
            + ' referrerpolicy="strict-origin-when-cross-origin"></iframe>'
            + '<div class="dual-action">'
            + '<a class="secondary-btn" href="' + esc(e.sourceUrl) + '" target="_blank" rel="noopener noreferrer">Mở trong ' + esc(e.providerLabel) + '</a>'
            + (canManage ? '<button type="button" class="secondary-btn" data-act="delembed" data-id="' + esc(e.id) + '">Gỡ khỏi thư viện</button>' : '')
            + '</div>'
            + '<p class="control-note">Nhạc phát từ ' + esc(e.providerLabel) + ' nên sẽ dừng khi rời trang này. '
            + (e.provider === 'SPOTIFY' ? 'Chưa đăng nhập Spotify trên trình duyệt thì mỗi bài thường chỉ nghe được 30 giây.' : '') + '</p></div>';
    }
    function openEmbed(id) {
        if (!embedOf(id)) return;
        if (M.isPlaying()) M.toggle();   // tránh hai nguồn nhạc phát cùng lúc
        state.embedReturn = state.view === 'embed' ? state.embedReturn : state.view;
        state.embedId = id;
        nav('embed');
    }
    async function deleteEmbed(id) {
        var e = embedOf(id);
        if (!e || !window.confirm('Gỡ "' + e.title + '" khỏi thư viện chung?')) return;
        try {
            var res = await fetch(M.ctx + '/api/music/embeds/' + encodeURIComponent(id), { method: 'DELETE' });
            if (!res.ok && res.status !== 404) throw new Error();
            toast('Đã gỡ khỏi thư viện.');
            state.embedId = null;
            state.view = state.embedReturn || 'home';
            await loadLibrary();
        } catch (err) {
            toast('Chưa gỡ được, vui lòng thử lại.', true);
        }
    }

    function homeHtml() {
        var f = state.filter;
        var pills = FILTERS.map(function (s) {
            return '<button type="button" data-act="filterpill" data-label="' + s + '" class="pill' + (f === s ? ' selected' : '') + '">' + s + '</button>';
        }).join('');
        var body;
        if (f === 'Trực tuyến') {
            body = '<div class="section-head"><h3>Nghe trực tuyến</h3><span class="subtle">Spotify · YouTube · SoundCloud</span></div>'
                + (state.embeds.length ? embedList(state.embeds)
                    : '<div class="empty-result">Chưa có playlist trực tuyến nào.'
                    + (canManage ? ' Bấm "+ Thêm nhạc" rồi dán link Spotify, YouTube hoặc SoundCloud.' : '') + '</div>');
        } else if (f === 'Gần đây' || f === 'Yêu thích') {
            var kind = f === 'Yêu thích' ? 'fav' : 'recent';
            var items = listFor(kind);
            body = '<div class="section-head"><h3>' + (kind === 'fav' ? 'Yêu thích' : 'Đã nghe gần đây') + '</h3></div>'
                + (items.length ? trackList(items, kind)
                    : '<div class="empty-result">' + (kind === 'fav' ? 'Bạn chưa yêu thích bài nào. Chạm trái tim bên cạnh bài hát để lưu lại. 💗' : 'Bạn chưa nghe bài nào. Chọn một playlist để bắt đầu nhé! 🎵') + '</div>');
        } else {
            var songs = state.tracks.slice(0, 8);
            body = '<div class="section-head"><h3>' + (f === 'Playlist' ? 'Tất cả playlist' : 'Playlist nổi bật') + '</h3>'
                + '<button type="button" class="text-link" data-act="filterpill" data-label="Playlist">Xem tất cả ›</button></div>'
                + '<div class="playlist-cards">' + playlistCard() + '</div>'
                + (state.embeds.length ? '<div class="section-head"><h3>Nghe trực tuyến</h3>'
                    + '<button type="button" class="text-link" data-act="filterpill" data-label="Trực tuyến">Xem tất cả ›</button></div>'
                    + embedList(state.embeds.slice(0, f === 'Playlist' ? state.embeds.length : 4)) : '')
                + '<div class="section-head"><h3>Bài hát</h3><span class="subtle">Thư viện chung</span></div>'
                + (songs.length ? trackList(songs, 'lib') : '<div class="empty-result">Thư viện chưa có bài hát.</div>');
        }
        return '<div class="page-enter">'
            + '<p class="greeting">🎧 Chào mừng quay lại</p><h1 class="page-title">Hôm nay nghe gì?</h1>'
            + '<div class="search-shell"><div class="search-wrap">' + icon('search', 'sm')
            + '<input id="muSearch" class="search-input" placeholder="Tìm bài hát, playlist..." autocomplete="off" aria-label="Tìm kiếm nhạc" value="' + esc(state.query) + '"></div>'
            + '<button type="button" class="filter-btn" data-act="focussearch" aria-label="Tìm bài hát">' + icon('filter') + '</button></div>'
            + '<div id="muResults" class="search-results" hidden></div>'
            + '<div class="pills">' + pills + '</div>'
            + (f === 'Dành cho bạn' ? '<div class="hero"><div class="deco"></div><div class="headphone">' + icon('headphones') + '</div>'
                + '<h2>Thêm nhạc,<br>thêm cảm hứng.</h2><p>Một khoảng lặng nhẹ nhàng giữa những ca làm việc bận rộn.</p>'
                + '<button type="button" class="white-btn" data-act="openplaylist">' + icon('play', 'sm') + ' Nghe ngay</button></div>' : '')
            + body
            + '<div class="music-note">' + icon('lock') + '<span>Nhạc vẫn phát tiếp khi bạn chuyển trang trong CustMap. Điều khiển trên màn hình khóa có thể hoạt động tùy trình duyệt và thiết bị.</span></div>'
            + '</div>';
    }

    function playlistHtml() {
        return '<div class="page-enter">'
            + '<div class="backline"><button type="button" class="backbtn" data-act="nav" data-view="home" aria-label="Quay lại">' + icon('back') + '</button><h2>Chi tiết playlist</h2></div>'
            + '<div class="detail-head"><span class="cover" ' + cover(libraryCover()) + '></span><div class="detail-text">'
            + '<span class="eyebrow">CustMap Music</span><h2>Thư viện chung</h2>'
            + '<div class="subtle">' + icon('headphones', 'sm') + ' ' + state.tracks.length + ' bài hát</div>'
            + '<p>Các bài hát được quản trị viên thêm vào CustMap.</p>'
            + '<div class="detail-actions">'
            + '<button type="button" class="icon-button" data-act="favall" aria-label="Yêu thích cả playlist">' + icon('heart') + '</button>'
            + '<button type="button" class="icon-button" data-act="sheet" data-sheet="queue" aria-label="Hàng chờ">' + icon('more') + '</button></div></div></div>'
            + '<div class="dual-action"><button type="button" class="secondary-btn" data-act="shuffleall">' + icon('shuffle', 'sm') + ' Phát ngẫu nhiên</button>'
            + '<button type="button" class="primary-btn" data-act="playall">' + icon('play', 'sm') + ' Phát tất cả</button></div>'
            + (state.tracks.length ? trackList(state.tracks, 'lib') : '<div class="empty-result">Thư viện chưa có bài hát.</div>')
            + '<p class="control-note">Bấm vào bài hát để phát và mở trang Đang phát.</p></div>';
    }

    function playerHtml() {
        var t = M.current();
        if (!t) return homeHtml();
        var m = M.meta(t);
        var fav = M.isFav(t.id);
        var repeat = M.repeat();
        return '<div class="page-enter">'
            + '<div class="backline"><button type="button" class="backbtn" data-act="playerback" aria-label="Thu nhỏ trình phát">' + icon('down') + '</button><h2>Đang phát</h2>'
            + '<button type="button" class="icon-button" style="margin-left:auto" data-act="sheet" data-sheet="queue" aria-label="Xem hàng chờ">' + icon('more') + '</button></div>'
            + '<div class="player-label">' + icon('wave', 'sm') + ' ĐANG PHÁT · CUSTMAP MUSIC</div>'
            + '<div class="full-cover" ' + cover(t.id) + ' role="img" aria-label="Ảnh bìa ' + esc(m.title) + '"></div>'
            + '<div class="now-meta"><div><h2>' + esc(m.title) + '</h2><span class="artist">' + esc(m.artist) + '</span></div>'
            + '<button type="button" class="like-square' + (fav ? ' on' : '') + '" data-act="fav" data-id="' + t.id + '" aria-pressed="' + fav + '" aria-label="' + (fav ? 'Bỏ yêu thích' : 'Yêu thích bài hát') + '">' + icon('heart') + '</button></div>'
            + '<div class="progress-area"><input id="muSeek" class="seek" aria-label="Tiến độ phát nhạc" type="range" value="0" min="0" max="100" step=".1">'
            + '<div class="times"><span id="muTimeNow">0:00</span><span id="muTimeLeft">-0:00</span></div></div>'
            + '<div class="player-controls">'
            + '<button type="button" class="icon-button' + (M.shuffle() ? ' active' : '') + '" id="muShuffleBtn" data-act="shuffle" aria-label="Phát ngẫu nhiên" aria-pressed="' + M.shuffle() + '" title="Ngẫu nhiên">' + icon('shuffle') + '</button>'
            + '<button type="button" class="icon-button" data-act="prev" aria-label="Bài trước">' + icon('prev') + '</button>'
            + '<button type="button" class="big-play" id="muBigPlay" data-act="toggle" aria-label="' + (M.isPlaying() ? 'Tạm dừng' : 'Phát') + '">' + icon(M.isPlaying() ? 'pause' : 'play') + '</button>'
            + '<button type="button" class="icon-button" data-act="next" aria-label="Bài tiếp theo">' + icon('next') + '</button>'
            + '<button type="button" class="icon-button' + (repeat !== 'off' ? ' active' : '') + '" id="muRepeatBtn" data-act="repeat" aria-label="Chế độ lặp" title="Lặp">' + icon('repeat') + (repeat === 'one' ? '<span class="one">1</span>' : '') + '</button></div>'
            + '<div class="background-card"><span class="lock">' + icon('lock') + '</span><div><strong>Phát nhạc khi khóa màn hình</strong><p id="muBgNote">'
            + ('mediaSession' in navigator ? 'Đã tích hợp Media Session. Khả năng phát nền tùy trình duyệt và thiết bị.' : 'Trình duyệt này chưa hỗ trợ Media Session. Có thể không điều khiển được khi khóa màn hình.') + '</p></div></div>'
            + '<div class="extra-actions" style="grid-template-columns:repeat(2,1fr)">'
            + '<button type="button" data-act="sheet" data-sheet="queue">' + icon('list') + '<small>Hàng chờ</small></button>'
            + '<button type="button" data-act="sheet" data-sheet="volume">' + icon('volume') + '<small>Âm lượng</small></button></div>'
            + '<p class="control-note">Giữ trang Nhạc hoặc chuyển trang trong CustMap, nhạc vẫn phát tiếp.</p></div>';
    }

    function loadingHtml(text, withRetry) {
        return '<div class="page-enter"><div class="empty-result">' + esc(text)
            + (withRetry ? '<br><button type="button" class="secondary-btn retry" data-act="retry" style="margin:10px auto 0">Thử lại</button>' : '') + '</div></div>';
    }

    /* ---------------------------------------------------------------- vẽ / cập nhật */

    function render() {
        if (!state.loaded) { content.innerHTML = loadingHtml('Đang tải thư viện...'); return; }
        if (state.error) { content.innerHTML = loadingHtml(state.error, true); return; }
        if (state.view === 'player' && !M.current()) state.view = 'home';
        if (state.view === 'embed' && !embedOf(state.embedId)) state.view = 'home';
        content.innerHTML = state.view === 'playlist' ? playlistHtml()
            : state.view === 'player' ? playerHtml()
            : state.view === 'embed' ? embedHtml() : homeHtml();
        if (admin) admin.hidden = state.view !== 'home';
        M.hideDock(state.view === 'player');
        if (state.view === 'home') updateSearchResults();
        refreshTime();
    }

    function nav(view) {
        state.view = view;
        render();
        window.scrollTo({ top: 0 });
    }

    function updateSearchResults() {
        var box = $('muResults');
        if (!box) return;
        var q = state.query.trim();
        if (!q) { box.hidden = true; box.innerHTML = ''; return; }
        var items = searchMatches();
        var online = embedMatches();
        box.hidden = false;
        var showPl = norm('Thư viện chung').indexOf(norm(q)) !== -1;
        box.innerHTML = (items.length ? trackList(items, 'search') : '')
            + (online.length ? embedList(online) : '')
            + (showPl ? '<div class="playlist-cards" style="margin-top:10px">' + playlistCard() + '</div>' : '')
            + (!items.length && !online.length && !showPl ? '<div class="empty-result">Không có bài hát hoặc playlist phù hợp.</div>' : '');
    }

    // Đánh dấu bài đang phát trong mọi danh sách đang hiện (không vẽ lại cả trang)
    function markPlaying() {
        var cur = M.current();
        var playing = M.isPlaying();
        var rows = content.querySelectorAll('.track-row');
        for (var i = 0; i < rows.length; i++) {
            var on = !!cur && rows[i].getAttribute('data-id') === cur.id;
            rows[i].classList.toggle('playing', on);
            var num = rows[i].querySelector('.track-number');
            if (num) num.innerHTML = on && playing ? icon('wave', 'sm') : num.getAttribute('data-n');
        }
    }

    function refreshState() {
        var big = $('muBigPlay');
        if (big) {
            big.innerHTML = icon(M.isPlaying() ? 'pause' : 'play');
            big.setAttribute('aria-label', M.isPlaying() ? 'Tạm dừng' : 'Phát');
        }
        var sh = $('muShuffleBtn');
        if (sh) { sh.classList.toggle('active', M.shuffle()); sh.setAttribute('aria-pressed', String(M.shuffle())); }
        var rp = $('muRepeatBtn');
        if (rp) {
            var mode = M.repeat();
            rp.classList.toggle('active', mode !== 'off');
            var one = rp.querySelector('.one');
            if (mode === 'one' && !one) rp.insertAdjacentHTML('beforeend', '<span class="one">1</span>');
            if (mode !== 'one' && one) one.remove();
        }
        markPlaying();
    }

    function refreshTime() {
        var seek = $('muSeek');
        if (!seek) return;
        var dur = M.duration();
        var pos = M.currentTime();
        if (!seeking && document.activeElement !== seek) seek.value = dur > 0 ? Math.min(100, pos / dur * 100) : 0;
        $('muTimeNow').textContent = M.fmt(pos);
        $('muTimeLeft').textContent = '-' + M.fmt(Math.max(0, dur - pos));
    }

    function refreshDurations(id) {
        var els = content.querySelectorAll('[data-dur="' + id + '"]');
        for (var i = 0; i < els.length; i++) els[i].textContent = M.fmt(M.durationOf(id));
    }

    /* ---------------------------------------------------------------- cửa sổ trượt từ dưới lên */

    function openSheet(type, id) {
        var body = '';
        if (type === 'queue') {
            var q = M.queue();
            var at = M.index();
            body = '<h3>Hàng chờ phát nhạc</h3>' + (q.length ? q.map(function (t, i) {
                var m = M.meta(t);
                return '<button type="button" class="item' + (i === at ? ' playing' : '') + '" data-act="qplay" data-i="' + i + '">'
                    + (i === at ? icon('wave', 'sm') : '<span style="width:16px;text-align:center;color:#7d8aa0">' + (i + 1) + '</span>') + '<span>' + esc(m.title) + '</span></button>';
            }).join('') : '<p class="subtle">Chưa có bài nào trong hàng chờ. Chọn một bài để bắt đầu.</p>');
        } else if (type === 'volume') {
            body = '<h3>🔊 Âm lượng</h3><p class="subtle">Điều chỉnh âm lượng của trình phát.</p>'
                + '<input type="range" min="0" max="1" step=".01" value="' + M.volume() + '" id="muVolume" aria-label="Âm lượng" style="width:100%;accent-color:var(--red);margin:16px 0">'
                + '<p class="subtle">Bạn cũng có thể dùng nút âm lượng trên thiết bị.</p>';
        } else if (type === 'track') {
            var t = trackOf(id);
            if (!t) return;
            var fav = M.isFav(id);
            body = '<h3>' + esc(M.meta(t).title) + '</h3>'
                + '<button type="button" class="primary-btn" data-act="play" data-id="' + id + '" style="width:100%">' + icon('play', 'sm') + ' Phát bài hát</button>'
                + '<div style="height:9px"></div>'
                + '<button type="button" class="secondary-btn" data-act="fav" data-id="' + id + '" style="width:100%">' + icon('heart', 'sm') + ' ' + (fav ? 'Bỏ yêu thích' : 'Thêm yêu thích') + '</button>';
        }
        sheetBody.innerHTML = body;
        sheet.classList.add('visible');
    }
    function closeSheet() { sheet.classList.remove('visible'); }

    /* ---------------------------------------------------------------- thao tác */

    function playFrom(kind, id) {
        var cur = M.current();
        if (cur && cur.id === id && M.queue().length) {      // bài đang phát: bấm lại thì mở màn Đang phát
            state.playerReturn = state.view === 'player' ? state.playerReturn : state.view;
            nav('player');
            return;
        }
        var items = listFor(kind);
        var at = items.findIndex(function (t) { return t.id === id; });
        if (at < 0) { items = state.tracks; at = items.findIndex(function (t) { return t.id === id; }); }
        if (at < 0) return;
        state.playerReturn = state.view === 'player' ? state.playerReturn : state.view;
        M.playQueue(items, at);
        nav('player');
    }
    function playAll(shuffle) {
        var items = state.tracks;
        if (!items.length) { toast('Thư viện chưa có bài hát.'); return; }
        M.setShuffle(shuffle);
        state.playerReturn = 'playlist';
        M.playQueue(items, shuffle ? Math.floor(Math.random() * items.length) : 0);
        nav('player');
    }

    root.addEventListener('click', function (e) {
        if (e.target === sheet) { closeSheet(); return; }
        var btn = e.target.closest('[data-act]');
        if (!btn || !root.contains(btn)) return;
        var id = btn.getAttribute('data-id');
        switch (btn.getAttribute('data-act')) {
            case 'nav': nav(btn.getAttribute('data-view')); break;
            case 'openplaylist': nav('playlist'); break;
            case 'filterpill': state.filter = btn.getAttribute('data-label'); render(); break;
            case 'focussearch': { var s = $('muSearch'); if (s) s.focus(); break; }
            case 'play': {
                closeSheet();
                var list = btn.closest('[data-list]');
                playFrom(list ? list.getAttribute('data-list') : 'lib', id);
                break;
            }
            case 'playall': playAll(false); break;
            case 'shuffleall': playAll(true); break;
            case 'favall': state.tracks.forEach(function (t) { if (!M.isFav(t.id)) M.toggleFav(t.id); }); toast('Đã lưu các bài trong playlist vào mục yêu thích.'); render(); break;
            case 'fav': M.toggleFav(id); closeSheet(); render(); break;
            case 'trackmenu': openSheet('track', id); break;
            case 'sheet': openSheet(btn.getAttribute('data-sheet'), id); break;
            case 'closesheet': closeSheet(); break;
            case 'qplay': { var q = M.queue(); closeSheet(); if (q.length) M.playIndex(Number(btn.getAttribute('data-i'))); break; }
            case 'playerback': nav(state.playerReturn || 'home'); break;
            case 'toggle': M.toggle(); break;
            case 'prev': M.prev(); break;
            case 'next': M.next(); break;
            case 'shuffle': M.toggleShuffle(); toast(M.shuffle() ? 'Đã bật phát ngẫu nhiên' : 'Đã tắt phát ngẫu nhiên'); break;
            case 'repeat': { var mode = M.cycleRepeat(); toast(mode === 'one' ? 'Lặp lại một bài' : (mode === 'all' ? 'Lặp lại danh sách' : 'Tắt lặp')); break; }
            case 'retry': loadLibrary(); break;
            case 'openembed': openEmbed(id); break;
            case 'embedback': state.embedId = null; nav(state.embedReturn || 'home'); break;
            case 'delembed': deleteEmbed(id); break;
            default: break;
        }
    });

    root.addEventListener('input', function (e) {
        var t = e.target;
        if (t.id === 'muSearch') { state.query = t.value; updateSearchResults(); }
        else if (t.id === 'muVolume') M.setVolume(Number(t.value));
        else if (t.id === 'muSeek') { M.seekRatio(Number(t.value) / 100); refreshTime(); }
    });
    root.addEventListener('pointerdown', function (e) { if (e.target.id === 'muSeek') seeking = true; });
    ['pointerup', 'pointercancel', 'change'].forEach(function (name) {
        root.addEventListener(name, function (e) { if (e.target.id === 'muSeek') seeking = false; });
    });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeSheet(); });

    M.on('track', function () {
        if (!state.loaded) return;
        if (state.view === 'player') { if (M.current()) render(); else nav('home'); }
        else { markPlaying(); }
    });
    M.on('state', refreshState);
    M.on('time', refreshTime);
    M.on('duration', refreshDurations);
    M.on('openplayer', function () { if (M.current()) { state.playerReturn = state.view === 'player' ? state.playerReturn : state.view; nav('player'); } });
    M.on('blocked', function () { toast('Trình duyệt đang chặn âm thanh. Hãy bấm Phát để tiếp tục.'); });
    M.on('error', function (message) { toast(message, true); });
    window.addEventListener('pagehide', function () { M.hideDock(false); });

    /* ---------------------------------------------------------------- tải thư viện */

    function loadLibrary() {
        state.loaded = false;
        state.error = '';
        render();
        // Danh sách nhạc trực tuyến tải song song; lỗi thì chỉ ẩn phần này, không chặn thư viện MP3
        var online = fetch(M.ctx + '/api/music/embeds', { headers: { Accept: 'application/json' } })
            .then(function (r) { return r.ok && !r.redirected ? r.json() : []; })
            .then(function (list) { state.embeds = Array.isArray(list) ? list : []; })
            .catch(function () { state.embeds = []; });
        return Promise.all([M.loadLibrary(true), online]).then(function (r) {
            state.tracks = r[0];
        }).catch(function (e) {
            state.tracks = [];
            state.error = (e && e.message) || 'Không tải được thư viện nhạc.';
        }).then(function () {
            state.loaded = true;
            render();
        });
    }

    /* ---------------------------------------------------------------- thêm nhạc (chỉ quản trị viên có quyền Nhạc) */

    var dialog = $('musicImport');
    if (dialog) {
        var inputs = [$('musicFiles'), $('musicFolder')];
        var linkBox = $('musicLinks');
        var linkGo = $('musicLinkGo');
        var controls = inputs.concat([linkBox, linkGo]);
        var statusEl = $('musicImportStatus');
        var results = $('musicImportResults');
        var busy = false;
        $('musicOpenImport').addEventListener('click', function () { dialog.showModal(); });

        // Đưa một file nhạc lên thư viện chung (dùng chung cho chọn file, chọn thư mục và dán link)
        var sendToLibrary = async function (file) {
            var body = new FormData();
            body.append('file', file);
            var response = await fetch(M.ctx + '/api/music/tracks', { method: 'POST', body: body });
            if (!response.ok || response.redirected) {
                if (response.status === 401 || response.status === 403 || response.redirected) {
                    throw new Error('Phiên đăng nhập hoặc quyền tải lên đã thay đổi.');
                }
                throw new Error(response.status === 400 || response.status === 413
                    ? 'File nhạc không hợp lệ hoặc vượt giới hạn 45 MB.' : 'Máy chủ chưa lưu được bài này.');
            }
        };

        // Link Spotify / YouTube / SoundCloud: lưu vào thư viện dạng nhúng khung phát; trả về dòng kết quả
        var addEmbed = async function (link) {
            var title = await MusicLink.fetchTitle(link);
            var response = await fetch(M.ctx + '/api/music/embeds', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify({ url: link, title: title })
            });
            var data = null;
            try { data = await response.json(); } catch (e) { /* không phải JSON */ }
            if (!response.ok || response.redirected) {
                if (response.status === 401 || response.redirected) throw new Error('Phiên đăng nhập đã hết hạn.');
                if (response.status === 403) throw new Error('Bạn không có quyền thêm nhạc.');
                throw new Error((data && data.message) || 'Máy chủ chưa lưu được link này.');
            }
            return data.providerLabel + ' · ' + data.title + (response.status === 201 ? ' · Đã thêm (nghe trực tuyến)' : ' · Đã có sẵn trong thư viện');
        };

        // Chạy lần lượt từng việc; mỗi việc là { label, file } hoặc { label, link }. Trả về các việc bị lỗi.
        var runBatch = async function (jobs, skippedNote) {
            busy = true;
            results.replaceChildren();
            controls.forEach(function (c) { c.disabled = true; });
            var done = 0;
            var failedJobs = [];
            try {
                for (var i = 0; i < jobs.length; i++) {
                    var job = jobs[i];
                    var row = document.createElement('li');
                    try {
                        if (job.link && MusicLink.streamingProvider(job.link)) {
                            statusEl.textContent = 'Đang thêm ' + (i + 1) + '/' + jobs.length + ' (nghe trực tuyến)...';
                            row.textContent = await addEmbed(job.link);
                            results.append(row);
                            done++;
                            continue;
                        }
                        var file = job.file;
                        if (!file) {
                            statusEl.textContent = 'Đang tải ' + (i + 1) + '/' + jobs.length + ' từ link...';
                            file = await MusicLink.download(job.link);
                        }
                        await sendToLibrary(file);
                        row.textContent = (job.link ? file.name.replace(/\.[a-z0-9]+$/i, '') : job.label) + ' · Có trong thư viện';
                    } catch (error) {
                        failedJobs.push(job);
                        row.textContent = job.label + ' · ' + error.message;
                    }
                    results.append(row);
                    done++;
                    statusEl.textContent = done + '/' + jobs.length + ' đã xử lý.';
                }
                statusEl.textContent = (done - failedJobs.length) + ' bài có trong thư viện, ' + failedJobs.length + ' lỗi'
                    + (skippedNote ? ', ' + skippedNote : '') + '.';
                await loadLibrary();
            } finally {
                controls.forEach(function (c) { c.disabled = false; });
                busy = false;
            }
            return failedJobs;
        };

        var uploadFiles = async function (event) {
            var files = Array.from(event.target.files);
            if (!files.length || busy) return;
            // File nhạc trình duyệt phát được (server nhận diện lại định dạng thật theo nội dung)
            var eligible = files.filter(function (f) { return /\.(mp3|m4a|mp4|aac|ogg|oga|opus|wav|flac|webm|weba)$/i.test(f.name); });
            try {
                await runBatch(eligible.map(function (f) { return { label: f.name, file: f }; }),
                    (files.length - eligible.length) + ' file không phải file nhạc');
            } finally {
                inputs.forEach(function (input) { input.value = ''; });
            }
        };
        inputs.forEach(function (input) { input.addEventListener('change', uploadFiles); });

        // Dán link vào ô: tự tải ngay. Link lỗi được giữ lại trong ô để sửa / thử lại
        var uploadLinks = async function () {
            if (busy) return;
            var links = MusicLink.extractUrls(linkBox.value);
            if (!links.length) {
                statusEl.textContent = 'Chưa có link nào trong ô.';
                return;
            }
            var failed = await runBatch(links.map(function (u) { return { label: u, link: u }; }), '');
            linkBox.value = failed.map(function (j) { return j.link; }).join('\n');
        };
        linkGo.addEventListener('click', uploadLinks);
        linkBox.addEventListener('paste', function () { setTimeout(uploadLinks, 0); });
        linkBox.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); uploadLinks(); }
        });
    }

    /* ---------------------------------------------------------------- khởi động */

    render();
    M.ready.then(function () {
        return loadLibrary();
    }).then(function () {
        if (M.current() && M.isPlaying()) { /* đang nghe dở từ trang trước: ở lại trang chủ nhạc, thanh nhỏ hiện sẵn */ }
    }).catch(function (e) {
        console.error(e);
        state.loaded = true;
        state.error = 'Không xác định được tài khoản. Hãy tải lại trang hoặc đăng nhập lại.';
        render();
    });
})();
