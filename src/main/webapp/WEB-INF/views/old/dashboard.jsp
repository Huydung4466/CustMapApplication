<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Hệ Thống Quản Lý Phản Ánh Khách Hàng</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --primary-color: #2563eb;
            --success-color: #16a34a;
            --warning-color: #d97706;
            --danger-color: #dc2626;
            --bg-body: #f8fafc;
            --card-border: #e2e8f0;
        }

        body {
            background-color: var(--bg-body);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            color: #1e293b;
            padding-bottom: 70px;
        }

        .top-navbar {
            background: #ffffff;
            border-bottom: 1px solid var(--card-border);
            position: sticky;
            top: 0;
            z-index: 1030;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }

        .container-app {
            max-width: 740px;
            margin: 0 auto;
            padding: 0 10px;
        }

        /* Thẻ khách hàng */
        .ticket-card {
            background: #ffffff;
            border: 1px solid var(--card-border);
            border-radius: 14px;
            padding: 14px 16px;
            margin-bottom: 14px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.03);
            transition: all 0.2s ease;
        }
        .ticket-card:hover {
            box-shadow: 0 4px 14px rgba(37,99,235,0.08);
            border-color: #cbd5e1;
        }

        .card-header-row {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            gap: 8px;
            margin-bottom: 6px;
        }
        .customer-title {
            font-size: 1.1rem;
            font-weight: 700;
            color: #0f172a;
        }
        .badge-viettel {
            background: #fef2f2;
            color: #ef4444;
            font-size: 0.72rem;
            font-weight: 700;
            padding: 2px 7px;
            border-radius: 4px;
            border: 1px solid #fee2e2;
            vertical-align: middle;
        }

        .status-badge {
            font-size: 0.78rem;
            font-weight: 600;
            padding: 4px 10px;
            border-radius: 6px;
            display: inline-flex;
            align-items: center;
            gap: 5px;
            white-space: nowrap;
        }
        .status-dang-xu-ly { background: #eff6ff; color: #1d4ed8; border: 1px solid #dbeafe; }
        .status-hen-gio    { background: #f0fdf4; color: #15803d; border: 1px solid #dcfce7; }
        .status-kll        { background: #fffbeb; color: #b45309; border: 1px solid #fef3c7; }
        .status-da-xu-ly   { background: #f8fafc; color: #64748b; border: 1px solid #e2e8f0; }

        .meta-info-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 0.85rem;
            margin-bottom: 8px;
        }
        .account-tag {
            background: #f1f5f9;
            color: #334155;
            font-family: monospace;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 4px;
        }
        .badge-dup {
            background: #ef4444;
            color: #fff;
            font-size: 0.72rem;
            font-weight: 700;
            padding: 1px 6px;
            border-radius: 4px;
            margin-left: 5px;
        }

        .location-section {
            background: #f8fafc;
            border-radius: 8px;
            padding: 8px 10px;
            margin-bottom: 10px;
            font-size: 0.88rem;
            display: flex;
            flex-direction: column;
            gap: 6px;
        }
        .btn-kmz-map {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            font-size: 0.76rem;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 6px;
            text-decoration: none;
            width: fit-content;
        }
        .has-kmz { background: #dbeafe; color: #1d4ed8; border: 1px solid #bfdbfe; }
        .no-kmz  { background: #f1f5f9; color: #64748b; border: 1px dashed #cbd5e1; }

        .complaint-box {
            font-size: 0.88rem;
            color: #334155;
            line-height: 1.4;
            margin-bottom: 10px;
            border-left: 3px solid #3b82f6;
            padding-left: 8px;
        }
        .complaint-collapsed {
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }
        .see-more-link {
            font-size: 0.8rem;
            color: #2563eb;
            font-weight: 600;
            cursor: pointer;
            text-decoration: none;
        }

        .action-buttons-grid {
            display: grid;
            grid-template-columns: 1fr 1fr auto;
            gap: 6px;
            margin-bottom: 10px;
        }
        .btn-call-phone {
            background: #15803d;
            color: #fff;
            border: none;
            border-radius: 8px;
            padding: 7px 10px;
            font-size: 0.82rem;
            font-weight: 600;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 5px;
            text-decoration: none;
            white-space: nowrap;
        }
        .btn-call-phone:hover { background: #166534; color: #fff; }
        .btn-call-report { background: #0f766e; }
        .btn-call-report:hover { background: #115e59; }

        .btn-action-trigger {
            background: #2563eb;
            color: #fff;
            border: none;
            border-radius: 8px;
            padding: 7px 14px;
            font-size: 0.85rem;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 5px;
            white-space: nowrap;
        }
        .btn-action-trigger:hover { background: #1d4ed8; }

        .quick-toolbar {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 5px;
            padding-top: 8px;
            border-top: 1px dashed var(--card-border);
        }
        .btn-quick-item {
            background: #fff;
            border: 1px solid var(--card-border);
            border-radius: 6px;
            padding: 5px;
            font-size: 0.78rem;
            font-weight: 600;
            color: #475569;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
            cursor: pointer;
        }
        .btn-quick-item:hover { background: #f1f5f9; color: #0f172a; }
        .btn-quick-item.active-hengio { border-color: #15803d; color: #15803d; background: #f0fdf4; }
        .btn-quick-item.active-kll    { border-color: #d97706; color: #d97706; background: #fffbeb; }
        .btn-quick-item.pinned        { border-color: #2563eb; color: #2563eb; background: #eff6ff; font-weight: 700; }

        .card-footer-info {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 0.78rem;
            color: #64748b;
            margin-top: 8px;
            padding-top: 6px;
            border-top: 1px solid #f1f5f9;
        }

        .filter-panel-bg {
            background: #ffffff;
            border: 1px solid var(--card-border);
            border-radius: 12px;
            padding: 12px;
            margin-bottom: 14px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.02);
        }
    </style>
</head>
<body>

<!-- TOP NAVBAR -->
<div class="top-navbar py-2 mb-3">
    <div class="container-app d-flex justify-content-between align-items-center">
        <div class="d-flex align-items-center gap-2">
            <span class="badge bg-primary rounded-pill px-3 py-1.5" style="font-size: 0.85rem;">
                <i class="fa-solid fa-user-shield me-1"></i><span id="currentUserFullName">Đang tải...</span>
            </span>
            <span class="badge bg-light text-secondary border small">
                <i class="fa-solid fa-server text-primary me-1"></i>KMZ: <b id="kmzTotalCount">0</b>
            </span>
        </div>
        <div class="d-flex align-items-center gap-2">
            <button class="btn btn-sm btn-outline-primary fw-semibold rounded-pill px-3" type="button" data-bs-toggle="collapse" data-bs-target="#importPanel">
                <i class="fa-solid fa-cloud-arrow-up me-1"></i>Import File
            </button>
            <a href="${pageContext.request.contextPath}/logout" class="btn btn-sm btn-outline-danger rounded-circle" title="Đăng xuất">
                <i class="fa-solid fa-power-off"></i>
            </a>
        </div>
    </div>
</div>

<div class="container-app">

    <!-- KHUNG IMPORT (COLLAPSE) -->
    <div class="collapse mb-3" id="importPanel">
        <div class="card border-0 shadow-sm rounded-4 p-3 bg-white">
            <h6 class="fw-bold text-primary mb-3"><i class="fa-solid fa-file-arrow-up me-1"></i>Nạp dữ liệu hệ thống</h6>
            <div class="row g-2">
                <div class="col-md-6 d-none" id="excelImportCol">
                    <div class="p-2 border rounded-3 bg-light">
                        <label class="form-label small fw-bold text-success mb-1">File Danh sách KH (.xlsx)</label>
                        <input type="file" id="excelFileInput" accept=".xlsx,.xls" class="form-control form-control-sm mb-2">
                        <button class="btn btn-sm btn-success w-100 fw-semibold" onclick="uploadExcelFile()" id="btnUploadExcel">
                            <i class="fa-solid fa-upload me-1"></i>Import Excel
                        </button>
                    </div>
                </div>
                <div class="col-md-6" id="kmzImportCol">
                    <div class="p-2 border rounded-3 bg-light">
                        <label class="form-label small fw-bold text-primary mb-1">File Tọa độ Tủ (.kmz / .kml)</label>
                        <input type="file" id="kmzFileInput" accept=".kmz,.kml" class="form-control form-control-sm mb-2">
                        <button class="btn btn-sm btn-primary w-100 fw-semibold" onclick="uploadKmzFile()" id="btnUploadKmz">
                            <i class="fa-solid fa-upload me-1"></i>Import KMZ
                        </button>
                    </div>
                </div>
            </div>
            <div id="importResultAlert" class="mt-2"></div>
        </div>
    </div>

    <!-- ================= BỘ LỌC KHOANH VÙNG THÔNG MINH ================= -->
    <div class="filter-panel-bg">
        <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="fw-bold text-primary small"><i class="fa-solid fa-filter me-1"></i>Bộ lọc khoanh vùng địa bàn</span>
            <button class="btn btn-sm btn-link text-decoration-none p-0 small text-muted" onclick="resetFilters()">Xóa lọc</button>
        </div>

        <div class="row g-2">
            <!-- 1. Tìm kiếm chung -->
            <div class="col-12 col-md-6">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light"><i class="fa-solid fa-magnifying-glass text-muted"></i></span>
                    <input type="text" id="keywordInput" class="form-control" placeholder="Tìm tên KH, SĐT, mã phản ánh..." oninput="debounceLoadTickets()">
                </div>
            </div>

            <!-- 2. Lọc theo Nhân viên phụ trách -->
            <div class="col-6 col-md-3">
                <select id="staffFilterSelect" class="form-select form-select-sm fw-semibold text-primary" onchange="onStaffChange()"></select>
            </div>

            <!-- 3. Lọc theo Trạng thái ca -->
            <div class="col-6 col-md-3">
                <select id="statusFilterSelect" class="form-select form-select-sm" onchange="loadTickets()">
                    <option value="ALL">Tất cả trạng thái</option>
                    <option value="DANG_XU_LY">Đang xử lý</option>
                    <option value="HEN_GIO">Đã hẹn giờ</option>
                    <option value="KLL">Không liên lạc (KLL)</option>
                    <option value="PINNED">⭐ Đã ghim</option>
                    <option value="DA_XU_LY">Đã xử lý xong</option>
                </select>
            </div>

            <!-- 4. Lọc theo Khu vực (Area) -->
            <div class="col-6 col-md-4">
                <select id="areaFilterSelect" class="form-select form-select-sm" onchange="onAreaChange()">
                    <option value="">-- Tất cả Khu vực --</option>
                </select>
            </div>

            <!-- 5. Lọc theo Đầu mã tủ KMZ (7 ký tự đầu, VD: DLK0412) -->
            <div class="col-6 col-md-4">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light text-muted" style="font-size:0.75rem;">Mã Tủ</span>
                    <select id="cabinetPrefixInput" class="form-select" onchange="loadTickets()">
                        <option value="">-- Tất cả --</option>
                    </select>
                </div>
            </div>

            <!-- 6. Lọc theo Mức ưu tiên (Priority) -->
            <div class="col-12 col-md-4">
                <select id="priorityFilterSelect" class="form-select form-select-sm" onchange="loadTickets()">
                    <option value="">-- Tất cả Mức độ ưu tiên --</option>
                    <option value="Hot">🔴 Hot</option>
                    <option value="Nguy cơ HOT">🟠 Nguy cơ HOT</option>
                    <option value="Bình thường">🟢 Bình thường</option>
                </select>
            </div>
        </div>

        <div class="d-flex justify-content-between align-items-center mt-2 pt-2 border-top text-muted" style="font-size: 0.78rem;">
            <div>Hiển thị: <b class="text-primary" id="ticketCountText">0</b> ca phản ánh</div>
            <div id="autoFilterNotice"></div>
        </div>
    </div>

    <!-- DANH SÁCH THẺ KHÁCH HÀNG -->
    <div id="ticketListContainer">
        <div class="text-center py-5 text-muted">
            <div class="spinner-border text-primary spinner-border-sm mb-2" role="status"></div>
            <div class="small">Đang tải dữ liệu...</div>
        </div>
    </div>

</div>

<!-- MODAL XỬ LÝ CA -->
<div class="modal fade" id="actionModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content rounded-4 border-0 shadow">
            <div class="modal-header border-bottom-0 pb-0">
                <h6 class="modal-title fw-bold text-primary">Cập nhật xử lý ca</h6>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body pt-2">
                <input type="hidden" id="modalTicketId">
                <div class="mb-2">
                    <label class="form-label small fw-bold">Trạng thái</label>
                    <select id="modalStatusSelect" class="form-select form-select-sm">
                        <option value="DANG_XU_LY">Đang xử lý</option>
                        <option value="HEN_GIO">Hẹn giờ</option>
                        <option value="KLL">Không liên lạc được (KLL)</option>
                        <option value="DA_XU_LY">Đã xử lý hoàn tất</option>
                    </select>
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold">Khung giờ hẹn</label>
                    <input type="text" id="modalAppointmentTime" class="form-control form-control-sm" placeholder="VD: 14:00 - 15:00 • Lộ trình hôm nay">
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold">Ghi chú</label>
                    <textarea id="modalCallNote" rows="2" class="form-control form-control-sm" placeholder="Ghi chú kết quả gọi..."></textarea>
                </div>
            </div>
            <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-sm btn-secondary rounded-pill px-3" data-bs-dismiss="modal">Đóng</button>
                <button type="button" class="btn btn-sm btn-primary rounded-pill px-4 fw-bold" onclick="submitModalAction()">Lưu</button>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const CTX = '${pageContext.request.contextPath}';
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
            const res = await fetch(`\${CTX}/api/auth/me`);
            currentUser = await res.json();
            // Chỉ ADMIN mới thấy ô import XLSX; STAFF chỉ thấy ô KMZ (full chiều ngang)
            if (currentUser.admin) {
                document.getElementById('excelImportCol').classList.remove('d-none');
            } else {
                document.getElementById('kmzImportCol').classList.replace('col-md-6', 'col-md-12');
            }
            document.getElementById('currentUserFullName').textContent = `\${currentUser.fullName}`;

            const staffSelect = document.getElementById('staffFilterSelect');
            staffSelect.innerHTML = '';

            if (currentUser.admin) {
                const staffRes = await fetch(`\${CTX}/api/tickets/staffs`);
                const staffs = await staffRes.json();
                const optAll = document.createElement('option');
                optAll.value = 'ALL';
                optAll.textContent = '-- Tất cả nhân viên --';
                staffSelect.appendChild(optAll);

                staffs.forEach(name => {
                    const opt = document.createElement('option');
                    opt.value = name;
                    opt.textContent = `NV: \${name}`;
                    if (name.trim().toUpperCase() === currentUser.fullName.trim().toUpperCase()) opt.selected = true;
                    staffSelect.appendChild(opt);
                });
            } else {
                const opt = document.createElement('option');
                opt.value = currentUser.fullName;
                opt.textContent = `Địa bàn: \${currentUser.fullName}`;
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
            const res = await fetch(`\${CTX}/api/tickets/areas?staffName=\${encodeURIComponent(staffName)}`);
            const areas = await res.json();
            const areaSelect = document.getElementById('areaFilterSelect');
            areaSelect.innerHTML = '<option value="">-- Tất cả Khu vực của NV --</option>';
            areas.forEach(area => {
                if (area) {
                    const opt = document.createElement('option');
                    opt.value = area;
                    opt.textContent = `Khu vực: \${area}`;
                    areaSelect.appendChild(opt);
                }
            });
        } catch (e) { console.error(e); }
    }

    async function loadCabinetDropdownForStaff(staffName, area) {
        try {
            const res = await fetch(`\${CTX}/api/tickets/cabinet-prefixes?staffName=\${encodeURIComponent(staffName || '')}&area=\${encodeURIComponent(area || '')}`);
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
            const res = await fetch(`\${CTX}/api/kmz/count`);
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
            const res = await fetch(`\${CTX}/api/tickets?\${params.toString()}`);
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
            const dupBadge = (item.duplicateCount && item.duplicateCount >= 2) ? `<span class="badge-dup">Trùng x\${item.duplicateCount}</span>` : '';
            const appTime = (item.appointmentTime) ? `<div class="text-success small fw-bold mb-1"><i class="fa-regular fa-clock me-1"></i>\${escapeHtml(item.appointmentTime)}</div>` : '';

            let kmzHtml = '';
            if (item.cabinetCode) {
                if (item.googleMapsUrl) {
                    kmzHtml = `<a href="\${item.googleMapsUrl}" target="_blank" class="btn-kmz-map has-kmz"><i class="fa-solid fa-map-pin"></i>Tủ: \${escapeHtml(item.cabinetCode)}</a>`;
                } else {
                    kmzHtml = `<span class="btn-kmz-map no-kmz"><i class="fa-solid fa-server"></i>Tủ: \${escapeHtml(item.cabinetCode)} (Chưa có KMZ)</span>`;
                }
            }

            const complaint = escapeHtml(item.complaintContent || 'Không có nội dung');
            const isLong = complaint.length > 90;

            let phoneButtons = '';
            if (item.contactPhone) {
                phoneButtons += `<a href="tel:\${item.contactPhone}" class="btn-call-phone"><i class="fa-solid fa-phone"></i>Gọi KH (\${item.contactPhone})</a>`;
            }
            if (item.reportPhone && item.reportPhone !== item.contactPhone) {
                phoneButtons += `<a href="tel:\${item.reportPhone}" class="btn-call-phone btn-call-report"><i class="fa-solid fa-phone-volume"></i>Báo cáo</a>`;
            }

            const isPinned = item.isPinned === true;

            return `
            <div class="ticket-card">
                <div class="card-header-row">
                    <div>
                        <span class="customer-title">\${escapeHtml(item.customerName || 'Khách hàng')}</span>
                        <span class="badge-viettel ms-1">Viettel</span>
                        \${item.priorityLevel ? `<span class="badge bg-light text-secondary border ms-1" style="font-size:0.7rem;">\${escapeHtml(item.priorityLevel)}</span>` : ''}
        </div>
            <div>\${statusBadge}</div>
        </div>

            <div class="meta-info-row">
                <div>
                    <span class="account-tag">\${escapeHtml(item.subscriberAccount || item.ticketCode)}</span>
                    \${dupBadge}
                </div>
                <span class="text-muted" style="font-size: 0.78rem;">PA: #\${escapeHtml(item.ticketCode)}</span>
            </div>

            \${appTime}

            <div class="location-section">
                <div><i class="fa-solid fa-location-dot text-danger me-1"></i>\${escapeHtml(item.installAddress || 'Chưa có địa chỉ')}</div>
                <div>\${kmzHtml}</div>
            </div>

            <div class="complaint-box">
                <div class="\${isLong ? 'complaint-collapsed' : ''}" id="c-\${item.id}">\${complaint}</div>
                \${isLong ? `<a class="see-more-link" onclick="toggleC(\${item.id})">...xem thêm</a>` : ''}
                \${item.callNote ? `<div class="text-muted fst-italic mt-1" style="font-size: 0.78rem;"><i class="fa-regular fa-note-sticky me-1"></i>\${escapeHtml(item.callNote)}</div>` : ''}
            </div>

            <div class="action-buttons-grid">
                \${phoneButtons}
                <button type="button" class="btn-action-trigger ms-auto" onclick="openModal(\${item.id}, '\${item.appStatus}')">
                    <i class="fa-regular fa-pen-to-square"></i>Xử lý
                </button>
            </div>

            <div class="quick-toolbar">
                <button type="button" class="btn-quick-item \${item.appStatus==='HEN_GIO'?'active-hengio':''}" onclick="openModal(\${item.id}, 'HEN_GIO')">
                    <i class="fa-regular fa-clock"></i>Hẹn giờ
                </button>
                <button type="button" class="btn-quick-item \${item.appStatus==='KLL'?'active-kll':''}" onclick="quickKLL(\${item.id})">
                    <i class="fa-solid fa-phone-slash"></i>KLL
                </button>
                <button type="button" class="btn-quick-item" onclick="openModal(\${item.id}, 'HEN_KHAC')">
                    <i class="fa-regular fa-calendar"></i>Khác
                </button>
                <button type="button" class="btn-quick-item \${isPinned?'pinned':''}" onclick="togglePin(\${item.id}, \${!isPinned})">
                    <i class="fa-solid fa-star text-warning"></i>\${isPinned?'Đã ghim':'Ghim'}
                </button>
            </div>

            <div class="card-footer-info">
                <span>Khu vực: <b>\${escapeHtml(item.area || '')}</b></span>
                <span>Phụ trách: <b class="text-dark">\${escapeHtml(item.techStaffName || 'Chưa phân công')}</b></span>
            </div>
        </div>`;
        }).join('');
    }

    function toggleC(id) {
        const el = document.getElementById(`c-\${id}`);
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
        await fetch(`\${CTX}/api/tickets/\${id}/action`, {
            method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        actionModalInstance.hide();
        loadTickets();
    }

    async function quickKLL(id) {
        await fetch(`\${CTX}/api/tickets/\${id}/action`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ appStatus: 'KLL', callNote: 'Không liên lạc được' })
        });
        loadTickets();
    }

    async function togglePin(id, isPinned) {
        await fetch(`\${CTX}/api/tickets/\${id}/action`, {
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
        const res = await fetch(`\${CTX}/api/tickets/import`, { method: 'POST', body: fd });
        const d = await res.json();
        document.getElementById('importResultAlert').innerHTML = `<div class="alert alert-success py-1 small">\${d.message}</div>`;
        loadTickets();
    }

    async function uploadKmzFile() {
        const file = document.getElementById('kmzFileInput').files[0];
        if (!file) return alert('Chọn file KMZ!');
        const fd = new FormData(); fd.append('file', file);
        const res = await fetch(`\${CTX}/api/kmz/import`, { method: 'POST', body: fd });
        const d = await res.json();
        document.getElementById('importResultAlert').innerHTML = `<div class="alert alert-primary py-1 small">\${d.message}</div>`;
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
            await apiFetch(`\${CTX}/api/tickets/\${id}/action`, {
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
</script>
</body>
</html>