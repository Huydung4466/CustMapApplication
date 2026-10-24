let currentUser = null;
let searchTimer = null;
let currentPage = 1;
const PAGE_SIZE = 10;
// Trang lịch sử đang xem: COMPLETED (đã hoàn thành) hoặc ISSUES (không liên lạc được & khác), do server gán
let meta = null; // trạng thái / lý do lưu trữ / các trang lịch sử, lấy từ /api/tickets/meta (enum phía server)

// ===================== NỘP BÁO CÁO (Google Form) =====================
// Form "BÁO CÁO KẾT QUẢ CÔNG VIỆC HẰNG NGÀY – ĐỘI KỸ THUẬT". Mã entry lấy từ chính form (đổi form thì cập nhật lại các mã này).
const REPORT_FORM_URL = 'https://docs.google.com/forms/d/e/1FAIpQLSfCOYYflGscdYCRIoTd71J4twFHl3KqTMzANM20Ps2FjeQHzA/viewform';
const REPORT_ENTRY = {
    date: '232984631',
    staff: '1386363437',
    area: '977003287',
    serviceType: '1313171066',
    account: '2037624764',
    customerName: '1327480577',
    phone: '464690292',
    address: '1666902213',
    cabinet: '1383431432',
    contractor: '1226051476'
};

// 24 địa bàn trong form (phải ghi đúng như trong form thì Google mới nhận)
const REPORT_AREAS = ['Buôn Ma Thuột', 'Ea Hleo', 'Ea Súp', 'Krông Năng', 'Krông Búk', 'Buôn Đôn', 'Cư Mgar', 'Ea Kar',
    'Mđrắk', 'Krông Pắk', 'Krông A Na', 'Krông Bông', 'Lắk', 'Cư Kuin', 'Buôn Hồ', 'Tuy Hòa', 'Đồng Xuân', 'Sông Cầu',
    'Tuy An', 'Sơn Hòa', 'Sông Hinh', 'Phú Hòa', 'Tây Hòa', 'Đông Hòa'];

// Bỏ dấu, chữ thường, bỏ khoảng trắng / dấu nháy để so khớp (VD "Cư M'gar" = "Cư Mgar", "Ea H'leo" = "Ea Hleo")
function normalizeText(str) {
    return String(str || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd').replace(/Đ/g, 'd').toLowerCase().replace(/[^a-z0-9]/g, '');
}

// Tìm địa bàn của form khớp với khu vực của ca; thử lần lượt: khu vực, cụm xã, xã. Không khớp thì trả null (nhân viên tự chọn)
function guessReportArea(item) {
    const candidates = [item.area, item.communeCluster, item.commune];
    // Tên dài xét trước để "Tuy An" không bị nhầm với "Tuy An..." khác, "Sơn Hòa"/"Phú Hòa"... đúng từng cái
    const sorted = REPORT_AREAS.slice().sort(function (x, y) { return normalizeText(y).length - normalizeText(x).length; });
    for (let i = 0; i < candidates.length; i++) {
        const text = normalizeText(candidates[i]);
        if (!text) continue;
        for (let j = 0; j < sorted.length; j++) {
            if (text.indexOf(normalizeText(sorted[j])) !== -1) return sorted[j];
        }
    }
    return null;
}

// Chỉ ca "Đã xử lý" (nằm trang Đã hoàn thành) mới có nút nộp báo cáo; ca KLL / không còn trong file thì không
function canSubmitReport(item) {
    return !item.archiveReason || item.archiveReason === 'DONE';
}

// Loại dịch vụ điền sẵn theo loại ca hạ tầng (khai báo ở TicketType phía server)
const SERVICE_BY_TICKET_TYPE = {
    PORT_KEM: 'FTTH',              // Port kém GPON
    LAN_HOMEWIFI_KEM: 'Mesh',      // LAN / HomeWifi kém
    LAP_THIEU_HOMEWIFI: 'Mesh'     // Lắp thiếu HomeWifi
};

// Ca hạ tầng: theo bảng trên. Ca phản ánh: đoán từ "Tên dịch vụ" (cột H trong file Excel), không đoán được thì để trống
function guessServiceTypes(item) {
    const byType = SERVICE_BY_TICKET_TYPE[item.ticketType];
    if (byType) return [byType];
    const result = [];
    const t = (item.serviceName || '').toLowerCase();
    if (/ftth|internet|cáp quang|cap quang|fiber|gpon/.test(t)) result.push('FTTH');
    if (/truyền hình|truyen hinh|\btv\b|mytv|nexttv/.test(t)) result.push('Truyền hình');
    if (/\bmesh\b|homewifi|home wifi/.test(t)) result.push('Mesh');
    if (/camera/.test(t)) result.push('Camera');
    return result;
}

function buildReportUrl(item) {
    const params = new URLSearchParams();
    params.set('usp', 'pp_url');
    const add = function (entry, value) {
        if (value !== null && value !== undefined && String(value).trim() !== '') params.append('entry.' + entry, String(value).trim());
    };
    // Ngày thực hiện = ngày ca được xử lý xong (chuyển vào lịch sử), dạng yyyy-mm-dd
    const d = item.archivedAt ? new Date(item.archivedAt) : new Date();
    const pad = function (n) { return String(n).padStart(2, '0'); };
    add(REPORT_ENTRY.date, d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()));
    add(REPORT_ENTRY.staff, item.assignedStaff || (currentUser && currentUser.fullName));
    add(REPORT_ENTRY.area, guessReportArea(item));
    guessServiceTypes(item).forEach(function (v) { add(REPORT_ENTRY.serviceType, v); });
    add(REPORT_ENTRY.account, item.subscriberAccount);
    add(REPORT_ENTRY.customerName, item.customerName);
    add(REPORT_ENTRY.phone, item.contactPhone || item.reportPhone);
    add(REPORT_ENTRY.address, item.installAddress);
    add(REPORT_ENTRY.cabinet, item.cabinetCode);
    add(REPORT_ENTRY.contractor, item.techStaffName);
    return REPORT_FORM_URL + '?' + params.toString();
}

document.addEventListener('DOMContentLoaded', async function () {
    await Promise.all([initUser(), initMeta()]);
    loadHistory();
});

async function initMeta() {
    try {
        meta = await apiFetch(CTX + '/api/tickets/meta');
        renderHistoryTabs();
    } catch (e) { console.error(e); }
}

function renderHistoryTabs() {
    document.getElementById('historyTabs').innerHTML = meta.historyTabs.map(function (t) {
        const active = t.code === HISTORY_TAB;
        return '<a href="' + CTX + t.path + '" class="btn btn-sm ' + (active ? 'btn-primary' : 'btn-outline-primary') + '">'
            + '<i class="' + escapeHtml(t.icon) + ' me-1"></i>' + escapeHtml(t.label) + '</a>';
    }).join('');
}

function currentTabInfo() {
    return meta ? meta.historyTabs.find(function (t) { return t.code === HISTORY_TAB; }) : null;
}

// Nhãn lý do lưu trữ; ca lưu trữ từ dữ liệu cũ chưa ghi lý do coi như đã hoàn thành
function reasonInfo(code) {
    const list = meta ? meta.archiveReasons : [];
    const found = list.find(function (r) { return r.code === (code || 'DONE'); });
    return found || { label: 'Đã lưu trữ', icon: 'fa-solid fa-box-archive', badgeClass: 'reason-closed' };
}

async function initUser() {
    try {
        currentUser = await AppNav.getMe();   // thanh trên cùng (top-navbar) đã lấy sẵn và hiện tên

        // ADMIN được xem lịch sử của từng nhân viên; STAFF chỉ xem của chính mình
        if (currentUser.admin) {
            const staffs = await apiFetch(CTX + '/api/tickets/staffs');
            const sel = document.getElementById('staffFilterSelect');
            sel.innerHTML = '';
            const optAll = document.createElement('option');
            optAll.value = 'ALL';
            optAll.textContent = '-- Tất cả nhân viên --';
            sel.appendChild(optAll);
            staffs.forEach(function (name) {
                const opt = document.createElement('option');
                opt.value = name;
                opt.textContent = 'NV: ' + name;
                if (name.trim().toUpperCase() === currentUser.fullName.trim().toUpperCase()) opt.selected = true;
                sel.appendChild(opt);
            });
            document.getElementById('staffCol').classList.remove('d-none');
        }
    } catch (e) { console.error(e); }
}

// keepPage = true: giữ trang hiện tại; mọi lần đổi bộ lọc gọi không tham số -> tự về trang 1
async function loadHistory(keepPage) {
    if (keepPage !== true) currentPage = 1;
    const params = new URLSearchParams();
    const staffSel = document.getElementById('staffFilterSelect');
    if (currentUser && currentUser.admin && staffSel.value) params.set('staffFilter', staffSel.value);
    const kw = document.getElementById('keywordInput').value.trim();
    if (kw) params.set('keyword', kw);
    const from = document.getElementById('fromDateInput').value;
    const to = document.getElementById('toDateInput').value;
    if (from) params.set('fromDate', from);
    if (to) params.set('toDate', to);
    params.set('tab', HISTORY_TAB);
    params.set('page', currentPage);
    params.set('size', PAGE_SIZE);

    const box = document.getElementById('historyList');
    try {
        const data = await apiFetch(CTX + '/api/tickets/history?' + params.toString());
        currentPage = data.page;
        document.getElementById('historyCountText').textContent = data.totalItem;
        renderHistory(data.resultList);
        renderPagination(data);
    } catch (e) {
        box.innerHTML = '<div class="alert alert-danger p-2 small">' + escapeHtml(e.message) + '</div>';
        document.getElementById('paginationBar').innerHTML = '';
    }
}

function debounceLoadHistory() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(function () { loadHistory(); }, 300);
}

function resetHistoryFilters() {
    document.getElementById('keywordInput').value = '';
    document.getElementById('fromDateInput').value = '';
    document.getElementById('toDateInput').value = '';
    loadHistory();
}

function formatDateTime(v) {
    if (!v) return '';
    const d = new Date(v);
    const pad = function (n) { return String(n).padStart(2, '0'); };
    return pad(d.getHours()) + ':' + pad(d.getMinutes()) + ' ' + pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + d.getFullYear();
}

function renderHistory(items) {
    const box = document.getElementById('historyList');
    if (!items || items.length === 0) {
        const tabInfo = currentTabInfo();
        box.innerHTML = '<div class="card border-0 shadow-sm rounded-4 p-4 text-center bg-white text-muted small">'
            + escapeHtml(tabInfo ? tabInfo.emptyText : 'Chưa có ca nào trong lịch sử') + '</div>';
        return;
    }

    box.innerHTML = items.map(function (item) {
        const reason = reasonInfo(item.archiveReason);
        const reasonBadge = '<span class="status-badge ' + escapeHtml(reason.badgeClass) + '"><i class="' + escapeHtml(reason.icon) + '"></i>'
            + escapeHtml(reason.label) + '</span>';

        let phones = '';
        if (item.contactPhone) {
            phones += '<a href="tel:' + escapeHtml(item.contactPhone) + '" class="btn-call-phone"><i class="fa-solid fa-phone"></i>' + escapeHtml(item.contactPhone) + '</a>';
        }
        if (item.reportPhone && item.reportPhone !== item.contactPhone) {
            phones += '<a href="tel:' + escapeHtml(item.reportPhone) + '" class="btn-call-phone btn-call-report"><i class="fa-solid fa-phone-volume"></i>' + escapeHtml(item.reportPhone) + '</a>';
        }

        const complaint = escapeHtml(item.complaintContent || 'Không có nội dung');
        const staffInfo = (currentUser && currentUser.admin)
            ? '<span>NV: <b class="text-dark">' + escapeHtml(item.assignedStaff || '') + '</b></span>' : '<span></span>';

        return '<div class="ticket-card history-card">'
            + '<div class="card-header-row"><div>'
            +   '<span class="customer-title">' + escapeHtml(item.customerName || 'Khách hàng') + '</span>'
            + '</div><div>' + reasonBadge + '</div></div>'
            + '<div class="meta-info-row"><div><span class="account-tag">' + escapeHtml(item.subscriberAccount || item.ticketCode) + '</span></div>'
            +   '<span class="text-muted vt-fs-78">PA: #' + escapeHtml(item.ticketCode) + '</span></div>'
            + '<div class="text-muted small mb-1"><i class="fa-regular fa-calendar-check me-1"></i>Chuyển vào lịch sử lúc ' + escapeHtml(formatDateTime(item.archivedAt)) + '</div>'
            + (item.appointmentTime ? '<div class="text-success small fw-bold mb-1"><i class="fa-regular fa-clock me-1"></i>' + escapeHtml(item.appointmentTime) + '</div>' : '')
            + '<div class="location-section"><div><i class="fa-solid fa-location-dot text-danger me-1"></i>' + escapeHtml(item.installAddress || 'Chưa có địa chỉ') + '</div></div>'
            + '<div class="complaint-box"><div class="complaint-collapsed" id="h-' + item.id + '">' + complaint + '</div>'
            +   '<a class="see-more-link" onclick="toggleHistoryText(' + item.id + ')">...xem thêm</a>'
            +   (item.callNote ? '<div class="text-muted fst-italic mt-1 vt-fs-78"><i class="fa-regular fa-note-sticky me-1"></i>' + escapeHtml(item.callNote) + '</div>' : '')
            + '</div>'
            + '<div class="action-buttons-grid">' + phones
            +   (canSubmitReport(item)
                ? '<a class="btn-report-submit ms-auto" href="' + escapeHtml(buildReportUrl(item)) + '" target="_blank" rel="noopener"><i class="fa-solid fa-file-signature"></i>Nộp báo cáo</a>'
                : '')
            +   '<button type="button" class="btn-action-trigger' + (canSubmitReport(item) ? '' : ' ms-auto') + '" onclick="restoreTicket(' + item.id + ')"><i class="fa-solid fa-rotate-left"></i>Đưa về danh sách</button>'
            + '</div>'
            + '<div class="card-footer-info"><span>Khu vực: <b>' + escapeHtml(item.area || '') + '</b></span>' + staffInfo + '</div>'
            + '</div>';
    }).join('');
}

function renderPagination(data) {
    const bar = document.getElementById('paginationBar');
    if (!data || data.totalPage <= 1) {
        bar.innerHTML = '';
        return;
    }
    const prevDisabled = data.page <= 1 ? ' disabled' : '';
    const nextDisabled = data.page >= data.totalPage ? ' disabled' : '';
    bar.innerHTML =
        '<button type="button" class="btn btn-sm btn-outline-primary rounded-pill px-3"' + prevDisabled
        + ' onclick="goToPage(' + (data.page - 1) + ')"><i class="fa-solid fa-chevron-left me-1"></i>Trước</button>'
        + '<span class="small text-muted">Trang <b class="text-dark">' + data.page + '</b> / ' + data.totalPage + '</span>'
        + '<button type="button" class="btn btn-sm btn-outline-primary rounded-pill px-3"' + nextDisabled
        + ' onclick="goToPage(' + (data.page + 1) + ')">Sau<i class="fa-solid fa-chevron-right ms-1"></i></button>';
}

function goToPage(page) {
    currentPage = page;
    loadHistory(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function toggleHistoryText(id) {
    document.getElementById('h-' + id).classList.toggle('complaint-collapsed');
}

async function restoreTicket(id) {
    if (!confirm('Đưa ca này về danh sách chính để xử lý tiếp?')) return;
    try {
        await apiFetch(CTX + '/api/tickets/' + id + '/restore', { method: 'PUT' });
        loadHistory(true);
    } catch (e) {
        alert(e.message);
    }
}

async function apiFetch(url, options) {
    const res = await fetch(url, options);
    if (res.redirected && res.url.includes('/login')) {
        window.location.href = CTX + '/login';
        throw new Error('Phiên đăng nhập đã hết hạn');
    }
    if (!res.ok) {
        let msg = 'Có lỗi xảy ra (mã ' + res.status + ')';
        try {
            const err = await res.json();
            msg = err.message || err.detail || msg;
        } catch (_) { /* không có JSON */ }
        throw new Error(msg);
    }
    return res.json();
}

function escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
