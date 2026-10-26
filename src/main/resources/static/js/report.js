/*
 * report.js - Trang Báo cáo: ADMIN xuất file Excel, xem danh sách file đã xuất (có đếm ngược tới lúc bị xóa) và tải về.
 * Cần: CTX, app-common.js.
 */
let files = [];          // {id, fileName, sizeBytes, createdAt, ttlMinutes, expireAtMs}
let ttlMinutes = 30;
let tickTimer = null;
let syncTimer = null;
let exporting = false;

// Màu trong file Excel (phải khớp với ExportStatus ở phía server)
const LEGEND = [
    { label: 'Đã xử lý',             fill: '#C6EFCE', font: '#006100' },
    { label: 'KLL',                  fill: '#FFC7CE', font: '#9C0006' },
    { label: 'Đã gọi OK',            fill: '#D0F0F0', font: '#0E6B6B' },
    { label: 'Quá hẹn',              fill: '#F8CBAD', font: '#843C0C' },
    { label: 'Hẹn giờ',              fill: '#FFF2CC', font: '#7F6000' },
    { label: 'Hẹn khác',             fill: '#DDEBF7', font: '#1F4E78' },
    { label: 'Đang xử lý',           fill: '#FFFFFF', font: '#000000' },
    { label: 'Không còn trong file', fill: '#E7E6E6', font: '#595959' }
];

document.addEventListener('DOMContentLoaded', async function () {
    let admin = false;
    try {
        const me = await AppNav.getMe();
        admin = !!me.admin;
    } catch (e) { console.error(e); }

    if (!admin) {
        document.getElementById('notAdmin').classList.remove('d-none');
        return;
    }
    document.getElementById('exportSection').classList.remove('d-none');
    document.getElementById('filesSection').classList.remove('d-none');
    renderLegend();
    loadStaffs();
    await syncFiles();
    tickTimer = setInterval(tick, 1000);
    syncTimer = setInterval(syncFiles, 30000);   // đồng bộ lại với server (file bị xóa, xuất từ máy khác...)
});

function renderLegend() {
    document.getElementById('legend').innerHTML = LEGEND.map(function (l) {
        return '<span class="legend-chip" style="background:' + l.fill + ';color:' + l.font + ';">' + escapeHtml(l.label) + '</span>';
    }).join('');
}

async function loadStaffs() {
    try {
        const staffs = await apiFetch(CTX + '/api/tickets/staffs');
        const sel = document.getElementById('exStaff');
        staffs.forEach(function (name) {
            const opt = document.createElement('option');
            opt.value = name;
            opt.textContent = name;
            sel.appendChild(opt);
        });
    } catch (e) { console.error(e); }
}

// ===================== DANH SÁCH FILE =====================
function toFile(dto) {
    return {
        id: dto.id, fileName: dto.fileName, sizeBytes: dto.sizeBytes, createdAt: dto.createdAt,
        ttlMinutes: dto.ttlMinutes,
        expireAtMs: Date.now() + dto.secondsLeft * 1000   // dùng số giây còn lại của server để không phụ thuộc đồng hồ máy
    };
}

async function syncFiles() {
    try {
        const list = await apiFetch(CTX + '/api/exports');
        files = list.map(toFile);
        if (files.length) ttlMinutes = files[0].ttlMinutes;
        renderFiles();
    } catch (e) {
        document.getElementById('fileList').innerHTML = '<div class="alert alert-danger p-2 small mb-0">' + escapeHtml(e.message) + '</div>';
    }
}

function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' KB';
    return (bytes / 1024 / 1024).toFixed(1) + ' MB';
}

function formatLeft(ms) {
    const total = Math.max(0, Math.ceil(ms / 1000));
    return pad2(Math.floor(total / 60)) + ':' + pad2(total % 60);
}

function renderFiles() {
    document.getElementById('ttlText').textContent = ttlMinutes;
    document.getElementById('fileCount').textContent = files.length;
    const box = document.getElementById('fileList');
    if (!files.length) {
        box.innerHTML = '<div class="text-center text-muted small py-3"><i class="fa-regular fa-file-excel fs-3 d-block mb-1"></i>Chưa có file nào. Bấm "Xuất file Excel" để tạo.</div>';
        return;
    }
    box.innerHTML = files.map(function (f) {
        const id = escapeHtml(f.id);
        return '<div class="file-item" data-id="' + id + '">'
            + '<div class="d-flex align-items-center gap-2">'
            +   '<i class="fa-solid fa-file-excel text-success fs-3"></i>'
            +   '<div class="flex-grow-1" style="min-width:0;">'
            +     '<div class="fw-bold text-truncate">' + escapeHtml(f.fileName) + '</div>'
            +     '<div class="small text-muted">Xuất lúc ' + escapeHtml(formatDateTime(f.createdAt)) + ' • ' + formatSize(f.sizeBytes) + '</div>'
            +   '</div>'
            + '</div>'
            + '<div class="countdown small mt-2" data-cd="' + id + '"><i class="fa-regular fa-clock me-1"></i>Tự xóa sau <b></b></div>'
            + '<div class="d-flex gap-2 mt-2">'
            +   '<a class="btn btn-sm btn-success flex-grow-1 fw-bold" href="' + CTX + '/api/exports/' + id + '/download" onclick="return onDownload(\'' + id + '\')">'
            +     '<i class="fa-solid fa-download me-1"></i>Tải về</a>'
            +   '<button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteFile(\'' + id + '\')" title="Xóa file này"><i class="fa-regular fa-trash-can"></i></button>'
            + '</div>'
            + '</div>';
    }).join('');
    tick();
}

// Mỗi giây: cập nhật đồng hồ đếm ngược; file hết hạn thì gỡ khỏi danh sách
function tick() {
    const now = Date.now();
    let expired = false;
    files.forEach(function (f) {
        const left = f.expireAtMs - now;
        if (left <= 0) { expired = true; return; }
        const el = document.querySelector('[data-cd="' + f.id + '"]');
        if (!el) return;
        el.querySelector('b').textContent = formatLeft(left);
        el.classList.toggle('soon', left < 5 * 60 * 1000);
    });
    if (expired) {
        files = files.filter(function (f) { return f.expireAtMs > now; });
        renderFiles();
        showToast('Có file đã hết hạn và bị xóa khỏi hệ thống.');
    }
}

// Bấm "Tải về": nhắc lại thời hạn xóa. Trả về false để chặn tải nếu file đã hết hạn.
function onDownload(id) {
    const f = files.filter(function (x) { return x.id === id; })[0];
    if (!f) return false;
    const left = f.expireAtMs - Date.now();
    if (left <= 0) { syncFiles(); return false; }
    showToast('Đang tải file. Bản trên hệ thống sẽ bị xóa sau ' + formatLeft(left) + ' (' + ttlMinutes + ' phút kể từ lúc xuất).');
    return true;
}

async function deleteFile(id) {
    if (!confirm('Xóa file này khỏi hệ thống? (File đã tải về máy không bị ảnh hưởng)')) return;
    try {
        await apiFetch(CTX + '/api/exports/' + encodeURIComponent(id), { method: 'DELETE' });
        files = files.filter(function (f) { return f.id !== id; });
        renderFiles();
        showToast('Đã xóa file');
    } catch (e) {
        showToast(e.message, true);
    }
}

// ===================== XUẤT FILE =====================
async function doExport() {
    if (exporting) return;
    exporting = true;
    const btn = document.getElementById('btnExport');
    const label = btn.querySelector('span');
    btn.disabled = true;
    label.textContent = 'Đang tạo file...';
    try {
        const dto = await apiFetch(CTX + '/api/exports/tickets', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                staff: document.getElementById('exStaff').value,
                scope: document.getElementById('exScope').value
            })
        });
        ttlMinutes = dto.ttlMinutes;
        files.unshift(toFile(dto));
        renderFiles();
        showToast('Đã tạo file. File tự xóa sau ' + dto.ttlMinutes + ' phút, hãy tải về ngay.');
    } catch (e) {
        showToast(e.message, true);
    } finally {
        exporting = false;
        btn.disabled = false;
        label.textContent = 'Xuất file Excel';
    }
}
