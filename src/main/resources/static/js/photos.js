/*
 * photos.js - Trang Ảnh (/photos).
 *  - Mọi tài khoản: xem lưới ảnh thu nhỏ (mới nhất lên đầu), lọc theo người chụp, bấm vào ảnh để xem đầy đủ.
 *  - Dưới mỗi ảnh có BỘ ĐẾM GIỜ còn lại trước khi ảnh tự xóa (hạn = giờ chụp + 72 giờ, theo đồng hồ server).
 *    Đang tắt tự xóa thì hiện "Tự xóa đang tắt"; ảnh cũ không có hạn thì hiện "Lưu lâu dài".
 *  - Chỉ tài khoản có quyền Camera (me.camera) thấy nút Tải về và Xóa; server cũng chặn người khác (ROLE_CAMERA).
 * Cần biến CTX (khai báo trong photos.jsp) và app-common.js (apiFetch, escapeHtml, showToast, formatDateTime).
 */
let me = null;
let owner = '';          // id người chụp đang lọc ('' = tất cả)
let page = 0;
let hasMore = false;
let loading = false;
let autoDelete = true;
let retentionHours = 72;
let items = [];          // ảnh đã tải, mỗi ảnh có thêm deadline (thời điểm hết hạn theo đồng hồ máy này)
let openId = null;       // ảnh đang xem đầy đủ

function $(id) { return document.getElementById(id); }

document.addEventListener('DOMContentLoaded', init);

async function init() {
    try {
        me = await AppNav.getMe();
        await Promise.all([loadOwners(), loadPage(0)]);
        setInterval(tick, 1000);
        document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeLightbox(); });
    } catch (e) {
        $('grid').innerHTML = '<div class="alert alert-danger small">' + escapeHtml(e.message) + '</div>';
    }
}

// ---------------------------------------------------------------- danh sách

async function loadOwners() {
    const list = await apiFetch(CTX + '/api/photos/owners');
    const total = list.reduce(function (n, o) { return n + o.count; }, 0);
    $('owners').innerHTML = chip('', 'Tất cả', total)
        + list.map(function (o) { return chip(String(o.userId), o.fullName || o.username || ('#' + o.userId), o.count); }).join('');
}

function chip(value, label, count) {
    return '<button type="button" class="ph-chip' + (owner === value ? ' active' : '') + '" onclick="pickOwner(\'' + escapeHtml(value) + '\')">'
        + escapeHtml(label) + '<span class="ph-chip-n">' + Number(count) + '</span></button>';
}

async function pickOwner(value) {
    owner = value;
    await Promise.all([loadOwners(), loadPage(0)]);
}

async function loadMore() { await loadPage(page + 1); }

async function loadPage(p) {
    if (loading) return;
    loading = true;
    $('btnMore').disabled = true;
    try {
        const data = await apiFetch(CTX + '/api/photos?page=' + p + (owner ? '&owner=' + encodeURIComponent(owner) : ''));
        autoDelete = data.autoDeleteEnabled;
        retentionHours = data.retentionHours;
        const fetchedAt = Date.now();
        // Hạn tự xóa quy ra thời điểm trên đồng hồ máy này (không phụ thuộc giờ điện thoại lệch so với server)
        data.items.forEach(function (it) { it.deadline = it.remainingSeconds === null ? null : fetchedAt + it.remainingSeconds * 1000; });
        items = p === 0 ? data.items : items.concat(data.items);
        page = p;
        hasMore = data.hasMore;
        render();
    } catch (e) {
        showToast(e.message, true);
    } finally {
        loading = false;
        $('btnMore').disabled = false;
    }
}

function render() {
    $('retentionNote').textContent = autoDelete
        ? 'Ảnh tự xóa sau ' + retentionHours + ' giờ kể từ lúc chụp.'
        : 'Tự xóa ảnh đang tắt: ảnh được giữ lại cho tới khi tài khoản Camera xóa hoặc bật lại tự xóa.';
    $('grid').innerHTML = items.map(cardHtml).join('');
    $('emptyState').classList.toggle('d-none', items.length > 0);
    $('btnMore').classList.toggle('d-none', !hasMore);
    tick();
}

function cardHtml(it) {
    const who = it.takenByName || it.takenByUsername || '—';
    const actions = me.camera
        ? '<div class="ph-actions">'
            + '<a class="btn btn-outline-secondary" href="' + CTX + escapeHtml(it.downloadUrl) + '"><i class="fa-solid fa-download me-1"></i>Tải</a>'
            + '<button type="button" class="btn btn-outline-danger" onclick="deletePhoto(' + Number(it.id) + ')"><i class="fa-regular fa-trash-can me-1"></i>Xóa</button>'
            + '</div>'
        : '';
    return '<div class="ph-card" data-id="' + Number(it.id) + '">'
        + '<div class="ph-thumb" onclick="openLightbox(' + Number(it.id) + ')">'
        +   '<img src="' + CTX + escapeHtml(it.thumbUrl) + '" loading="lazy" alt="Ảnh hiện trường">'
        +   (it.ticketId ? '<span class="ph-tag">Gắn ca</span>' : '')
        + '</div>'
        + '<div class="ph-body">'
        +   '<div class="ph-who">' + escapeHtml(who) + '</div>'
        +   '<div class="ph-time">' + escapeHtml(formatDateTime(it.takenAt)) + '</div>'
        +   (it.address ? '<div class="ph-addr">' + escapeHtml(it.address) + '</div>' : '')
        +   '<div class="ph-count" data-count-id="' + Number(it.id) + '"></div>'
        +   actions
        + '</div>'
        + '</div>';
}

// ---------------------------------------------------------------- đếm ngược

function pad(n) { return n < 10 ? '0' + n : String(n); }

// Còn lại giờ:phút:giây, ví dụ 71:30:05
function fmtRemaining(ms) {
    const total = Math.max(0, Math.floor(ms / 1000));
    return Math.floor(total / 3600) + ':' + pad(Math.floor((total % 3600) / 60)) + ':' + pad(total % 60);
}

// Nội dung và màu của bộ đếm cho một ảnh: [html, class]
function countdownOf(it) {
    if (it.deadline === null) return ['<i class="fa-solid fa-infinity"></i>Lưu lâu dài', 'muted'];
    if (!autoDelete) return ['<i class="fa-regular fa-clock"></i>Tự xóa đang tắt', 'muted'];
    const left = it.deadline - Date.now();
    if (left <= 0) return ['<i class="fa-solid fa-hourglass-end"></i>Hết hạn, sắp được dọn', 'danger'];
    const cls = left < 3600 * 1000 ? 'danger' : (left < 6 * 3600 * 1000 ? 'warn' : '');
    return ['<i class="fa-regular fa-hourglass-half"></i>Tự xóa sau ' + fmtRemaining(left), cls];
}

function paintCount(el, it) {
    const r = countdownOf(it);
    el.className = 'ph-count' + (r[1] ? ' ' + r[1] : '');
    el.innerHTML = r[0];
}

function tick() {
    document.querySelectorAll('.ph-count[data-count-id]').forEach(function (el) {
        const it = items.find(function (x) { return x.id === Number(el.getAttribute('data-count-id')); });
        if (it) paintCount(el, it);
    });
    if (openId !== null) {
        const it = items.find(function (x) { return x.id === openId; });
        if (it) paintCount($('lbCount'), it);
    }
}

// ---------------------------------------------------------------- xem đầy đủ / xóa

function openLightbox(id) {
    const it = items.find(function (x) { return x.id === id; });
    if (!it) return;
    openId = id;
    $('lbImg').src = CTX + it.fileUrl;
    const who = it.takenByName || it.takenByUsername || '—';
    let meta = '<b>' + escapeHtml(who) + '</b> · ' + escapeHtml(formatDateTime(it.takenAt));
    if (it.address) meta += '<br>' + escapeHtml(it.address);
    if (it.latitude !== null && it.longitude !== null) {
        meta += '<br><a href="https://www.google.com/maps?q=' + encodeURIComponent(it.latitude + ',' + it.longitude)
            + '" target="_blank" rel="noopener">' + escapeHtml(Number(it.latitude).toFixed(6) + ', ' + Number(it.longitude).toFixed(6)) + '</a>';
    }
    $('lbMeta').innerHTML = meta;
    $('lbActions').classList.toggle('d-none', !me.camera);
    $('lbDownload').href = CTX + it.downloadUrl;
    $('lbDelete').onclick = function () { deletePhoto(id); };
    $('lightbox').classList.add('open');
    $('lightbox').setAttribute('aria-hidden', 'false');
    document.body.classList.add('ph-open');
    tick();
}

function closeLightbox() {
    openId = null;
    $('lightbox').classList.remove('open');
    $('lightbox').setAttribute('aria-hidden', 'true');
    $('lbImg').removeAttribute('src');
    document.body.classList.remove('ph-open');
}

async function deletePhoto(id) {
    if (!me.camera) return;
    if (!confirm('Xóa ảnh này khỏi máy chủ? Không khôi phục được.')) return;
    try {
        await apiFetch(CTX + '/api/photos/' + id, { method: 'DELETE' });
        showToast('Đã xóa ảnh', false);
        closeLightbox();
        await Promise.all([loadOwners(), loadPage(0)]);
    } catch (e) {
        showToast(e.message, true);
    }
}
