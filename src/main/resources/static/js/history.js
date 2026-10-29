let currentUser = null;
let searchTimer = null;
let currentPage = 1;
const PAGE_SIZE = 10;
// Trang lịch sử đang xem: COMPLETED (đã hoàn thành) hoặc ISSUES (không liên lạc được & khác), do server gán
let meta = null; // trạng thái / lý do lưu trữ / các trang lịch sử, lấy từ /api/tickets/meta (enum phía server)

// ===================== NỘP BÁO CÁO =====================
// Nút "Nộp báo cáo" mở trang trung gian /report-form/{id} (xem thông tin ca, chọn nhóm nhiệm vụ, chọn / lưu mẫu nội dung)
// rồi mới sang Google Form. Phần dựng link Google Form nằm trong js/report-form.js.
// Chỉ ca "Đã xử lý" (nằm trang Đã hoàn thành) mới có nút nộp báo cáo; ca KLL / không còn trong file thì không
function canSubmitReport(item) {
    return !item.archiveReason || item.archiveReason === 'DONE';
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

        // Huy hiệu nộp báo cáo cạnh huy hiệu "Đã xử lý" (chỉ ca nộp được báo cáo); nhãn / màu do server gửi (ReportStatus)
        const canReport = canSubmitReport(item);
        const reportDone = item.reportStatus === 'SUBMITTED';
        const reportBadge = canReport
            ? '<span class="status-badge ' + escapeHtml(item.reportStatusBadge || 'report-not-submitted') + '"><i class="'
                + escapeHtml(item.reportStatusIcon || 'fa-regular fa-file') + '"></i>' + escapeHtml(item.reportStatusLabel || 'Chưa nộp') + '</span>'
            : '';

        const complaint = escapeHtml(item.complaintContent || 'Không có nội dung');
        const staffInfo = (currentUser && currentUser.admin)
            ? '<span>NV: <b class="text-dark">' + escapeHtml(item.assignedStaff || '') + '</b></span>' : '<span></span>';

        return '<div class="ticket-card history-card">'
            + '<div class="card-header-row"><div>'
            +   '<span class="customer-title">' + escapeHtml(item.customerName || 'Khách hàng') + '</span>'
            + '</div><div class="d-flex flex-wrap justify-content-end gap-1">' + reasonBadge + reportBadge + '</div></div>'
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
            +   (canReport
                ? '<a class="btn-report-submit' + (reportDone ? ' is-done' : '') + ' ms-auto" href="' + CTX + '/report-form/' + item.id + '"><i class="fa-solid fa-file-signature"></i>'
                    + (reportDone ? 'Nộp lại' : 'Nộp báo cáo') + '</a>'
                    + (reportDone ? '' : '<button type="button" class="btn-report-mark" title="Đã nộp rồi? Bấm để đánh dấu đã nộp" onclick="markReportSubmitted(' + item.id + ')"><i class="fa-solid fa-file-circle-check"></i></button>')

                ? '<a class="btn-report-submit ms-auto" href="' + CTX + '/report-form/' + item.id + '"><i class="fa-solid fa-file-signature"></i>Nộp báo cáo</a>'

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

// Đánh dấu thủ công "Đã nộp" (khi đã gửi form nhưng chưa xác nhận trong trang nộp báo cáo)
async function markReportSubmitted(id) {
    if (!confirm('Đánh dấu ca này là ĐÃ NỘP báo cáo?')) return;
    try {
        await apiFetch(CTX + '/api/tickets/' + id + '/report-status', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'SUBMITTED' })
        });
        loadHistory(true);
    } catch (e) {
        alert(e.message);
    }
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
