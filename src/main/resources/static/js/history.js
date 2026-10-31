let currentUser = null;
let searchTimer = null;
let currentPage = 1;
const PAGE_SIZE = 10;
// Trang lịch sử đang xem: COMPLETED (đã hoàn thành) hoặc ISSUES (không liên lạc được & khác), do server gán
let meta = null;          // trạng thái / lý do lưu trữ / các trang lịch sử, lấy từ /api/tickets/meta (enum phía server)
let reqSeq = 0;           // đánh số mỗi lần tải để bỏ qua kết quả trả về trễ của lần tải cũ
let itemsById = {};       // ca của trang đang hiển thị, tra theo id (dùng cho hộp thoại chi tiết)
const tabCounts = {};     // số ca của từng trang lịch sử (hiện trên tab)

// Dòng mô tả dưới tiêu đề của từng trang
const TAB_HINTS = {
    COMPLETED: 'Ca đã xử lý xong: nộp báo cáo, kiểm tra trạng thái báo cáo hoặc đưa về danh sách khi cần.',
    ISSUES: 'Ca không liên lạc được, khách báo ổn hoặc không còn trong file GNOC. Có thể đưa về danh sách để xử lý lại.'
};
// Màu nhãn Bootstrap của loại ca (TicketType.color) -> màu nhãn của trang này
const TYPE_TAG = { danger: 'red', warning: 'amber', info: 'blue', primary: 'blue', success: 'green', secondary: 'slate' };

const $ = function (id) { return document.getElementById(id); };

// ===================== NỘP BÁO CÁO =====================
// Nút "Nộp báo cáo" mở trang trung gian /report-form/{id} (xem thông tin ca, chọn nhóm nhiệm vụ, chọn / lưu mẫu nội dung)
// rồi mới sang Google Form. Phần dựng link Google Form nằm trong js/report-form.js.
// Chỉ ca "Đã xử lý" (nằm trang Đã hoàn thành) mới có nút nộp báo cáo; ca KLL / không còn trong file thì không
function canSubmitReport(item) {
    return !item.archiveReason || item.archiveReason === 'DONE';
}

document.addEventListener('DOMContentLoaded', async function () {
    $('hxSubtitle').textContent = TAB_HINTS[HISTORY_TAB] || '';
    bindEvents();
    await Promise.all([initUser(), initMeta()]);
    loadHistory();
    loadTabCounts();
});

function bindEvents() {
    $('keywordInput').addEventListener('input', debounceLoadHistory);
    ['fromDateInput', 'toDateInput'].forEach(function (id) {
        $(id).addEventListener('change', function () { loadHistory(); });
    });
    $('staffFilterSelect').addEventListener('change', function () { loadHistory(); loadTabCounts(); });
    $('hxClearBtn').addEventListener('click', resetHistoryFilters);

    // Mọi nút trong danh sách dùng chung một bộ lắng nghe
    $('historyList').addEventListener('click', onListClick);
    $('paginationBar').addEventListener('click', function (e) {
        const b = e.target.closest('[data-page]');
        if (b && !b.disabled) goToPage(Number(b.dataset.page));
    });

    // Phím Esc đóng hộp xác nhận
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && $('hxConfirmOverlay').classList.contains('show')) answerConfirm(false);
    });
}

async function initMeta() {
    try {
        meta = await apiFetch(CTX + '/api/tickets/meta');
        renderHistoryTabs();
        const t = currentTabInfo();
        if (t) $('hxListTitle').textContent = 'Danh sách ca: ' + t.label.toLowerCase();
    } catch (e) { console.error(e); }
}

function renderHistoryTabs() {
    $('historyTabs').innerHTML = meta.historyTabs.map(function (t) {
        const active = t.code === HISTORY_TAB;
        return '<a href="' + escapeHtml(CTX + t.path) + '" class="hx-tab' + (active ? ' is-active' : '') + '"'
            + (active ? ' aria-current="page"' : '') + '>'
            + '<i class="' + escapeHtml(t.icon) + '"></i><span>' + escapeHtml(t.label) + '</span>'
            + '<span class="hx-tabcount" data-code="' + escapeHtml(t.code) + '"></span></a>';
    }).join('');
    paintTabCounts();
}

// Số ca của từng trang (không tính ô tìm kiếm / ngày, chỉ theo nhân viên đang chọn)
async function loadTabCounts() {
    if (!meta) return;
    const staff = staffParam();
    await Promise.all(meta.historyTabs.map(async function (t) {
        const p = new URLSearchParams();
        if (staff) p.set('staffFilter', staff);
        p.set('tab', t.code);
        p.set('page', 1);
        p.set('size', 1);
        try {
            const d = await apiFetch(CTX + '/api/tickets/history?' + p.toString());
            tabCounts[t.code] = d.totalItem;
        } catch (e) { /* không lấy được thì để trống số ca */ }
    }));
    paintTabCounts();
}

function paintTabCounts() {
    document.querySelectorAll('.hx-tabcount[data-code]').forEach(function (el) {
        const n = tabCounts[el.dataset.code];
        el.textContent = (n === undefined || n === null) ? '' : n;
    });
}

function staffParam() {
    const sel = $('staffFilterSelect');
    return (currentUser && currentUser.admin && sel.value) ? sel.value : '';
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
            const sel = $('staffFilterSelect');
            sel.innerHTML = '';
            const optAll = document.createElement('option');
            optAll.value = 'ALL';
            optAll.textContent = 'Tất cả nhân viên';
            sel.appendChild(optAll);
            staffs.forEach(function (name) {
                const opt = document.createElement('option');
                opt.value = name;
                opt.textContent = 'NV: ' + name;
                if (name.trim().toUpperCase() === currentUser.fullName.trim().toUpperCase()) opt.selected = true;
                sel.appendChild(opt);
            });
            $('staffCol').classList.remove('is-hidden');
            $('hxFilterGrid').classList.add('has-staff');
        }
    } catch (e) { console.error(e); }
}

function hasActiveFilter() {
    return !!($('keywordInput').value.trim() || $('fromDateInput').value || $('toDateInput').value);
}

// keepPage = true: giữ trang hiện tại; mọi lần đổi bộ lọc gọi không tham số -> tự về trang 1
async function loadHistory(keepPage) {
    if (keepPage !== true) currentPage = 1;
    const params = new URLSearchParams();
    const staff = staffParam();
    if (staff) params.set('staffFilter', staff);
    const kw = $('keywordInput').value.trim();
    if (kw) params.set('keyword', kw);
    const from = $('fromDateInput').value;
    const to = $('toDateInput').value;
    if (from) params.set('fromDate', from);
    if (to) params.set('toDate', to);
    params.set('tab', HISTORY_TAB);
    params.set('page', currentPage);
    params.set('size', PAGE_SIZE);

    const box = $('historyList');
    const seq = ++reqSeq;
    box.classList.add('is-loading');
    try {
        const data = await apiFetch(CTX + '/api/tickets/history?' + params.toString());
        if (seq !== reqSeq) return;   // đã có lần tải mới hơn
        currentPage = data.page;
        renderHistory(data.resultList);
        renderResultText(data);
        renderPagination(data);
    } catch (e) {
        if (seq !== reqSeq) return;
        box.innerHTML = '<div class="hx-error"><i class="fa-solid fa-triangle-exclamation me-1"></i>' + escapeHtml(e.message) + '</div>';
        $('paginationBar').innerHTML = '';
        $('hxResultText').textContent = 'Không tải được dữ liệu';
        $('historyCountText').textContent = '';
    } finally {
        if (seq === reqSeq) box.classList.remove('is-loading');
    }
}

function renderResultText(data) {
    const total = Number(data.totalItem) || 0;
    const shown = (data.resultList || []).length;
    $('historyCountText').textContent = total + ' kết quả';
    if (total === 0) {
        $('hxResultText').innerHTML = hasActiveFilter() ? 'Không có ca phù hợp với bộ lọc' : 'Chưa có ca nào';
        return;
    }
    const start = (data.page - 1) * PAGE_SIZE + 1;
    const end = start + shown - 1;
    $('hxResultText').innerHTML = 'Hiển thị <strong>' + start + '–' + end + '</strong> / <strong>' + total + '</strong> ca'
        + (hasActiveFilter() ? ' phù hợp với bộ lọc' : '');
}

function debounceLoadHistory() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(function () { loadHistory(); }, 300);
}

function resetHistoryFilters() {
    $('keywordInput').value = '';
    $('fromDateInput').value = '';
    $('toDateInput').value = '';
    loadHistory();
}

function formatDateTime(v) {
    if (!v) return '';
    const d = new Date(v);
    const pad = function (n) { return String(n).padStart(2, '0'); };
    return pad(d.getHours()) + ':' + pad(d.getMinutes()) + ' ' + pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + d.getFullYear();
}

// ===================== THẺ CA =====================
function renderHistory(items) {
    const box = $('historyList');
    itemsById = {};
    if (!items || items.length === 0) {
        const tabInfo = currentTabInfo();
        const filtered = hasActiveFilter();
        box.innerHTML = '<div class="hx-empty"><div class="hx-empty-icon"><i class="fa-solid ' + (filtered ? 'fa-magnifying-glass' : 'fa-inbox') + '"></i></div>'
            + '<h3>' + (filtered ? 'Không tìm thấy ca phù hợp' : 'Chưa có ca nào') + '</h3>'
            + '<p>' + escapeHtml(filtered ? 'Thử đổi từ khóa hoặc khoảng thời gian.' : (tabInfo ? tabInfo.emptyText : 'Chưa có ca nào trong lịch sử')) + '</p>'
            + (filtered ? '<button type="button" class="hx-btn outline" data-act="clear" style="flex:none">Xóa bộ lọc</button>' : '')
            + '</div>';
        return;
    }
    items.forEach(function (it) { itemsById[it.id] = it; });
    box.innerHTML = items.map(cardHtml).join('');
}

function phoneButtons(item) {
    let h = '';
    if (item.contactPhone) {
        h += '<a href="tel:' + escapeHtml(item.contactPhone) + '" class="hx-btn call" aria-label="Gọi khách hàng ' + escapeHtml(item.contactPhone) + '">'
            + '<i class="fa-solid fa-phone"></i>' + escapeHtml(item.contactPhone) + '</a>';
    }
    if (item.reportPhone && item.reportPhone !== item.contactPhone) {
        h += '<a href="tel:' + escapeHtml(item.reportPhone) + '" class="hx-btn call alt" title="Số nhập báo cáo" aria-label="Gọi số nhập báo cáo ' + escapeHtml(item.reportPhone) + '">'
            + '<i class="fa-solid fa-phone-volume"></i>' + escapeHtml(item.reportPhone) + '</a>';
    }
    return h;
}

// Phần nội dung: ca phản ánh -> đoạn nội dung khiếu nại; ca hạ tầng -> vài thông số nổi bật
function contentHtml(item) {
    const attrs = item.attributeItems || [];
    if (item.ticketGroup === 'HA_TANG' && attrs.length) {
        const chips = attrs.slice(0, 4).map(function (a) {
            return '<span class="hx-chip"><i>' + escapeHtml(a.label) + '</i><b>' + escapeHtml(a.value) + (a.unit ? ' ' + escapeHtml(a.unit) : '') + '</b></span>';
        }).join('');
        return '<div class="hx-desc"><div class="hx-chips">' + chips + '</div>'
            + (attrs.length > 4 ? '<button type="button" class="hx-more" data-act="detail" data-id="' + item.id + '">Xem đủ ' + attrs.length + ' thông số</button>' : '')
            + '</div>';
    }
    const text = item.complaintContent || 'Không có nội dung';
    const long = text.length > 110;
    return '<div class="hx-desc" id="h-' + item.id + '"><div class="hx-desc-text">' + escapeHtml(text) + '</div>'
        + (long ? '<button type="button" class="hx-more" data-act="toggle" data-id="' + item.id + '">Xem thêm</button>' : '')
        + '</div>';
}

function cardHtml(item) {
    const reason = reasonInfo(item.archiveReason);
    const reasonTag = '<span class="hx-tag ' + escapeHtml(reason.badgeClass) + '"><i class="' + escapeHtml(reason.icon) + '"></i>' + escapeHtml(reason.label) + '</span>';

    // Nhãn nộp báo cáo cạnh nhãn "Đã xử lý" (chỉ ca nộp được báo cáo); nhãn / màu do server gửi (ReportStatus)
    const canReport = canSubmitReport(item);
    const reportDone = item.reportStatus === 'SUBMITTED';
    const reportTag = canReport
        ? '<span class="hx-tag ' + escapeHtml(item.reportStatusBadge || 'report-not-submitted') + '"><i class="'
            + escapeHtml(item.reportStatusIcon || 'fa-regular fa-file') + '"></i>' + escapeHtml(item.reportStatusLabel || 'Chưa nộp') + '</span>'
        : '';

    const isInfra = item.ticketGroup === 'HA_TANG';
    const typeTag = isInfra
        ? '<span class="hx-tag ' + (TYPE_TAG[item.ticketTypeColor] || 'slate') + '">' + escapeHtml(item.ticketTypeLabel) + '</span>' : '';

    const info = '<span><i class="fa-regular fa-calendar-check"></i>Chuyển vào lịch sử lúc ' + escapeHtml(formatDateTime(item.archivedAt)) + '</span>'
        + (item.cabinetCode ? '<span><i class="fa-solid fa-server"></i>Tủ: <b>' + escapeHtml(item.cabinetCode) + '</b></span>' : '')
        + (item.appointmentTime ? '<span class="is-appt"><i class="fa-regular fa-clock"></i>' + escapeHtml(item.appointmentTime) + '</span>' : '');

    const staffInfo = (currentUser && currentUser.admin)
        ? '<span>NV: <strong>' + escapeHtml(item.assignedStaff || '') + '</strong></span>' : '';

    const reportBtns = canReport
        ? '<a class="hx-btn ' + (reportDone ? 'green-line' : 'green') + '" href="' + escapeHtml(CTX) + '/report-form/' + item.id + '"><i class="fa-solid fa-file-signature"></i>'
            + (reportDone ? 'Nộp lại' : 'Nộp báo cáo') + '</a>'
            + (reportDone ? '' : '<button type="button" class="hx-btn icon" data-act="mark" data-id="' + item.id + '" title="Đã nộp rồi? Bấm để đánh dấu đã nộp" aria-label="Đánh dấu đã nộp báo cáo"><i class="fa-solid fa-file-circle-check"></i></button>')
        : '';

    return '<article class="hx-case" id="case-' + item.id + '">'
        + '<div class="hx-case-head"><h3 class="hx-name">' + escapeHtml(item.customerName || 'Khách hàng') + '</h3>'
        +   '<div class="hx-badges">' + reasonTag + reportTag + '</div></div>'
        + '<div class="hx-idrow"><span class="hx-code">' + escapeHtml(item.subscriberAccount || item.ticketCode) + '</span>'
        +   '<span class="hx-pa">' + (typeTag ? typeTag + ' ' : 'PA: #' + escapeHtml(item.ticketCode)) + '</span></div>'
        + '<div class="hx-info">' + info + '</div>'
        + '<div class="hx-address"><i class="fa-solid fa-location-dot"></i><span>' + escapeHtml(item.installAddress || 'Chưa có địa chỉ') + '</span></div>'
        + contentHtml(item)
        + (item.callNote ? '<div class="hx-note"><i class="fa-regular fa-note-sticky"></i><span>' + escapeHtml(item.callNote) + '</span></div>' : '')
        + '<div class="hx-foot"><div class="hx-meta"><span>Khu vực: <strong>' + escapeHtml(item.area || '') + '</strong></span>' + staffInfo + '</div>'
        +   '<div class="hx-actions">' + phoneButtons(item)
        +     '<button type="button" class="hx-btn outline" data-act="detail" data-id="' + item.id + '"><i class="fa-regular fa-eye"></i>Chi tiết</button>'
        +     reportBtns
        +     '<button type="button" class="hx-btn gray" data-act="restore" data-id="' + item.id + '"><i class="fa-solid fa-rotate-left"></i>Đưa về danh sách</button>'
        +   '</div></div>'
        + '</article>';
}

function onListClick(e) {
    const btn = e.target.closest('[data-act]');
    if (!btn) return;
    const id = btn.dataset.id;
    switch (btn.dataset.act) {
        case 'toggle': toggleHistoryText(btn, id); break;
        case 'detail': openDetail(id); break;
        case 'restore': restoreTicket(Number(id), btn); break;
        case 'mark': markReportSubmitted(Number(id), btn); break;
        case 'clear': resetHistoryFilters(); break;
    }
}

function toggleHistoryText(btn, id) {
    const box = $('h-' + id);
    if (!box) return;
    const open = box.classList.toggle('is-open');
    btn.textContent = open ? 'Thu gọn' : 'Xem thêm';
}

// ===================== PHÂN TRANG =====================
function renderPagination(data) {
    const bar = $('paginationBar');
    if (!data || data.totalPage <= 1) {
        bar.innerHTML = '';
        return;
    }
    bar.innerHTML =
        '<button type="button" class="hx-btn outline" data-page="' + (data.page - 1) + '"' + (data.page <= 1 ? ' disabled' : '') + '><i class="fa-solid fa-chevron-left"></i>Trước</button>'
        + '<span>Trang <b>' + data.page + '</b> / ' + data.totalPage + '</span>'
        + '<button type="button" class="hx-btn outline" data-page="' + (data.page + 1) + '"' + (data.page >= data.totalPage ? ' disabled' : '') + '>Sau<i class="fa-solid fa-chevron-right"></i></button>';
}

function goToPage(page) {
    currentPage = page;
    loadHistory(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ===================== CHI TIẾT CA =====================
// Hộp thoại nằm ở fragments/ticket-detail-modal.jsp + js/ticket-detail.js (dùng chung nhiều trang)
function openDetail(id) {
    const item = itemsById[id];
    if (!item) return;
    TicketDetail.open(item, {
        reasonLabel: reasonInfo(item.archiveReason).label,
        showReport: canSubmitReport(item),
        onNotify: hxToast
    });
}

function showOverlay(id) {
    const o = $(id);
    o.classList.add('show');
    o.setAttribute('aria-hidden', 'false');
}

function hideOverlay(id) {
    const o = $(id);
    o.classList.remove('show');
    o.setAttribute('aria-hidden', 'true');
}

// ===================== XÁC NHẬN / THÔNG BÁO =====================
let confirmResolve = null;

// Trả về Promise<boolean>; thay cho confirm() của trình duyệt
function askConfirm(opts) {
    return new Promise(function (resolve) {
        confirmResolve = resolve;
        $('hxConfirmTitle').textContent = opts.title;
        $('hxConfirmSub').textContent = opts.subtitle || '';
        $('hxConfirmText').textContent = opts.message;
        const yes = $('hxConfirmYes');
        yes.textContent = opts.yesText || 'Xác nhận';
        yes.className = 'hx-btn ' + (opts.yesClass || 'red');
        showOverlay('hxConfirmOverlay');
        yes.focus();
    });
}

function answerConfirm(ok) {
    hideOverlay('hxConfirmOverlay');
    if (confirmResolve) { const r = confirmResolve; confirmResolve = null; r(ok); }
}
document.addEventListener('DOMContentLoaded', function () {
    $('hxConfirmYes').addEventListener('click', function () { answerConfirm(true); });
    $('hxConfirmNo').addEventListener('click', function () { answerConfirm(false); });
    $('hxConfirmOverlay').addEventListener('click', function (e) { if (e.target === this) answerConfirm(false); });
});

let toastTimer = null;
function hxToast(msg, isError) {
    let el = $('hxToast');
    if (!el) {
        el = document.createElement('div');
        el.id = 'hxToast';
        el.setAttribute('role', 'status');
        document.body.appendChild(el);
    }
    el.className = 'hx-toast' + (isError ? ' error' : '');
    el.textContent = msg;
    el.style.display = 'block';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.style.display = 'none'; }, isError ? 4200 : 2800);
}

// ===================== THAO TÁC =====================
// Đánh dấu thủ công "Đã nộp" (khi đã gửi form nhưng chưa xác nhận trong trang nộp báo cáo)
async function markReportSubmitted(id, btn) {
    const ok = await askConfirm({
        title: 'Đánh dấu đã nộp báo cáo',
        subtitle: nameOf(id),
        message: 'Đánh dấu ca này là ĐÃ NỘP báo cáo?',
        yesText: 'Đã nộp', yesClass: 'green'
    });
    if (!ok) return;
    if (btn) btn.disabled = true;
    try {
        await apiFetch(CTX + '/api/tickets/' + id + '/report-status', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'SUBMITTED' })
        });
        hxToast('Đã đánh dấu đã nộp báo cáo.');
        loadHistory(true);
    } catch (e) {
        if (btn) btn.disabled = false;
        hxToast(e.message, true);
    }
}

async function restoreTicket(id, btn) {
    const ok = await askConfirm({
        title: 'Đưa ca về danh sách',
        subtitle: nameOf(id),
        message: 'Đưa ca này về danh sách chính để xử lý tiếp?',
        yesText: 'Đưa về danh sách', yesClass: 'red'
    });
    if (!ok) return;
    if (btn) btn.disabled = true;
    try {
        await apiFetch(CTX + '/api/tickets/' + id + '/restore', { method: 'PUT' });
        hxToast('Đã đưa ca về danh sách chính.');
        loadHistory(true);
        loadTabCounts();
    } catch (e) {
        if (btn) btn.disabled = false;
        hxToast(e.message, true);
    }
}

function nameOf(id) {
    const it = itemsById[id];
    return it ? (it.customerName || '') + (it.subscriberAccount ? ' · ' + it.subscriberAccount : '') : '';
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
    return String(str === null || str === undefined ? '' : str)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
