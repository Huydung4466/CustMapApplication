let currentUser = null;
let searchTimer = null;
let actionModalInstance = null;

document.addEventListener('DOMContentLoaded', async () => {
    actionModalInstance = new bootstrap.Modal(document.getElementById('actionModal'));
    await initCurrentUserAndStaffFilter();
    // await loadFilterDropdowns();
    await loadKmzCount();
    await loadTickets();
});

async function initCurrentUserAndStaffFilter() {
    try {
        const res = await fetch(`${CTX}/api/auth/me`);
        currentUser = await res.json();
        // Chỉ ADMIN mới thấy ô import XLSX; STAFF chỉ thấy ô KMZ (full chiều ngang)
        if (currentUser.admin) {
            document.getElementById('excelImportCol').classList.remove('d-none');
        } else {
            document.getElementById('kmzImportCol').classList.replace('col-md-6', 'col-md-12');
        }
        document.getElementById('currentUserFullName').textContent = `${currentUser.fullName}`;

        const staffSelect = document.getElementById('staffFilterSelect');
        staffSelect.innerHTML = '';

        if (currentUser.admin) {
            const staffRes = await fetch(`${CTX}/api/tickets/staffs`);
            const staffs = await staffRes.json();
            const optAll = document.createElement('option');
            optAll.value = 'ALL';
            optAll.textContent = '-- Tất cả nhân viên --';
            staffSelect.appendChild(optAll);

            staffs.forEach(name => {
                const opt = document.createElement('option');
                opt.value = name;
                opt.textContent = `NV: ${name}`;
                if (name.trim().toUpperCase() === currentUser.fullName.trim().toUpperCase()) opt.selected = true;
                staffSelect.appendChild(opt);
            });
        } else {
            const opt = document.createElement('option');
            opt.value = currentUser.fullName;
            opt.textContent = `Địa bàn: ${currentUser.fullName}`;
            opt.selected = true;
            staffSelect.appendChild(opt);
            staffSelect.disabled = true;
        }
        // Load danh sách khu vực phù hợp với nhân viên đang chọn ban đầu
        await loadAreaDropdownForStaff(staffSelect.value);
        await loadCabinetDropdownForStaff(staffSelect.value, '');
    } catch (e) { console.error(e); }
}

// Chỉ load các khu vực mà nhân viên đó quản lý (VD: Long chỉ có Buôn Hồ và Cư Mgar)
async function loadAreaDropdownForStaff(staffName) {
    try {
        const res = await fetch(`${CTX}/api/tickets/areas?staffName=${encodeURIComponent(staffName)}`);
        const areas = await res.json();
        const areaSelect = document.getElementById('areaFilterSelect');
        areaSelect.innerHTML = '<option value="">-- Tất cả Khu vực của NV --</option>';
        areas.forEach(area => {
            if (area) {
                const opt = document.createElement('option');
                opt.value = area;
                opt.textContent = `Khu vực: ${area}`;
                areaSelect.appendChild(opt);
            }
        });
    } catch (e) { console.error(e); }
}

async function loadCabinetDropdownForStaff(staffName, area) {
    try {
        const res = await fetch(`${CTX}/api/tickets/cabinet-prefixes?staffName=${encodeURIComponent(staffName || '')}&area=${encodeURIComponent(area || '')}`);
        const prefixes = await res.json();
        const sel = document.getElementById('cabinetPrefixInput');
        sel.innerHTML = '<option value="">-- Tất cả --</option>';
        prefixes.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p;
            opt.textContent = p;
            sel.appendChild(opt);
        });
    } catch (e) { console.error(e); }
}

async function onStaffChange() {
    const staff = document.getElementById('staffFilterSelect').value;
    await Promise.all([loadAreaDropdownForStaff(staff), loadCabinetDropdownForStaff(staff, '')]);
    loadTickets();
}

async function onAreaChange() {
    const staff = document.getElementById('staffFilterSelect').value;
    const area = document.getElementById('areaFilterSelect').value;
    await loadCabinetDropdownForStaff(staff, area);
    loadTickets();
}

async function loadKmzCount() {
    try {
        const res = await fetch(`${CTX}/api/kmz/count`);
        const data = await res.json();
        document.getElementById('kmzTotalCount').textContent = data.totalLocations || 0;
    } catch (e) { console.error(e); }
}

async function loadTickets() {
    const keyword = document.getElementById('keywordInput').value.trim();
    const staffFilter = document.getElementById('staffFilterSelect').value;
    const statusVal = document.getElementById('statusFilterSelect').value;
    const area = document.getElementById('areaFilterSelect').value;
    const cabinetPrefix = document.getElementById('cabinetPrefixInput').value.trim();
    const priority = document.getElementById('priorityFilterSelect').value;

    let appStatus = 'ALL', onlyPinned = false;
    if (statusVal === 'PINNED') onlyPinned = true;
    else appStatus = statusVal;

    const params = new URLSearchParams({
        staffFilter: staffFilter || '',
        keyword,
        appStatus,
        onlyPinned,
        area: area || '',
        cabinetPrefix: cabinetPrefix || '',
        priority: priority || ''
    });

    const container = document.getElementById('ticketListContainer');
    try {
        const res = await fetch(`${CTX}/api/tickets?${params.toString()}`);
        const tickets = await res.json();
        document.getElementById('ticketCountText').textContent = tickets.length;
        renderTicketCards(tickets);
    } catch (e) {
        container.innerHTML = `<div class="alert alert-danger p-2 small">Lỗi tải dữ liệu</div>`;
    }
}

function debounceLoadTickets() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(loadTickets, 300);
}

async function resetFilters() {
    document.getElementById('keywordInput').value = '';
    document.getElementById('areaFilterSelect').value = '';
    document.getElementById('priorityFilterSelect').value = '';
    document.getElementById('statusFilterSelect').value = 'ALL';
    await loadCabinetDropdownForStaff(document.getElementById('staffFilterSelect').value, '');
    document.getElementById('cabinetPrefixInput').value = '';
    loadTickets();
}

function renderTicketCards(tickets) {
    const container = document.getElementById('ticketListContainer');
    if (!tickets || tickets.length === 0) {
        container.innerHTML = `<div class="card border-0 shadow-sm rounded-4 p-4 text-center bg-white text-muted small">Không tìm thấy ca phản ánh phù hợp với bộ lọc</div>`;
        return;
    }

    container.innerHTML = tickets.map(item => {
        const statusBadge = renderStatusBadge(item.appStatus);
        const dupBadge = (item.duplicateCount && item.duplicateCount >= 2) ? `<span class="badge-dup">Trùng x${item.duplicateCount}</span>` : '';
        const appTime = (item.appointmentTime) ? `<div class="text-success small fw-bold mb-1"><i class="fa-regular fa-clock me-1"></i>${escapeHtml(item.appointmentTime)}</div>` : '';

        let kmzHtml = '';
        if (item.cabinetCode) {
            if (item.googleMapsUrl) {
                kmzHtml = `<a href="${item.googleMapsUrl}" target="_blank" class="btn-kmz-map has-kmz"><i class="fa-solid fa-map-pin"></i>Tủ: ${escapeHtml(item.cabinetCode)}</a>`;
            } else {
                kmzHtml = `<span class="btn-kmz-map no-kmz"><i class="fa-solid fa-server"></i>Tủ: ${escapeHtml(item.cabinetCode)} (Chưa có KMZ)</span>`;
            }
        }

        const complaint = escapeHtml(item.complaintContent || 'Không có nội dung');
        const isLong = complaint.length > 90;

        let phoneButtons = '';
        if (item.contactPhone) {
            phoneButtons += `<a href="tel:${item.contactPhone}" class="btn-call-phone"><i class="fa-solid fa-phone"></i>Gọi KH (${item.contactPhone})</a>`;
        }
        if (item.reportPhone && item.reportPhone !== item.contactPhone) {
            phoneButtons += `<a href="tel:${item.reportPhone}" class="btn-call-phone btn-call-report"><i class="fa-solid fa-phone-volume"></i>Báo cáo</a>`;
        }

        const isPinned = item.isPinned === true;

        return `
        <div class="ticket-card">
            <div class="card-header-row">
                <div>
                    <span class="customer-title">${escapeHtml(item.customerName || 'Khách hàng')}</span>
                    ${item.priorityLevel ? `<span class="badge bg-light text-secondary border ms-1 vt-fs-70">${escapeHtml(item.priorityLevel)}</span>` : ''}
    </div>
        <div>${statusBadge}</div>
    </div>

        <div class="meta-info-row">
            <div>
                <span class="account-tag">${escapeHtml(item.subscriberAccount || item.ticketCode)}</span>
                ${dupBadge}
            </div>
            <span class="text-muted vt-fs-78">PA: #${escapeHtml(item.ticketCode)}</span>
        </div>

        ${appTime}

        <div class="location-section">
            <div><i class="fa-solid fa-location-dot text-danger me-1"></i>${escapeHtml(item.installAddress || 'Chưa có địa chỉ')}</div>
            <div>${kmzHtml}</div>
        </div>

        <div class="complaint-box">
            <div class="${isLong ? 'complaint-collapsed' : ''}" id="c-${item.id}">${complaint}</div>
            ${isLong ? `<a class="see-more-link" onclick="toggleC(${item.id})">...xem thêm</a>` : ''}
            ${item.callNote ? `<div class="text-muted fst-italic mt-1 vt-fs-78"><i class="fa-regular fa-note-sticky me-1"></i>${escapeHtml(item.callNote)}</div>` : ''}
        </div>

        <div class="action-buttons-grid">
            ${phoneButtons}
            <button type="button" class="btn-action-trigger ms-auto" onclick="openModal(${item.id}, '${item.appStatus}')">
                <i class="fa-regular fa-pen-to-square"></i>Xử lý
            </button>
        </div>

        <div class="quick-toolbar">
            <button type="button" class="btn-quick-item ${item.appStatus==='HEN_GIO'?'active-hengio':''}" onclick="openModal(${item.id}, 'HEN_GIO')">
                <i class="fa-regular fa-clock"></i>Hẹn giờ
            </button>
            <button type="button" class="btn-quick-item ${item.appStatus==='KLL'?'active-kll':''}" onclick="quickKLL(${item.id})">
                <i class="fa-solid fa-phone-slash"></i>KLL
            </button>
            <button type="button" class="btn-quick-item" onclick="openModal(${item.id}, 'HEN_KHAC')">
                <i class="fa-regular fa-calendar"></i>Khác
            </button>
            <button type="button" class="btn-quick-item ${isPinned?'pinned':''}" onclick="togglePin(${item.id}, ${!isPinned})">
                <i class="fa-solid fa-star text-warning"></i>${isPinned?'Đã ghim':'Ghim'}
            </button>
        </div>

        <div class="card-footer-info">
            <span>Khu vực: <b>${escapeHtml(item.area || '')}</b></span>
            <span>Phụ trách: <b class="text-dark">${escapeHtml(item.techStaffName || 'Chưa phân công')}</b></span>
        </div>
    </div>`;
    }).join('');
}

function toggleC(id) {
    const el = document.getElementById(`c-${id}`);
    el.classList.toggle('complaint-collapsed');
}

function renderStatusBadge(status) {
    switch(status) {
        case 'HEN_GIO': return `<span class="status-badge status-hen-gio"><i class="fa-solid fa-circle-check"></i>Hẹn giờ</span>`;
        case 'KLL': return `<span class="status-badge status-kll"><i class="fa-solid fa-phone-slash"></i>KLL</span>`;
        case 'DA_XU_LY': return `<span class="status-badge status-da-xu-ly"><i class="fa-solid fa-check"></i>Đã xử lý</span>`;
        default: return `<span class="status-badge status-dang-xu-ly"><i class="fa-solid fa-spinner fa-spin"></i>Đang xử lý</span>`;
    }
}

function openModal(id, status) {
    document.getElementById('modalTicketId').value = id;
    document.getElementById('modalStatusSelect').value = status || 'DANG_XU_LY';
    actionModalInstance.show();
}

async function submitModalAction() {
    const id = document.getElementById('modalTicketId').value;
    const payload = {
        appStatus: document.getElementById('modalStatusSelect').value,
        appointmentTime: document.getElementById('modalAppointmentTime').value,
        callNote: document.getElementById('modalCallNote').value
    };
    await fetch(`${CTX}/api/tickets/${id}/action`, {
        method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    actionModalInstance.hide();
    loadTickets();
}

async function quickKLL(id) {
    await fetch(`${CTX}/api/tickets/${id}/action`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ appStatus: 'KLL', callNote: 'Không liên lạc được' })
    });
    loadTickets();
}

async function togglePin(id, isPinned) {
    await fetch(`${CTX}/api/tickets/${id}/action`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isPinned })
    });
    loadTickets();
}

async function uploadExcelFile() {
    const file = document.getElementById('excelFileInput').files[0];
    if (!file) return alert('Chọn file Excel!');
    const fd = new FormData(); fd.append('file', file);
    const res = await fetch(`${CTX}/api/tickets/import`, { method: 'POST', body: fd });
    const d = await res.json();
    document.getElementById('importResultAlert').innerHTML = `<div class="alert alert-success py-1 small">${d.message}</div>`;
    loadTickets();
}

async function uploadKmzFile() {
    const file = document.getElementById('kmzFileInput').files[0];
    if (!file) return alert('Chọn file KMZ!');
    const fd = new FormData(); fd.append('file', file);
    const res = await fetch(`${CTX}/api/kmz/import`, { method: 'POST', body: fd });
    const d = await res.json();
    document.getElementById('importResultAlert').innerHTML = `<div class="alert alert-primary py-1 small">${d.message}</div>`;
    loadKmzCount();
    loadTickets();
}

async function apiFetch(url, options) {
    const res = await fetch(url, options);
    // Hết phiên đăng nhập: server chuyển hướng sang trang login
    if (res.redirected && res.url.includes('/login')) {
        window.location.href = CTX + '/login';
        throw new Error('Phiên đăng nhập đã hết hạn');
    }
    if (!res.ok) {
        let msg = 'Có lỗi xảy ra (mã ' + res.status + ')';
        if (res.status === 413) {
            msg = 'File quá lớn, vượt giới hạn cho phép';
        } else {
            try {
                const err = await res.json();
                msg = err.message || err.detail || msg;
            } catch (_) { /* không có JSON */ }
        }
        throw new Error(msg);
    }
    return res.json();
}

async function sendTicketAction(id, payload) {
    try {
        await apiFetch(`${CTX}/api/tickets/${id}/action`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        return true;
    } catch (e) {
        alert(e.message);
        return false;
    }
}

function escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
