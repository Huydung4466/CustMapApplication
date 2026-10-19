/*
 * appointments.js - Trang "Khách đã hẹn": các ca đã bấm Hẹn giờ / Hẹn khác.
 * Cần: CTX, app-common.js, Bootstrap. Biến ticketCache / hàm loadTickets được camera-capture.js sử dụng
 * (nút camera trên từng thẻ ca).
 */
let currentUser = null;
let ticketCache = {};
let currentPage = 1;
let searchTimer = null;
let actionModalInstance = null;
const PAGE_SIZE = 10;

document.addEventListener('DOMContentLoaded', async function () {
    actionModalInstance = new bootstrap.Modal(document.getElementById('actionModal'));
    await initUser();
    loadTickets();
});

async function initUser() {
    try {
        currentUser = await apiFetch(CTX + '/api/auth/me');
        document.getElementById('currentUserFullName').textContent = currentUser.fullName;
        // ADMIN xem được ca đã hẹn của từng nhân viên; STAFF chỉ thấy ca của chính mình
        if (currentUser.admin) {
            const staffs = await apiFetch(CTX + '/api/tickets/staffs');
            const sel = document.getElementById('staffFilterSelect');
            sel.innerHTML = '';
            const all = document.createElement('option');
            all.value = 'ALL';
            all.textContent = '-- Tất cả nhân viên --';
            sel.appendChild(all);
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

// keepPage = true: giữ trang hiện tại (sau khi thao tác trên thẻ); đổi bộ lọc thì về trang 1
async function loadTickets(keepPage) {
    if (keepPage !== true) currentPage = 1;
    const params = new URLSearchParams();
    const staffSel = document.getElementById('staffFilterSelect');
    if (currentUser && currentUser.admin && staffSel.value) params.set('staffFilter', staffSel.value);
    const kw = document.getElementById('keywordInput').value.trim();
    if (kw) params.set('keyword', kw);
    const st = document.getElementById('statusFilterSelect').value;
    if (st && st !== 'ALL') params.set('appStatus', st);
    params.set('page', currentPage);
    params.set('size', PAGE_SIZE);

    const box = document.getElementById('ticketListContainer');
    try {
        const data = await apiFetch(CTX + '/api/tickets/appointments?' + params.toString());
        currentPage = data.page;
        document.getElementById('ticketCountText').textContent = data.totalItem;
        renderTickets(data.resultList);
        renderPagination(data);
    } catch (e) {
        box.innerHTML = '<div class="alert alert-danger p-2 small">' + escapeHtml(e.message) + '</div>';
        document.getElementById('paginationBar').innerHTML = '';
    }
}

function debounceLoadTickets() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(function () { loadTickets(); }, 300);
}

function resetFilters() {
    document.getElementById('keywordInput').value = '';
    document.getElementById('statusFilterSelect').value = 'ALL';
    loadTickets();
}

function renderTickets(tickets) {
    const box = document.getElementById('ticketListContainer');
    ticketCache = {};
    (tickets || []).forEach(function (t) { ticketCache[t.id] = t; });
    renderReminderBanner(tickets || []);

    if (!tickets || tickets.length === 0) {
        box.innerHTML = '<div class="card border-0 shadow-sm rounded-4 p-4 text-center bg-white text-muted small">'
            + '<i class="fa-regular fa-calendar-check fs-3 mb-2 d-block"></i>Chưa có khách nào đã hẹn.<br>'
            + 'Khi bạn bấm <b>Hẹn giờ</b> hoặc <b>Hẹn khác</b> ở danh sách ca, khách sẽ chuyển sang đây.</div>';
        return;
    }
    box.innerHTML = tickets.map(cardHtml).join('');
}

function statusBadge(status) {
    if (status === 'HEN_KHAC') {
        return '<span class="status-badge status-hen-khac"><i class="fa-regular fa-calendar"></i>Hẹn khác</span>';
    }
    return '<span class="status-badge status-hen-gio"><i class="fa-solid fa-circle-check"></i>Hẹn giờ</span>';
}

function cardHtml(item) {
    const id = Number(item.id);
    const overdue = isOverdue(item);
    const dup = (item.duplicateCount && item.duplicateCount >= 2) ? '<span class="badge-dup">Trùng x' + Number(item.duplicateCount) + '</span>' : '';
    const priority = item.priorityLevel
        ? '<span class="badge ms-1" style="font-size:0.7rem;' + priorityStyle(item.priorityLevel) + '">' + escapeHtml(item.priorityLevel) + '</span>' : '';

    let apptClass = 'appt-strip', apptText, apptIcon = 'fa-regular fa-clock';
    if (item.appointmentTime) {
        apptText = escapeHtml(item.appointmentTime);
        if (overdue) { apptClass += ' is-overdue'; apptIcon = 'fa-solid fa-triangle-exclamation'; }
    } else {
        apptClass += ' is-empty';
        apptText = 'Chưa đặt giờ hẹn';
    }
    const apptTag = overdue ? '<span class="small">Quá hẹn</span>' : '';

    let kmz = '';
    if (item.cabinetCode) {
        kmz = item.googleMapsUrl
            ? '<a href="' + escapeHtml(item.googleMapsUrl) + '" target="_blank" rel="noopener" class="btn-kmz-map has-kmz"><i class="fa-solid fa-map-pin"></i>Tủ: ' + escapeHtml(item.cabinetCode) + '</a>'
            : '<span class="btn-kmz-map no-kmz"><i class="fa-solid fa-server"></i>Tủ: ' + escapeHtml(item.cabinetCode) + ' (Chưa có KMZ)</span>';
    }

    const complaint = escapeHtml(item.complaintContent || 'Không có nội dung');
    const isLong = complaint.length > 90;

    let phones = '';
    if (item.contactPhone) {
        phones += '<a href="tel:' + escapeHtml(item.contactPhone) + '" class="btn-call-phone"><i class="fa-solid fa-phone"></i>Gọi KH (' + escapeHtml(item.contactPhone) + ')</a>';
    }
    if (item.reportPhone && item.reportPhone !== item.contactPhone) {
        phones += '<a href="tel:' + escapeHtml(item.reportPhone) + '" class="btn-call-phone btn-call-report"><i class="fa-solid fa-phone-volume"></i>Báo cáo</a>';
    }

    return '<div class="ticket-card ' + (overdue ? 'overdue' : '') + '">'
        + '<div class="card-header-row"><div>'
        +   '<span class="customer-title">' + escapeHtml(item.customerName || 'Khách hàng') + '</span>' + priority
        + '</div><div>' + statusBadge(item.appStatus) + '</div></div>'
        + '<div class="' + apptClass + '"><span><i class="' + apptIcon + ' me-1"></i>' + apptText + '</span>' + apptTag + '</div>'
        + '<div class="meta-info-row"><div><span class="account-tag">' + escapeHtml(item.subscriberAccount || item.ticketCode) + '</span>' + dup + '</div>'
        +   '<span class="text-muted" style="font-size:0.78rem;">PA: #' + escapeHtml(item.ticketCode) + '</span></div>'
        + '<div class="location-section">'
        +   '<div><i class="fa-solid fa-location-dot text-danger me-1"></i>' + escapeHtml(item.installAddress || 'Chưa có địa chỉ') + '</div>'
        +   (kmz ? '<div>' + kmz + '</div>' : '')
        + '</div>'
        + '<div class="complaint-box"><div class="' + (isLong ? 'complaint-collapsed' : '') + '" id="c-' + id + '">' + complaint + '</div>'
        +   (isLong ? '<a class="see-more-link" onclick="toggleComplaint(' + id + ')">...xem thêm</a>' : '')
        +   (item.callNote ? '<div class="text-muted fst-italic mt-1" style="font-size:0.78rem;"><i class="fa-regular fa-note-sticky me-1"></i>' + escapeHtml(item.callNote) + '</div>' : '')
        + '</div>'
        + '<div class="action-buttons-grid">' + phones
        +   '<div class="action-right">'
        +     '<button type="button" class="btn-camera" title="Chụp ảnh hiện trường" onclick="openCamera(' + id + ')"><i class="fa-solid fa-camera"></i>'
        +       (item.photoCount > 0 ? '<span class="photo-count">' + Number(item.photoCount) + '</span>' : '') + '</button>'
        +     '<button type="button" class="btn-action-trigger" onclick="openModal(' + id + ')"><i class="fa-regular fa-pen-to-square"></i>Xử lý</button>'
        +   '</div>'
        + '</div>'
        + '<div class="quick-toolbar">'
        +   '<button type="button" class="btn-quick-item" onclick="quickBackToList(' + id + ')"><i class="fa-solid fa-rotate-left"></i>Bỏ hẹn</button>'
        +   '<button type="button" class="btn-quick-item" onclick="quickKLL(' + id + ')"><i class="fa-solid fa-phone-slash"></i>KLL</button>'
        +   '<button type="button" class="btn-quick-item" onclick="quickDone(' + id + ')"><i class="fa-solid fa-check"></i>Hoàn tất</button>'
        + '</div>'
        + '<div class="card-footer-info"><span>Khu vực: <b>' + escapeHtml(item.area || '') + '</b></span>'
        +   '<span>Phụ trách: <b class="text-dark">' + escapeHtml(item.techStaffName || 'Chưa phân công') + '</b></span></div>'
        + '</div>';
}

function toggleComplaint(id) {
    const el = document.getElementById('c-' + id);
    if (el) el.classList.toggle('complaint-collapsed');
}

// ---------- Banner nhắc: ca sắp đến giờ (trong 60 phút) và ca quá giờ, tính trên danh sách đang hiển thị ----------
function renderReminderBanner(tickets) {
    const box = document.getElementById('reminderBanner');
    const now = Date.now();
    const soon = [];
    let overdueCount = 0;
    tickets.forEach(function (t) {
        if (!t.appointmentStart) return;
        const start = new Date(t.appointmentStart).getTime();
        const end = new Date(t.appointmentEnd || t.appointmentStart).getTime();
        if (end < now) overdueCount++;
        else if (start - now <= 60 * 60 * 1000) soon.push(t);
    });
    let html = '';
    if (soon.length > 0) {
        soon.sort(function (a, b) { return new Date(a.appointmentStart) - new Date(b.appointmentStart); });
        const names = soon.slice(0, 4).map(function (t) {
            const d = new Date(t.appointmentStart);
            return escapeHtml(t.customerName || 'Khách hàng') + ' (' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ')';
        }).join(', ');
        html += '<div class="reminder-banner alert-warning border border-warning-subtle"><i class="fa-regular fa-bell me-1"></i><b>'
            + soon.length + ' ca sắp đến giờ hẹn:</b> ' + names + (soon.length > 4 ? '...' : '') + '</div>';
    }
    if (overdueCount > 0) {
        html += '<div class="reminder-banner alert-danger border border-danger-subtle"><i class="fa-solid fa-triangle-exclamation me-1"></i><b>'
            + overdueCount + ' ca đã quá giờ hẹn</b> (viền đỏ), hãy gọi lại hoặc cập nhật trạng thái.</div>';
    }
    box.innerHTML = html;
}

// ---------- Phân trang ----------
function renderPagination(data) {
    const bar = document.getElementById('paginationBar');
    if (!data || data.totalPage <= 1) { bar.innerHTML = ''; return; }
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
    loadTickets(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ---------- Cập nhật ca ----------
async function sendTicketAction(id, payload, successMessage) {
    try {
        await apiFetch(CTX + '/api/tickets/' + id + '/action', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (successMessage) showToast(successMessage);
        return true;
    } catch (e) {
        showToast(e.message, true);
        return false;
    }
}

function openModal(id) {
    const item = ticketCache[id] || {};
    document.getElementById('modalTicketId').value = id;
    document.getElementById('modalStatusSelect').value = item.appStatus || 'HEN_GIO';
    document.getElementById('modalApptStart').value = toInputValue(item.appointmentStart);
    document.getElementById('modalApptEnd').value = toInputValue(item.appointmentEnd);
    document.getElementById('modalCallNote').value = item.callNote || '';
    actionModalInstance.show();
}

async function submitModalAction() {
    const id = document.getElementById('modalTicketId').value;
    const status = document.getElementById('modalStatusSelect').value;
    const startVal = document.getElementById('modalApptStart').value;
    const endVal = document.getElementById('modalApptEnd').value;
    if (endVal && !startVal) {
        return showToast('Hãy chọn giờ bắt đầu hẹn trước.', true);
    }
    const payload = { appStatus: status, callNote: document.getElementById('modalCallNote').value };
    if (status === 'DANG_XU_LY') {
        // Bỏ hẹn: đưa ca về danh sách chính và xóa giờ hẹn
        payload.clearAppointment = true;
    } else if (startVal) {
        payload.appointmentStart = startVal;
        payload.appointmentEnd = endVal || null;
    } else if (ticketCache[id] && ticketCache[id].appointmentStart) {
        payload.clearAppointment = true;
    }
    let message = 'Đã cập nhật ca';
    if (status === 'DANG_XU_LY') message = 'Đã bỏ hẹn, ca quay về danh sách chính';
    else if (status === 'KLL' || status === 'DA_XU_LY') message = 'Đã chuyển ca vào Lịch sử';
    if (await sendTicketAction(id, payload, message)) {
        actionModalInstance.hide();
    }
    loadTickets(true);
}

async function quickBackToList(id) {
    if (!confirm('Bỏ hẹn khách này và đưa về danh sách ca chính?')) return;
    await sendTicketAction(id, { appStatus: 'DANG_XU_LY', clearAppointment: true }, 'Đã bỏ hẹn, ca quay về danh sách chính');
    loadTickets(true);
}

async function quickKLL(id) {
    if (!confirm('Không liên lạc được khách hàng này? Ca sẽ được chuyển vào Lịch sử.')) return;
    await sendTicketAction(id, { appStatus: 'KLL', callNote: 'Không liên lạc được' }, 'Đã chuyển ca vào Lịch sử');
    loadTickets(true);
}

async function quickDone(id) {
    if (!confirm('Đã xử lý xong ca này? Ca sẽ được chuyển vào Lịch sử.')) return;
    await sendTicketAction(id, { appStatus: 'DA_XU_LY' }, 'Đã chuyển ca vào Lịch sử');
    loadTickets(true);
}
