/*
 * camera-capture.js - Chụp ảnh hiện trường cho thẻ ca (kiểu Timemark).
 *
 * Luồng: bấm nút camera trên thẻ ca -> mở camera toàn màn hình, trên khung hình trực tiếp có chữ
 * (giờ, tọa độ GPS, địa chỉ, mã thuê bao) -> bấm chụp thì chữ được in luôn vào ảnh -> xem lại ->
 * Lưu (gửi lên server) / Chia sẻ (Zalo...) / Tải về.
 *
 * Hai chế độ:
 *  - Gắn với ca: openCamera(ticketId) - in thêm mã thuê bao, có nút Lưu ảnh (gửi lên server, cần ticketCache, apiFetch(), loadTickets()).
 *  - Chụp nhanh (nút "+" ở thanh điều hướng): CameraCapture.openFree() - không gắn ca/tài khoản nào, không lưu lên server,
 *    chỉ có Chụp lại / Chia sẻ / Tải về. Giờ, tọa độ GPS và địa chỉ vẫn được in lên ảnh.
 * Font Awesome cho biểu tượng.
 * Camera và GPS chỉ chạy khi mở trang bằng HTTPS (hoặc localhost).
 */
(function () {
    'use strict';

    // ===================== CẤU HÌNH =====================
    var MAX_EDGE = 1600;            // cạnh dài tối đa của ảnh lưu (px)
    var JPEG_QUALITY = 0.85;
    var GOOD_ACCURACY_M = 50;       // GPS sai số <= ngưỡng này thì báo xanh
    var GEOCODE_MIN_MOVE_M = 30;    // chỉ dò lại địa chỉ khi đã di chuyển quá ngần này
    var GEOCODE_RETRY_MS = 5000;
    var GEOCODE_MAX_FAILS = 3;
    var SHADE_BOTTOM = true;        // làm tối nhẹ phía dưới ảnh cho chữ dễ đọc trên nền sáng (đặt false để bỏ)

    var ui = null;      // các phần tử giao diện (tạo một lần)
    var state = null;   // trạng thái của phiên chụp đang mở

    // ===================== HÀM THUẦN (định dạng, tính toán) =====================
    function pad(n) { return (n < 10 ? '0' : '') + n; }

    // 17/10/26 09:43
    function formatTime(d) {
        return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + String(d.getFullYear()).slice(-2) +
            ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    // 20261017_094312 - dùng đặt tên file
    function formatStamp(d) {
        return d.getFullYear() + pad(d.getMonth() + 1) + pad(d.getDate()) + '_' +
            pad(d.getHours()) + pad(d.getMinutes()) + pad(d.getSeconds());
    }

    // 12.926 -> N 12° 55' 34"
    function toDMS(value, posChar, negChar) {
        var hemi = value >= 0 ? posChar : negChar;
        var abs = Math.abs(value);
        var deg = Math.floor(abs);
        var minFloat = (abs - deg) * 60;
        var min = Math.floor(minFloat);
        var sec = Math.round((minFloat - min) * 60);
        if (sec === 60) { sec = 0; min += 1; }
        if (min === 60) { min = 0; deg += 1; }
        return hemi + ' ' + deg + '\u00B0 ' + min + "' " + sec + '"';
    }

    function formatCoords(lat, lng) {
        return toDMS(lat, 'N', 'S') + ', ' + toDMS(lng, 'E', 'W');
    }

    function distanceM(lat1, lng1, lat2, lng2) {
        var R = 6371000, rad = Math.PI / 180;
        var dLat = (lat2 - lat1) * rad, dLng = (lng2 - lng1) * rad;
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Ghép địa chỉ từ kết quả OpenStreetMap Nominatim (bỏ quốc gia và mã bưu chính)
    function composeAddress(data) {
        if (!data) return '';
        var a = data.address || {};
        var street = [a.house_number, a.road].filter(Boolean).join(' ');
        var parts = [
            street,
            a.quarter || a.neighbourhood || a.hamlet,
            a.suburb || a.village,
            a.city_district || a.town || a.municipality || a.city,
            a.county,
            a.state
        ].filter(Boolean);
        var out = [];
        parts.forEach(function (p) { if (out.indexOf(p) === -1) out.push(p); });
        if (out.length) return out.join(', ');

        return String(data.display_name || '').split(',')
            .map(function (s) { return s.trim(); })
            .filter(function (s) { return s && s !== 'Việt Nam' && !/^\d{5,6}$/.test(s); })
            .join(', ');
    }

    // Ngắt một đoạn chữ thành nhiều dòng sao cho mỗi dòng không rộng quá maxW
    function wrapText(ctx, text, maxW) {
        var words = String(text).split(/\s+/);
        var lines = [], cur = '';
        words.forEach(function (w) {
            var test = cur ? cur + ' ' + w : w;
            if (cur && ctx.measureText(test).width > maxW) { lines.push(cur); cur = w; }
            else { cur = test; }
        });
        if (cur) lines.push(cur);
        return lines;
    }

    function safeName(s) { return String(s || 'anh').replace(/[^A-Za-z0-9_\-]/g, '_'); }

    // ===================== NỘI DUNG CHỮ IN LÊN ẢNH =====================
    function currentAddress(s) {
        if (s.geoAddress) return { text: s.geoAddress, source: 'GEOCODE' };
        var t = String(s.ticket.installAddress || '').trim();
        if (t) return { text: t, source: 'TICKET' };
        return { text: '', source: null };
    }

    function accountOf(ticket) { return ticket.subscriberAccount || ticket.ticketCode || ''; }

    function buildLines(ticket, when, pos, address) {
        var lines = [{ text: formatTime(when) }];
        if (pos) lines.push({ text: formatCoords(pos.lat, pos.lng) });
        if (address && address.text) lines.push({ text: address.text, wrap: true });
        var acc = accountOf(ticket);
        if (acc) lines.push({ text: String(acc) });
        return lines;
    }

    // Vẽ khối chữ ở góc phải dưới. Mọi kích thước tính theo bề rộng ảnh nên khung xem trước và ảnh thật giống nhau.
    function drawOverlay(ctx, W, H, lines) {
        var fs = Math.max(11, Math.round(W * 0.036));
        var lh = Math.round(fs * 1.3);
        var margin = Math.round(W * 0.03);
        var maxW = W - margin * 2;

        ctx.save();
        ctx.font = '500 ' + fs + 'px "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif';
        ctx.textAlign = 'right';
        ctx.textBaseline = 'alphabetic';

        var out = [];
        lines.forEach(function (l) {
            if (l.wrap) wrapText(ctx, l.text, maxW).forEach(function (t) { out.push(t); });
            else out.push(l.text);
        });

        if (SHADE_BOTTOM) {
            var shadeH = (out.length * lh + margin * 2) * 1.3;
            var g = ctx.createLinearGradient(0, H - shadeH, 0, H);
            g.addColorStop(0, 'rgba(0,0,0,0)');
            g.addColorStop(1, 'rgba(0,0,0,0.45)');
            ctx.fillStyle = g;
            ctx.fillRect(0, H - shadeH, W, shadeH);
        }

        ctx.fillStyle = '#fff';
        ctx.shadowColor = 'rgba(0,0,0,0.8)';
        ctx.shadowBlur = Math.max(2, fs * 0.14);
        ctx.shadowOffsetX = 0;
        ctx.shadowOffsetY = Math.max(1, fs * 0.05);

        var y = H - margin;
        for (var i = out.length - 1; i >= 0; i--) {
            ctx.fillText(out[i], W - margin, y);
            y -= lh;
        }
        ctx.restore();
    }

    // ===================== GIAO DIỆN =====================
    var CSS = [
        '#ccRoot{position:fixed;inset:0;z-index:2000;background:#000;color:#fff;display:none;flex-direction:column;height:100vh;height:100dvh;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif}',
        '#ccRoot.cc-open{display:flex}',
        '#ccRoot button{font-family:inherit;cursor:pointer}',
        '.cc-top{display:flex;align-items:center;gap:10px;padding:10px 12px;padding-top:calc(10px + env(safe-area-inset-top));min-height:56px}',
        '.cc-icon-btn{width:40px;height:40px;border-radius:50%;border:0;background:rgba(255,255,255,.14);color:#fff;font-size:1.05rem;display:flex;align-items:center;justify-content:center;flex:0 0 auto}',
        '.cc-icon-btn:active{background:rgba(255,255,255,.28)}',
        '.cc-title{flex:1;min-width:0}',
        '.cc-title b{display:block;font-size:.95rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}',
        '.cc-title span{display:block;font-size:.75rem;color:#cbd5e1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}',
        '.cc-area{flex:1;position:relative;display:flex;align-items:center;justify-content:center;min-height:0;overflow:hidden}',
        '.cc-stage{position:relative;background:#111;overflow:hidden}',
        '.cc-video,.cc-photo{position:absolute;inset:0;width:100%;height:100%;display:block}',
        '.cc-overlay{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}',
        '.cc-flash{position:absolute;inset:0;background:#fff;opacity:0;pointer-events:none}',
        '.cc-flash.go{animation:ccflash .18s ease-out}',
        '@keyframes ccflash{from{opacity:.85}to{opacity:0}}',
        '.cc-chips{position:absolute;left:10px;top:8px;display:flex;flex-direction:column;gap:6px;align-items:flex-start;z-index:2;pointer-events:none}',
        '.cc-chip{display:inline-flex;align-items:center;gap:6px;background:rgba(0,0,0,.55);color:#fff;border-radius:999px;padding:4px 10px;font-size:.74rem;font-weight:600}',
        '.cc-dot{width:8px;height:8px;border-radius:50%;background:#94a3b8;display:inline-block}',
        '.cc-dot.ok{background:#22c55e}.cc-dot.warn{background:#facc15}.cc-dot.bad{background:#ef4444}',
        '.cc-fallback{display:none;position:absolute;inset:0;padding:24px;text-align:center;flex-direction:column;align-items:center;justify-content:center;gap:14px;color:#e2e8f0;font-size:.92rem}',
        '.cc-fallback i.big{font-size:2rem;color:#facc15}',
        '.cc-bottom{padding:14px 16px;padding-bottom:calc(14px + env(safe-area-inset-bottom));display:flex;align-items:center;justify-content:center;min-height:104px}',
        '.cc-live,.cc-review{display:flex;align-items:center;justify-content:space-between;width:100%;max-width:520px;gap:10px}',
        '.cc-shutter{width:74px;height:74px;border-radius:50%;border:4px solid #fff;background:transparent;padding:0;display:flex;align-items:center;justify-content:center}',
        '.cc-shutter::after{content:"";width:56px;height:56px;border-radius:50%;background:#fff;transition:transform .08s}',
        '.cc-shutter:active::after{transform:scale(.9)}',
        '.cc-shutter:disabled{opacity:.35}',
        '.cc-act{flex:1;border:0;border-radius:12px;padding:12px 8px;font-weight:600;font-size:.88rem;color:#fff;background:rgba(255,255,255,.16);display:flex;flex-direction:column;align-items:center;gap:4px}',
        '.cc-act i{font-size:1.05rem}',
        '.cc-act.primary{background:#2563eb}',
        '.cc-act:disabled{opacity:.5}',
        '.cc-toast{position:absolute;left:50%;bottom:116px;transform:translateX(-50%);background:rgba(15,23,42,.92);color:#fff;border-radius:10px;padding:8px 14px;font-size:.84rem;max-width:88%;text-align:center;display:none;z-index:3}'
    ].join('\n');

    var HTML = [
        '<div class="cc-top">',
        '  <button type="button" class="cc-icon-btn" data-cc="close" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button>',
        '  <div class="cc-title"><b data-cc="title"></b><span data-cc="sub"></span></div>',
        '  <button type="button" class="cc-icon-btn" data-cc="switch" aria-label="Đổi camera" style="display:none"><i class="fa-solid fa-camera-rotate"></i></button>',
        '</div>',
        '<div class="cc-area">',
        '  <div class="cc-chips">',
        '    <span class="cc-chip"><i class="cc-dot" data-cc="gpsDot"></i><span data-cc="gpsText"></span></span>',
        '    <span class="cc-chip"><i class="cc-dot" data-cc="addrDot"></i><span data-cc="addrText"></span></span>',
        '  </div>',
        '  <div class="cc-stage" data-cc="stage">',
        '    <video class="cc-video" data-cc="video" playsinline muted autoplay></video>',
        '    <img class="cc-photo" data-cc="photo" alt="Ảnh vừa chụp" style="display:none">',
        '    <canvas class="cc-overlay" data-cc="overlay"></canvas>',
        '    <div class="cc-flash" data-cc="flash"></div>',
        '  </div>',
        '  <div class="cc-fallback" data-cc="fallback">',
        '    <i class="fa-solid fa-triangle-exclamation big"></i>',
        '    <div data-cc="fallbackMsg"></div>',
        '    <button type="button" class="cc-act primary" data-cc="pickFallback" style="flex:none;padding:12px 18px"><i class="fa-solid fa-image"></i><span>Chụp bằng ứng dụng máy ảnh</span></button>',
        '  </div>',
        '</div>',
        '<div class="cc-toast" data-cc="toast"></div>',
        '<div class="cc-bottom">',
        '  <div class="cc-live" data-cc="live">',
        '    <button type="button" class="cc-icon-btn" data-cc="pick" aria-label="Dùng ứng dụng máy ảnh của thiết bị" title="Dùng ứng dụng máy ảnh của thiết bị"><i class="fa-solid fa-image"></i></button>',
        '    <button type="button" class="cc-shutter" data-cc="shutter" aria-label="Chụp ảnh" disabled></button>',
        '    <span style="width:40px;flex:0 0 40px"></span>',
        '  </div>',
        '  <div class="cc-review" data-cc="review" style="display:none">',
        '    <button type="button" class="cc-act" data-cc="retake"><i class="fa-solid fa-rotate-left"></i><span>Chụp lại</span></button>',
        '    <button type="button" class="cc-act primary" data-cc="save"><i class="fa-solid fa-floppy-disk"></i><span>Lưu ảnh</span></button>',
        '    <button type="button" class="cc-act" data-cc="share"><i class="fa-solid fa-share-nodes"></i><span>Chia sẻ</span></button>',
        '    <button type="button" class="cc-act" data-cc="download"><i class="fa-solid fa-download"></i><span>Tải về</span></button>',
        '  </div>',
        '</div>',
        '<input type="file" accept="image/*" capture="environment" style="display:none" data-cc="file">'
    ].join('');

    function ensureUi() {
        if (ui) return;
        var style = document.createElement('style');
        style.id = 'ccStyle';
        style.textContent = CSS;
        document.head.appendChild(style);

        var el = document.createElement('div');
        el.id = 'ccRoot';
        el.setAttribute('role', 'dialog');
        el.setAttribute('aria-modal', 'true');
        el.setAttribute('aria-label', 'Chụp ảnh hiện trường');
        el.innerHTML = HTML;
        document.body.appendChild(el);

        function q(name) { return el.querySelector('[data-cc="' + name + '"]'); }
        ui = {
            root: el, area: el.querySelector('.cc-area'),
            stage: q('stage'), video: q('video'), photo: q('photo'), overlay: q('overlay'), flash: q('flash'),
            fallback: q('fallback'), fallbackMsg: q('fallbackMsg'),
            title: q('title'), sub: q('sub'),
            gpsDot: q('gpsDot'), gpsText: q('gpsText'), addrDot: q('addrDot'), addrText: q('addrText'),
            live: q('live'), review: q('review'),
            shutter: q('shutter'), switchBtn: q('switch'), file: q('file'),
            btnRetake: q('retake'), btnSave: q('save'), btnShare: q('share'), btnDownload: q('download'),
            toastEl: q('toast'), toastTimer: null
        };
        ui.video.muted = true;

        q('close').addEventListener('click', closeCamera);
        ui.shutter.addEventListener('click', capture);
        ui.switchBtn.addEventListener('click', switchCamera);
        q('pick').addEventListener('click', function () { ui.file.click(); });
        q('pickFallback').addEventListener('click', function () { ui.file.click(); });
        ui.file.addEventListener('change', onFilePicked);
        ui.btnRetake.addEventListener('click', retake);
        ui.btnSave.addEventListener('click', save);
        ui.btnShare.addEventListener('click', share);
        ui.btnDownload.addEventListener('click', download);
        ui.video.addEventListener('loadedmetadata', onVideoSize);
        ui.video.addEventListener('resize', onVideoSize);
        window.addEventListener('resize', layoutStage);
        window.addEventListener('orientationchange', function () { setTimeout(layoutStage, 250); });
        document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && state) closeCamera(); });
    }

    function toast(msg, ms) {
        ui.toastEl.textContent = msg;
        ui.toastEl.style.display = 'block';
        clearTimeout(ui.toastTimer);
        ui.toastTimer = setTimeout(function () { ui.toastEl.style.display = 'none'; }, ms || 2500);
    }

    function btnLabel(btn, text) { btn.querySelector('span').textContent = text; }

    function setMode(mode) {
        state.mode = mode;
        var live = mode === 'live';
        var showFallback = live && state.noCamera;
        ui.live.style.display = live ? 'flex' : 'none';
        ui.review.style.display = live ? 'none' : 'flex';
        ui.video.style.display = live ? 'block' : 'none';
        ui.overlay.style.display = live ? 'block' : 'none';
        ui.photo.style.display = live ? 'none' : 'block';
        ui.fallback.style.display = showFallback ? 'flex' : 'none';
        ui.stage.style.display = showFallback ? 'none' : 'block';
    }

    // Khung hình luôn đúng tỉ lệ của ảnh để xem trước = ảnh chụp thật
    function layoutStage() {
        if (!state) return;
        var aw = ui.area.clientWidth, ah = ui.area.clientHeight;
        if (!aw || !ah) return;
        var ratio = state.ratio || 3 / 4;
        var w = aw, h = aw / ratio;
        if (h > ah) { h = ah; w = ah * ratio; }
        ui.stage.style.width = Math.floor(w) + 'px';
        ui.stage.style.height = Math.floor(h) + 'px';
        redrawPreview();
    }

    function redrawPreview() {
        if (!state || state.mode !== 'live') return;
        var w = ui.stage.clientWidth, h = ui.stage.clientHeight;
        if (!w || !h) return;
        var dpr = window.devicePixelRatio || 1;
        var cw = Math.round(w * dpr), ch = Math.round(h * dpr);
        if (ui.overlay.width !== cw || ui.overlay.height !== ch) { ui.overlay.width = cw; ui.overlay.height = ch; }
        var ctx = ui.overlay.getContext('2d');
        ctx.clearRect(0, 0, cw, ch);
        drawOverlay(ctx, cw, ch, buildLines(state.ticket, new Date(), state.pos, currentAddress(state)));
    }

    // ===================== TRẠNG THÁI GPS / ĐỊA CHỈ =====================
    function setChip(dot, textEl, level, text) {
        dot.className = 'cc-dot' + (level ? ' ' + level : '');
        textEl.textContent = text;
        dot.parentNode.style.display = text ? 'inline-flex' : 'none';
    }

    function updateChips() {
        var s = state;
        if (!s) return;
        var g;
        switch (s.gpsStatus) {
            case 'ok':
                var acc = Math.round(s.pos.acc);
                g = acc <= GOOD_ACCURACY_M ? { level: 'ok', text: 'GPS ±' + acc + ' m' } : { level: 'warn', text: 'GPS yếu ±' + acc + ' m' };
                break;
            case 'denied': g = { level: 'bad', text: 'Chưa cho phép dùng vị trí' }; break;
            case 'insecure': g = { level: 'bad', text: 'GPS cần mở trang bằng HTTPS' }; break;
            case 'unavailable':
            case 'error': g = { level: 'bad', text: 'Không lấy được GPS' }; break;
            default: g = { level: '', text: 'Đang lấy GPS…' };
        }
        setChip(ui.gpsDot, ui.gpsText, g.level, g.text);

        var a;
        if (s.geoAddress) a = { level: 'ok', text: 'Địa chỉ theo GPS' };
        else if (s.geoBusy && s.pos) a = { level: '', text: 'Đang dò địa chỉ…' };
        else if (String(s.ticket.installAddress || '').trim()) a = { level: 'warn', text: 'Địa chỉ theo thông tin ca' };
        else a = { level: 'bad', text: 'Chưa có địa chỉ' };
        setChip(ui.addrDot, ui.addrText, a.level, a.text);
    }

    // Chỉ cho chụp khi đã có camera và GPS đã có kết quả (có tọa độ hoặc đã báo lỗi rõ ràng)
    function updateShutter() {
        var s = state;
        if (!s) return;
        var ready = !!(s.stream && s.videoRatio) && s.gpsStatus !== 'waiting' && s.mode === 'live' && !s.busy;
        ui.shutter.disabled = !ready;
        ui.shutter.title = ready ? 'Chụp ảnh' : (s.gpsStatus === 'waiting' ? 'Đang chờ GPS…' : 'Đang bật camera…');
    }

    function startGps() {
        var s = state;
        if (!window.isSecureContext) { s.gpsStatus = 'insecure'; return; }
        if (!navigator.geolocation) { s.gpsStatus = 'unavailable'; return; }
        s.watchId = navigator.geolocation.watchPosition(function (p) {
            if (state !== s) return;
            s.pos = { lat: p.coords.latitude, lng: p.coords.longitude, acc: p.coords.accuracy || 0 };
            s.gpsStatus = 'ok';
            maybeGeocode();
            updateChips(); updateShutter(); redrawPreview();
        }, function (err) {
            if (state !== s) return;
            if (!s.pos) s.gpsStatus = (err && err.code === 1) ? 'denied' : 'error';
            else if (err && err.code === 1) { s.gpsStatus = 'denied'; s.pos = null; }
            updateChips(); updateShutter(); redrawPreview();
        }, { enableHighAccuracy: true, maximumAge: 5000, timeout: 20000 });
    }

    // Dò địa chỉ từ tọa độ bằng OpenStreetMap Nominatim (miễn phí, cần giữ tần suất thấp)
    function reverseGeocode(lat, lng) {
        var url = 'https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&addressdetails=1&accept-language=vi' +
            '&lat=' + encodeURIComponent(lat) + '&lon=' + encodeURIComponent(lng);
        var ctrl = (typeof AbortController !== 'undefined') ? new AbortController() : null;
        var timer = setTimeout(function () { if (ctrl) ctrl.abort(); }, 8000);
        return fetch(url, { headers: { 'Accept': 'application/json' }, signal: ctrl ? ctrl.signal : undefined })
            .then(function (r) { if (!r.ok) throw new Error('geocode ' + r.status); return r.json(); })
            .then(composeAddress)
            .then(function (t) { clearTimeout(timer); return t; }, function (e) { clearTimeout(timer); throw e; });
    }

    function maybeGeocode() {
        var s = state;
        if (!s || !s.pos || s.geoBusy || s.geoFails >= GEOCODE_MAX_FAILS) return;
        var now = Date.now();
        if (now - s.geoLastTry < GEOCODE_RETRY_MS) return;
        if (s.geoAddress && s.geoFrom &&
            distanceM(s.geoFrom.lat, s.geoFrom.lng, s.pos.lat, s.pos.lng) < GEOCODE_MIN_MOVE_M) return;

        var from = { lat: s.pos.lat, lng: s.pos.lng };
        s.geoBusy = true;
        s.geoLastTry = now;
        reverseGeocode(from.lat, from.lng).then(function (text) {
            if (state !== s) return;
            if (text) { s.geoAddress = text; s.geoFrom = from; s.geoFails = 0; }
            else { s.geoFails++; }
        }, function () {
            if (state === s) s.geoFails++;
        }).then(function () {
            if (state !== s) return;
            s.geoBusy = false;
            updateChips(); redrawPreview();
        });
    }

    // ===================== CAMERA =====================
    function stopTracks(stream) { stream.getTracks().forEach(function (t) { t.stop(); }); }
    function stopStream(s) { if (s.stream) { stopTracks(s.stream); s.stream = null; } }

    function describeCameraError(err) {
        var n = err && err.name;
        if (n === 'NotAllowedError' || n === 'SecurityError') return 'Bạn chưa cho phép dùng camera. Hãy bật quyền camera cho trang này rồi thử lại.';
        if (n === 'NotFoundError' || n === 'OverconstrainedError') return 'Không tìm thấy camera trên thiết bị này.';
        if (n === 'NotReadableError') return 'Camera đang được ứng dụng khác sử dụng. Hãy đóng ứng dụng đó rồi thử lại.';
        return 'Không bật được camera trực tiếp.';
    }

    function cameraUnavailable(s, msg) {
        if (state !== s) return;
        s.noCamera = true;
        ui.fallbackMsg.textContent = msg + ' Bạn vẫn có thể chụp bằng ứng dụng máy ảnh của thiết bị, giờ và vị trí sẽ được in lên ảnh.';
        setMode(s.mode);
        updateShutter();
    }

    function startCamera() {
        var s = state;
        stopStream(s);
        s.videoRatio = 0;
        if (!window.isSecureContext) {
            return cameraUnavailable(s, 'Camera chỉ bật được khi mở trang bằng HTTPS (hoặc localhost).');
        }
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            return cameraUnavailable(s, 'Trình duyệt này không hỗ trợ camera trực tiếp.');
        }
        navigator.mediaDevices.getUserMedia({
            audio: false,
            video: { facingMode: { ideal: s.facing }, width: { ideal: 1920 }, height: { ideal: 1080 } }
        }).then(function (stream) {
            if (state !== s) { stopTracks(stream); return; }
            s.noCamera = false;
            s.stream = stream;
            ui.video.srcObject = stream;
            setMode(s.mode);
            var p = ui.video.play();
            if (p && p.catch) p.catch(function () {});
            if (navigator.mediaDevices.enumerateDevices) {
                navigator.mediaDevices.enumerateDevices().then(function (list) {
                    if (state !== s) return;
                    var n = list.filter(function (d) { return d.kind === 'videoinput'; }).length;
                    ui.switchBtn.style.display = n > 1 ? 'flex' : 'none';
                }).catch(function () {});
            }
        }).catch(function (err) {
            cameraUnavailable(s, describeCameraError(err));
        });
    }

    function onVideoSize() {
        var s = state;
        if (!s) return;
        var vw = ui.video.videoWidth, vh = ui.video.videoHeight;
        if (!vw || !vh) return;
        s.videoRatio = vw / vh;
        if (s.mode === 'live') s.ratio = s.videoRatio;
        layoutStage();
        updateShutter();
    }

    function switchCamera() {
        var s = state;
        if (!s || s.mode !== 'live' || s.busy) return;
        s.facing = s.facing === 'environment' ? 'user' : 'environment';
        startCamera();
    }

    // ===================== CHỤP =====================
    function snapshot(s, when) {
        var pos = s.pos ? { lat: s.pos.lat, lng: s.pos.lng, acc: s.pos.acc } : null;
        var address = currentAddress(s);
        return { when: when, pos: pos, address: address, lines: buildLines(s.ticket, when, pos, address) };
    }

    function capture() {
        var s = state;
        if (!s || s.busy || s.mode !== 'live' || !s.stream) return;
        var vw = ui.video.videoWidth, vh = ui.video.videoHeight;
        if (!vw || !vh) return;
        ui.flash.classList.remove('go');
        void ui.flash.offsetWidth;
        ui.flash.classList.add('go');
        produce(s, ui.video, vw, vh);
    }

    // Vẽ khung hình (hoặc ảnh chọn từ máy) lên canvas, in chữ, rồi xuất JPEG
    function produce(s, source, sw, sh) {
        var scale = Math.min(1, MAX_EDGE / Math.max(sw, sh));
        var W = Math.round(sw * scale), H = Math.round(sh * scale);
        var canvas = document.createElement('canvas');
        canvas.width = W;
        canvas.height = H;
        var ctx = canvas.getContext('2d');
        ctx.drawImage(source, 0, 0, W, H);

        var snap = snapshot(s, new Date());
        drawOverlay(ctx, W, H, snap.lines);
        canvas.toBlob(function (blob) {
            if (!blob) { toast('Không tạo được ảnh, hãy thử lại.'); return; }
            showReview(s, blob, snap, W, H);
        }, 'image/jpeg', JPEG_QUALITY);
    }

    function makeFile(blob, name) {
        try { return new File([blob], name, { type: 'image/jpeg' }); }
        catch (e) { blob.name = name; return blob; }
    }

    function canShareFile(file) {
        try { return !!(navigator.canShare && navigator.share && navigator.canShare({ files: [file] })); }
        catch (e) { return false; }
    }

    function showReview(s, blob, snap, W, H) {
        if (state !== s) return;
        var name = safeName(accountOf(s.ticket)) + '_' + formatStamp(snap.when) + '.jpg';
        var c = {
            blob: blob, file: makeFile(blob, name), name: name, url: URL.createObjectURL(blob),
            pos: snap.pos, address: snap.address, saved: false
        };
        s.captured = c;
        s.ratio = W / H;
        ui.photo.src = c.url;
        setMode('review');
        btnLabel(ui.btnRetake, 'Chụp lại');
        btnLabel(ui.btnSave, 'Lưu ảnh');
        ui.btnSave.disabled = false;
        ui.btnSave.style.display = s.free ? 'none' : '';
        ui.btnShare.style.display = canShareFile(c.file) ? '' : 'none';
        layoutStage();
    }

    function onFilePicked() {
        var s = state;
        var f = ui.file.files && ui.file.files[0];
        ui.file.value = '';
        if (!s || !f) return;
        var url = URL.createObjectURL(f);
        var img = new Image();
        img.onload = function () {
            URL.revokeObjectURL(url);
            if (state !== s) return;
            produce(s, img, img.naturalWidth, img.naturalHeight);
        };
        img.onerror = function () {
            URL.revokeObjectURL(url);
            toast('Không đọc được ảnh vừa chọn.');
        };
        img.src = url;
    }

    function retake() {
        var s = state;
        if (!s || s.busy) return;
        if (s.captured) URL.revokeObjectURL(s.captured.url);
        s.captured = null;
        ui.photo.removeAttribute('src');
        s.ratio = s.videoRatio || 3 / 4;
        setMode('live');
        layoutStage();
        updateShutter();
        if (s.noCamera) ui.file.click();
    }

    // ===================== LƯU / CHIA SẺ / TẢI VỀ =====================
    function uploadPhoto(ticketId, fd) {
        var url = (typeof CTX !== 'undefined' ? CTX : '') + '/api/tickets/' + ticketId + '/photos';
        if (typeof apiFetch === 'function') {
            return apiFetch(url, { method: 'POST', body: fd });
        }
        return fetch(url, { method: 'POST', body: fd }).then(function (r) {
            if (!r.ok) throw new Error('Có lỗi xảy ra (mã ' + r.status + ')');
            return r.json();
        });
    }

    function updateSub() {
        var s = state;
        if (!s) return;
        var base = accountOf(s.ticket) || (s.free ? 'Ảnh không gắn với ca nào' : '');
        ui.sub.textContent = s.savedCount ? (base + ' • Đã lưu ' + s.savedCount + ' ảnh') : base;
    }

    function save() {
        var s = state, c = s && s.captured;
        if (!c || c.saved || s.busy) return;
        s.busy = true;
        ui.btnSave.disabled = true;
        btnLabel(ui.btnSave, 'Đang lưu…');

        var fd = new FormData();
        fd.append('file', c.file, c.name);
        if (c.pos) {
            fd.append('latitude', c.pos.lat);
            fd.append('longitude', c.pos.lng);
            fd.append('accuracy', Math.round(c.pos.acc * 10) / 10);
        }
        if (c.address && c.address.text) {
            fd.append('address', c.address.text);
            fd.append('addressSource', c.address.source);
        }

        uploadPhoto(s.ticketId, fd).then(function () {
            s.busy = false;
            c.saved = true;
            s.savedCount++;
            btnLabel(ui.btnSave, 'Đã lưu');
            btnLabel(ui.btnRetake, 'Chụp tiếp');
            updateSub();
            toast('Đã lưu ảnh');
        }, function (e) {
            s.busy = false;
            ui.btnSave.disabled = false;
            btnLabel(ui.btnSave, 'Lưu ảnh');
            toast((e && e.message) || 'Không lưu được ảnh, hãy thử lại.', 4000);
        });
    }

    function share() {
        var s = state, c = s && s.captured;
        if (!c) return;
        navigator.share({ files: [c.file], title: accountOf(s.ticket) || 'Ảnh hiện trường' }).then(function () {
            c.exported = true;
        }).catch(function (err) {
            if (err && err.name !== 'AbortError') toast('Không chia sẻ được ảnh, hãy dùng nút Tải về.', 3500);
        });
    }

    function download() {
        var c = state && state.captured;
        if (!c) return;
        var a = document.createElement('a');
        a.href = c.url;
        a.download = c.name;
        document.body.appendChild(a);
        a.click();
        a.remove();
        c.exported = true;
    }

    // ===================== MỞ / ĐÓNG =====================
    function tick() {
        if (!state) return;
        maybeGeocode();
        redrawPreview();
        updateChips();
    }

    // ticketId rỗng (null/undefined) = chụp nhanh, không gắn với ca nào
    function open(ticketId) {
        if (state) return;
        var free = (ticketId === null || ticketId === undefined);
        var ticket = free ? { customerName: 'Chụp ảnh nhanh' }
            : ((typeof ticketCache !== 'undefined' && ticketCache) ? ticketCache[ticketId] : null);
        if (!ticket) { alert('Không tìm thấy thông tin ca, hãy tải lại danh sách.'); return; }
        ensureUi();

        var s = state = {
            ticketId: ticketId, ticket: ticket, free: free, facing: 'environment', mode: 'live',
            stream: null, videoRatio: 0, ratio: 3 / 4, noCamera: false,
            pos: null, gpsStatus: 'waiting', watchId: null,
            geoAddress: '', geoFrom: null, geoBusy: false, geoLastTry: 0, geoFails: 0,
            captured: null, busy: false, savedCount: 0, timer: null
        };

        ui.title.textContent = ticket.customerName || 'Khách hàng';
        updateSub();
        ui.switchBtn.style.display = 'none';
        ui.root.classList.add('cc-open');
        document.body.style.overflow = 'hidden';
        setMode('live');
        layoutStage();

        startGps();
        startCamera();
        updateChips();
        updateShutter();
        s.timer = setInterval(tick, 1000);
    }

    function closeCamera() {
        var s = state;
        if (!s) return;
        if (s.busy) { toast('Đang lưu ảnh, vui lòng chờ…'); return; }
        // Chụp gắn ca: cảnh báo nếu chưa Lưu. Chụp nhanh: cảnh báo nếu chưa Tải về / Chia sẻ
        var unsaved = s.captured && (s.free ? !s.captured.exported : !s.captured.saved);
        if (unsaved && !confirm(s.free ? 'Ảnh vừa chụp chưa được tải về hoặc chia sẻ. Bỏ ảnh này?' : 'Ảnh vừa chụp chưa được lưu. Bỏ ảnh này?')) return;

        clearInterval(s.timer);
        if (s.watchId !== null && navigator.geolocation) navigator.geolocation.clearWatch(s.watchId);
        stopStream(s);
        ui.video.srcObject = null;
        if (s.captured) URL.revokeObjectURL(s.captured.url);
        ui.photo.removeAttribute('src');
        ui.root.classList.remove('cc-open');
        document.body.style.overflow = '';
        state = null;

        // Có ảnh mới lưu thì tải lại danh sách để số ảnh trên nút camera được cập nhật
        if (s.savedCount > 0 && typeof loadTickets === 'function') loadTickets(true);
    }

    window.openCamera = open;
    window.CameraCapture = {
        open: open,
        openFree: function () { open(null); },
        close: closeCamera,
        _test: {
            toDMS: toDMS, formatCoords: formatCoords, formatTime: formatTime, formatStamp: formatStamp,
            composeAddress: composeAddress, wrapText: wrapText, distanceM: distanceM,
            buildLines: buildLines, safeName: safeName
        }
    };
})();
