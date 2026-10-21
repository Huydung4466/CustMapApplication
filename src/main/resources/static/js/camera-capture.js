/*
 * camera-capture.js - Chụp ảnh hiện trường bằng CAMERA GỐC CỦA ĐIỆN THOẠI rồi in thông tin lên ảnh (kiểu Timemark).
 *
 * Vì sao không dùng camera của trình duyệt: ảnh lấy từ khung hình trình duyệt bị mờ, thiếu các xử lý của ứng dụng máy ảnh
 * (lấy nét, HDR, chống rung...). Cách làm hiện tại: bấm "Chụp ảnh" -> mở ứng dụng máy ảnh của thiết bị -> chụp xong ảnh
 * quay về trang -> trang in thông tin (giờ, tọa độ, địa chỉ, mã thuê bao...) lên ảnh gốc -> xem lại ->
 * Lưu (gửi lên server) / Chia sẻ (Zalo...) / Tải về.
 *
 * Mục nào được in lên ảnh do ADMIN bật/tắt (trang Cá nhân > Cấu hình camera), lấy qua GET /api/settings/camera.
 *
 * Hai chế độ:
 *  - Gắn với ca: openCamera(ticketId) - in thêm thông tin của ca, có nút Lưu ảnh (gửi lên server, cần ticketCache, apiFetch(), loadTickets()).
 *  - Chụp nhanh (nút "+" ở thanh điều hướng): CameraCapture.openFree() - không gắn ca/tài khoản nào, không lưu lên server,
 *    chỉ có Chụp lại / Chia sẻ / Tải về. Giờ, tọa độ GPS và địa chỉ vẫn được in lên ảnh.
 *
 * GPS chỉ chạy khi mở trang bằng HTTPS (hoặc localhost). Font Awesome cho biểu tượng.
 */
(function () {
    'use strict';

    // ===================== CẤU HÌNH =====================
    var MAX_EDGE = 2560;            // cạnh dài tối đa của ảnh lưu (px); ảnh nhỏ hơn thì giữ nguyên
    var JPEG_QUALITY = 0.9;
    var MAX_BYTES = 7 * 1024 * 1024;        // server nhận tối đa 8 MB; nếu vượt thì nén thêm
    var MAX_PHOTO_AGE_MS = 30 * 60 * 1000;  // chỉ nhận ảnh chụp trong vòng 30 phút (tránh chọn nhầm ảnh cũ trong thư viện)
    var GOOD_ACCURACY_M = 50;       // GPS sai số <= ngưỡng này thì báo xanh
    var GEOCODE_MIN_MOVE_M = 30;    // chỉ dò lại địa chỉ khi đã di chuyển quá ngần này
    var GEOCODE_RETRY_MS = 5000;
    var GEOCODE_MAX_FAILS = 3;
    var SHADE_BOTTOM = true;        // làm tối nhẹ phía dưới ảnh cho chữ dễ đọc trên nền sáng (đặt false để bỏ)

    // Mục in lên ảnh: mặc định khi chưa tải được cấu hình từ server (khớp mặc định phía server)
    var DEFAULT_CFG = {
        showTime: true, showCoordinates: true, showAddress: true,
        showCustomerName: false, showAccount: true, showCabinetCode: false, showStaff: false
    };
    var cfg = Object.assign({}, DEFAULT_CFG);
    var staffName = '';
    var staffLoaded = false;

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

    // Chỉ in những mục ADMIN đã bật; thứ tự: giờ, tọa độ, địa chỉ, tên KH, mã thuê bao, mã tủ, nhân viên
    function buildLines(ticket, when, pos, address, conf, staff) {
        conf = conf || DEFAULT_CFG;
        var lines = [];
        if (conf.showTime) lines.push({ text: formatTime(when) });
        if (conf.showCoordinates && pos) lines.push({ text: formatCoords(pos.lat, pos.lng) });
        if (conf.showAddress && address && address.text) lines.push({ text: address.text, wrap: true });
        if (conf.showCustomerName && ticket.customerName) lines.push({ text: String(ticket.customerName) });
        var acc = accountOf(ticket);
        if (conf.showAccount && acc) lines.push({ text: String(acc) });
        if (conf.showCabinetCode && ticket.cabinetCode) lines.push({ text: 'Tủ: ' + ticket.cabinetCode });
        if (conf.showStaff && staff) lines.push({ text: 'NV: ' + staff });
        return lines;
    }

    // Vẽ khối chữ ở góc phải dưới. Mọi kích thước tính theo bề rộng ảnh nên khung xem trước và ảnh thật giống nhau.
    function drawOverlay(ctx, W, H, lines) {
        var fs = Math.max(11, Math.round(W * 0.036));
        var lh = Math.round(fs * 1.3);
        var margin = Math.round(W * 0.03);
        var maxW = W - margin * 2;

        if (!lines.length) return;
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
        '.cc-photo{position:absolute;inset:0;width:100%;height:100%;display:block}',
        '.cc-chips{position:absolute;left:10px;top:8px;display:flex;flex-direction:column;gap:6px;align-items:flex-start;z-index:2;pointer-events:none}',
        '.cc-chip{display:inline-flex;align-items:center;gap:6px;background:rgba(255,255,255,.14);color:#fff;border-radius:999px;padding:4px 10px;font-size:.74rem;font-weight:600}',
        '.cc-dot{width:8px;height:8px;border-radius:50%;background:#94a3b8;display:inline-block}',
        '.cc-dot.ok{background:#22c55e}.cc-dot.warn{background:#facc15}.cc-dot.bad{background:#ef4444}',
        '.cc-ready{position:absolute;inset:0;padding:64px 22px 16px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;text-align:center;overflow:auto}',
        '.cc-ready-icon{width:84px;height:84px;border-radius:50%;background:rgba(255,255,255,.12);display:flex;align-items:center;justify-content:center;font-size:2.1rem}',
        '.cc-ready-title{font-weight:700;font-size:1.05rem}',
        '.cc-ready-hint{font-size:.84rem;color:#cbd5e1;max-width:360px}',
        '.cc-preview{width:100%;max-width:380px;margin-top:6px;padding:12px 14px;border-radius:12px;background:rgba(255,255,255,.08);text-align:right;font-size:.86rem;line-height:1.45}',
        '.cc-preview small{display:block;text-align:left;color:#94a3b8;font-size:.72rem;margin-bottom:6px}',
        '.cc-preview div.l{word-break:break-word}',
        '.cc-bottom{padding:14px 16px;padding-bottom:calc(14px + env(safe-area-inset-bottom));display:flex;align-items:center;justify-content:center;min-height:104px}',
        '.cc-live,.cc-review{display:flex;align-items:center;justify-content:center;width:100%;max-width:520px;gap:10px}',
        '.cc-main{flex:1;max-width:420px;border:0;border-radius:14px;background:#2563eb;color:#fff;padding:16px 12px;font-size:1.05rem;font-weight:700;display:flex;align-items:center;justify-content:center;gap:10px}',
        '.cc-main:active{filter:brightness(.92)}',
        '.cc-main:disabled{opacity:.45}',
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
        '  <span style="width:40px;flex:0 0 40px"></span>',
        '</div>',
        '<div class="cc-area">',
        '  <div class="cc-chips" data-cc="chips">',
        '    <span class="cc-chip"><i class="cc-dot" data-cc="gpsDot"></i><span data-cc="gpsText"></span></span>',
        '    <span class="cc-chip"><i class="cc-dot" data-cc="addrDot"></i><span data-cc="addrText"></span></span>',
        '  </div>',
        '  <div class="cc-ready" data-cc="ready">',
        '    <div class="cc-ready-icon"><i class="fa-solid fa-camera"></i></div>',
        '    <div class="cc-ready-title">Chụp bằng camera của điện thoại</div>',
        '    <div class="cc-ready-hint">Ảnh nét như khi chụp bằng ứng dụng máy ảnh. Thông tin bên dưới sẽ được in lên ảnh sau khi chụp.</div>',
        '    <div class="cc-preview"><small>Sẽ in lên ảnh:</small><div data-cc="preview"></div></div>',
        '  </div>',
        '  <div class="cc-stage" data-cc="stage" style="display:none">',
        '    <img class="cc-photo" data-cc="photo" alt="Ảnh vừa chụp">',
        '  </div>',
        '</div>',
        '<div class="cc-toast" data-cc="toast"></div>',
        '<div class="cc-bottom">',
        '  <div class="cc-live" data-cc="live">',
        '    <button type="button" class="cc-main" data-cc="shoot" disabled><i class="fa-solid fa-camera"></i><span>Chụp ảnh</span></button>',
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
            stage: q('stage'), photo: q('photo'), ready: q('ready'), preview: q('preview'), chips: q('chips'),
            title: q('title'), sub: q('sub'),
            gpsDot: q('gpsDot'), gpsText: q('gpsText'), addrDot: q('addrDot'), addrText: q('addrText'),
            live: q('live'), review: q('review'), shoot: q('shoot'), file: q('file'),
            btnRetake: q('retake'), btnSave: q('save'), btnShare: q('share'), btnDownload: q('download'),
            toastEl: q('toast'), toastTimer: null
        };

        q('close').addEventListener('click', closeCamera);
        // Bấm nút là mở camera ngay trong cùng thao tác chạm (trình duyệt chỉ cho mở camera khi có thao tác của người dùng)
        ui.shoot.addEventListener('click', function () { if (!ui.shoot.disabled) ui.file.click(); });
        ui.file.addEventListener('change', onFilePicked);
        ui.btnRetake.addEventListener('click', retake);
        ui.btnSave.addEventListener('click', save);
        ui.btnShare.addEventListener('click', share);
        ui.btnDownload.addEventListener('click', download);
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

    // mode: 'ready' (chờ chụp, hiện nội dung sẽ in) | 'review' (xem lại ảnh đã chụp)
    function setMode(mode) {
        state.mode = mode;
        var ready = mode === 'ready';
        ui.live.style.display = ready ? 'flex' : 'none';
        ui.review.style.display = ready ? 'none' : 'flex';
        ui.ready.style.display = ready ? 'flex' : 'none';
        ui.chips.style.display = ready ? 'flex' : 'none';
        ui.stage.style.display = ready ? 'none' : 'block';
    }

    // Khung xem lại luôn đúng tỉ lệ của ảnh
    function layoutStage() {
        if (!state || state.mode !== 'review') return;
        var aw = ui.area.clientWidth, ah = ui.area.clientHeight;
        if (!aw || !ah) return;
        var ratio = state.ratio || 3 / 4;
        var w = aw, h = aw / ratio;
        if (h > ah) { h = ah; w = ah * ratio; }
        ui.stage.style.width = Math.floor(w) + 'px';
        ui.stage.style.height = Math.floor(h) + 'px';
    }

    // Danh sách "Sẽ in lên ảnh" trên màn hình chờ chụp, cập nhật theo GPS/địa chỉ hiện có
    function updatePreview() {
        if (!state || state.mode !== 'ready') return;
        var lines = buildLines(state.ticket, new Date(), state.pos, currentAddress(state), cfg, staffName);
        ui.preview.textContent = '';
        if (!lines.length) {
            var none = document.createElement('div');
            none.className = 'l';
            none.style.color = '#94a3b8';
            none.textContent = 'Quản trị viên đang tắt mọi mục: ảnh sẽ không có chữ.';
            ui.preview.appendChild(none);
            return;
        }
        lines.forEach(function (l) {
            var d = document.createElement('div');
            d.className = 'l';
            d.textContent = l.text;
            ui.preview.appendChild(d);
        });
    }

    // ===================== CẤU HÌNH TỪ SERVER =====================
    function baseUrl() { return (typeof CTX !== 'undefined' ? CTX : ''); }

    function loadConfig() {
        fetch(baseUrl() + '/api/settings/camera', { headers: { 'Accept': 'application/json' } })
            .then(function (r) { if (!r.ok) throw new Error('cfg ' + r.status); return r.json(); })
            .then(function (data) {
                Object.keys(DEFAULT_CFG).forEach(function (k) {
                    if (typeof data[k] === 'boolean') cfg[k] = data[k];
                });
                if (cfg.showStaff && !staffLoaded) loadStaffName();
                updatePreview();
            })
            .catch(function () { /* giữ cấu hình mặc định */ });
    }

    // Tên nhân viên đang đăng nhập (để in "NV: ..." khi ADMIN bật mục này)
    function loadStaffName() {
        staffLoaded = true;
        fetch(baseUrl() + '/api/auth/me', { headers: { 'Accept': 'application/json' } })
            .then(function (r) { if (!r.ok) throw new Error('me ' + r.status); return r.json(); })
            .then(function (u) { staffName = (u && u.fullName) || ''; updatePreview(); })
            .catch(function () { staffLoaded = false; });
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

    function startGps() {
        var s = state;
        if (!window.isSecureContext) { s.gpsStatus = 'insecure'; return; }
        if (!navigator.geolocation) { s.gpsStatus = 'unavailable'; return; }
        s.watchId = navigator.geolocation.watchPosition(function (p) {
            if (state !== s) return;
            s.pos = { lat: p.coords.latitude, lng: p.coords.longitude, acc: p.coords.accuracy || 0 };
            s.gpsStatus = 'ok';
            maybeGeocode();
            updateChips(); updateShoot(); updatePreview();
        }, function (err) {
            if (state !== s) return;
            if (!s.pos) s.gpsStatus = (err && err.code === 1) ? 'denied' : 'error';
            else if (err && err.code === 1) { s.gpsStatus = 'denied'; s.pos = null; }
            updateChips(); updateShoot(); updatePreview();
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
            updateChips(); updatePreview();
        });
    }

    // ===================== NÚT CHỤP =====================
    // Chỉ cho chụp khi GPS đã có kết quả (có tọa độ hoặc đã báo lỗi rõ ràng) để ảnh có đủ thông tin
    function updateShoot() {
        var s = state;
        if (!s) return;
        var waiting = s.gpsStatus === 'waiting';
        ui.shoot.disabled = !(s.mode === 'ready' && !waiting && !s.busy);
        btnLabel(ui.shoot, waiting ? 'Đang chờ GPS…' : (s.busy ? 'Đang xử lý ảnh…' : 'Chụp ảnh'));
    }

    // ===================== XỬ LÝ ẢNH =====================
    function snapshot(s, when) {
        var pos = s.pos ? { lat: s.pos.lat, lng: s.pos.lng, acc: s.pos.acc } : null;
        var address = currentAddress(s);
        return { when: when, pos: pos, address: address, lines: buildLines(s.ticket, when, pos, address, cfg, staffName) };
    }

    // Xuất JPEG; nếu quá nặng thì nén thêm cho vừa giới hạn của server
    function exportJpeg(canvas, quality, cb) {
        canvas.toBlob(function (blob) {
            if (blob && blob.size > MAX_BYTES && quality > 0.6) { exportJpeg(canvas, quality - 0.1, cb); return; }
            cb(blob);
        }, 'image/jpeg', quality);
    }

    // Vẽ ảnh gốc lên canvas, in chữ, rồi xuất JPEG
    function produce(s, source, sw, sh, taken) {
        var scale = Math.min(1, MAX_EDGE / Math.max(sw, sh));
        var W = Math.round(sw * scale), H = Math.round(sh * scale);
        var canvas = document.createElement('canvas');
        canvas.width = W;
        canvas.height = H;
        var ctx = canvas.getContext('2d');
        ctx.drawImage(source, 0, 0, W, H);

        var snap = snapshot(s, taken);
        drawOverlay(ctx, W, H, snap.lines);
        exportJpeg(canvas, JPEG_QUALITY, function (blob) {
            s.busy = false;
            updateShoot();
            if (!blob) { toast('Không tạo được ảnh, hãy thử lại.'); return; }
            showReview(s, blob, snap, W, H);
        });
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
        if (s.captured) URL.revokeObjectURL(s.captured.url);
        var name = safeName(accountOf(s.ticket)) + '_' + formatStamp(snap.when) + '.jpg';
        var c = {
            blob: blob, file: makeFile(blob, name), name: name, url: URL.createObjectURL(blob),
            pos: snap.pos, address: snap.address, saved: false, exported: false
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

    // Ảnh vừa chụp từ ứng dụng máy ảnh quay về trang
    function onFilePicked() {
        var s = state;
        var f = ui.file.files && ui.file.files[0];
        ui.file.value = '';
        if (!s || !f || s.busy) return;

        // Giờ in lên ảnh = giờ file được tạo; ảnh quá cũ (chọn nhầm từ thư viện) thì không nhận
        var taken = new Date();
        if (f.lastModified) {
            var made = new Date(f.lastModified);
            var age = Date.now() - made.getTime();
            if (age > MAX_PHOTO_AGE_MS) {
                toast('Ảnh này không phải ảnh vừa chụp (tạo lúc ' + formatTime(made) + '). Hãy chụp trực tiếp bằng camera.', 5500);
                return;
            }
            if (age >= 0) taken = made;
        }

        s.busy = true;
        updateShoot();
        toast('Đang xử lý ảnh…', 1500);
        var url = URL.createObjectURL(f);
        var img = new Image();
        img.onload = function () {
            URL.revokeObjectURL(url);
            if (state !== s) return;
            produce(s, img, img.naturalWidth, img.naturalHeight, taken);
        };
        img.onerror = function () {
            URL.revokeObjectURL(url);
            s.busy = false;
            updateShoot();
            toast('Không đọc được ảnh vừa chụp.');
        };
        img.src = url;
    }

    // Chụp lại / chụp tiếp: mở lại camera ngay (đang trong thao tác chạm của người dùng)
    function retake() {
        var s = state;
        if (!s || s.busy) return;
        if (s.captured) URL.revokeObjectURL(s.captured.url);
        s.captured = null;
        ui.photo.removeAttribute('src');
        setMode('ready');
        updateShoot();
        updatePreview();
        ui.file.click();
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
        updateChips();
        updatePreview();
        updateShoot();
    }

    // ticketId rỗng (null/undefined) = chụp nhanh, không gắn với ca nào
    function open(ticketId) {
        if (state) return;
        var free = (ticketId === null || ticketId === undefined);
        var ticket = free ? { _free: true }
            : ((typeof ticketCache !== 'undefined' && ticketCache) ? ticketCache[ticketId] : null);
        if (!ticket) { alert('Không tìm thấy thông tin ca, hãy tải lại danh sách.'); return; }
        ensureUi();

        var s = state = {
            ticketId: ticketId, ticket: ticket, free: free, mode: 'ready', ratio: 3 / 4,
            pos: null, gpsStatus: 'waiting', watchId: null,
            geoAddress: '', geoFrom: null, geoBusy: false, geoLastTry: 0, geoFails: 0,
            captured: null, busy: false, savedCount: 0, timer: null
        };

        ui.title.textContent = free ? 'Chụp ảnh nhanh' : (ticket.customerName || 'Khách hàng');
        updateSub();
        ui.root.classList.add('cc-open');
        document.body.style.overflow = 'hidden';
        setMode('ready');

        loadConfig();
        startGps();
        updateChips();
        updatePreview();
        updateShoot();
        s.timer = setInterval(tick, 1000);
    }

    function closeCamera() {
        var s = state;
        if (!s) return;
        if (s.busy) { toast('Đang xử lý, vui lòng chờ…'); return; }
        // Chụp gắn ca: cảnh báo nếu chưa Lưu. Chụp nhanh: cảnh báo nếu chưa Tải về / Chia sẻ
        var unsaved = s.captured && (s.free ? !s.captured.exported : !s.captured.saved);
        if (unsaved && !confirm(s.free ? 'Ảnh vừa chụp chưa được tải về hoặc chia sẻ. Bỏ ảnh này?' : 'Ảnh vừa chụp chưa được lưu. Bỏ ảnh này?')) return;

        clearInterval(s.timer);
        if (s.watchId !== null && navigator.geolocation) navigator.geolocation.clearWatch(s.watchId);
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
