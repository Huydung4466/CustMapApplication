/*
 * appointments.js - Trang "Khách đã hẹn": các ca đã bấm Hẹn giờ / Hẹn khác.
 * Thẻ khách vẽ bởi js/ticket-card.js (dùng chung với dashboard). Cần: CTX, app-common.js, ticket-card.js, Bootstrap. Biến ticketCache / hàm loadTickets được camera-capture.js sử dụng
 * (nút camera trên từng thẻ ca).
 */
let currentUser = null;
let ticketCache = {};
let currentPage = 1;
let searchTimer = null;
let actionModalInstance = null;
const PAGE_SIZE = 10;
let statusMeta = { statuses: [], statusOptions: {}, archiveReasons: [], historyTabs: [] }; // lấy từ /api/tickets/meta

document.addEventListener('DOMContentLoaded', async function () {
    actionModalInstance = new bootstrap.Modal(document.getElementById('actionModal'));
    await loadStatusMeta();
    await initUser();
    loadTickets();
});

// Trạng thái xử lý (nhãn, biểu tượng, màu, các lựa chọn trong hộp "Cập nhật lịch hẹn") do server khai báo
// bằng enum TicketStatus + StatusContext; giao diện chỉ việc đọc từ /api/tickets/meta
async function loadStatusMeta() {
    try {
        statusMeta = await apiFetch(CTX + '/api/tickets/meta');
        const sel = document.getElementById('modalStatusSelect');
        sel.innerHTML = '';
        (statusMeta.statusOptions.APPOINTMENT || []).forEach(function (o) {
            const opt = document.createElement('option');
            opt.value = o.code;
            opt.textContent = o.label;
            sel.appendChild(opt);
        });
    } catch (e) { console.error(e); }
}

function findStatus(code) {
    return statusMeta.statuses.find(function (s) { return s.code === code; });
}

// Thông báo sau khi chuyển ca vào Lịch sử, kèm tên trang Lịch sử chứa ca đó (Đã hoàn thành / Không liên lạc được & khác)
function archiveMessage(statusCode) {
    const info = findStatus(statusCode);
    const reason = info && statusMeta.archiveReasons.find(function (r) { return r.code === info.archiveReason; });
    const tab = reason && statusMeta.historyTabs.find(function (t) { return t.code === reason.tab; });
    return 'Đã chuyển ca vào Lịch sử' + (tab ? ' › ' + tab.label : '');
}

async function initUser() {
    try {
        currentUser = await AppNav.getMe();   // thanh trên cùng (top-navbar) đã lấy sẵn và hiện tên
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
    // Trang này chỉ có ca đã hẹn; trạng thái lạ coi như Hẹn giờ
    const info = findStatus(status) || findStatus('HEN_GIO');
    if (!info) return '';
    return '<span class="status-badge ' + escapeHtml(info.badgeClass) + '"><i class="' + escapeHtml(info.icon) + '"></i>'
        + escapeHtml(info.label) + '</span>';
}

// Phần thông tin khách vẽ bởi TicketCard (dùng chung với dashboard nên hai trang luôn giống nhau, gồm cả ca hạ tầng);
// ở đây chỉ khai báo phần riêng của trang Đã hẹn: dải giờ hẹn, nút Xử lý và thanh Bỏ hẹn / KLL / Hoàn tất
function cardHtml(item) {
    const id = Number(item.id);
    const overdue = isOverdue(item);

    let apptClass = 'appt-strip', apptText, apptIcon = 'fa-regular fa-clock';
    if (item.appointmentTime) {
        apptText = escapeHtml(item.appointmentTime);
        if (overdue) { apptClass += ' is-overdue'; apptIcon = 'fa-solid fa-triangle-exclamation'; }
    } else {
        apptClass += ' is-empty';
        apptText = 'Chưa đặt giờ hẹn';
    }
    const apptTag = overdue ? '<span class="small">Quá hẹn</span>' : '';

    return TicketCard.build(item, {
        cardClass: overdue ? 'overdue' : '',
        statusBadge: statusBadge(item.appStatus),
        apptHtml: '<div class="' + apptClass + '"><span><i class="' + apptIcon + ' me-1"></i>' + apptText + '</span>' + apptTag + '</div>',
        apptFirst: true,
        processCall: function (it) { return 'openModal(' + Number(it.id) + ')'; },
        toolbarHtml: '<div class="quick-toolbar">'
            + '<button type="button" class="btn-quick-item" onclick="quickBackToList(' + id + ')"><i class="fa-solid fa-rotate-left"></i>Bỏ hẹn</button>'
            + '<button type="button" class="btn-quick-item" onclick="quickKLL(' + id + ')"><i class="fa-solid fa-phone-slash"></i>KLL</button>'
            + '<button type="button" class="btn-quick-item" onclick="quickDone(' + id + ')"><i class="fa-solid fa-check"></i>Hoàn tất</button>'
            + '</div>'
    });
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
        if (window.BottomNav) BottomNav.refreshAppointments();   // số đếm trên tab "Đã hẹn" (thanh dưới)
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
    else if (findStatus(status) && findStatus(status).archives) message = archiveMessage(status);
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
    await sendTicketAction(id, { appStatus: 'KLL', callNote: 'Không liên lạc được' }, archiveMessage('KLL'));
    loadTickets(true);
}

async function quickDone(id) {
    if (!confirm('Đã xử lý xong ca này? Ca sẽ được chuyển vào Lịch sử.')) return;
    await sendTicketAction(id, { appStatus: 'DA_XU_LY' }, archiveMessage('DA_XU_LY'));
    loadTickets(true);
}
