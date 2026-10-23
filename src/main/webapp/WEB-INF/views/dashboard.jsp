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
            overflow-x: hidden;
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

        /* Ca quá giờ hẹn */
        .ticket-card.overdue {
            border-color: #ef4444;
            box-shadow: 0 0 0 2px rgba(239,68,68,0.15);
        }
        .badge-distance {
            background: #ecfeff;
            color: #0e7490;
            border: 1px solid #cffafe;
            font-size: 0.72rem;
            font-weight: 700;
            padding: 2px 7px;
            border-radius: 6px;
            margin-right: 6px;
            white-space: nowrap;
        }
        .reminder-banner {
            border-radius: 12px;
            padding: 8px 12px;
            font-size: 0.8rem;
            margin-bottom: 10px;
        }

        /* Hàng nút: gọi điện bên trái, chụp ảnh + xử lý bên phải; hẹp quá thì nhóm bên phải xuống dòng */
        .action-buttons-grid {
            display: flex;
            flex-wrap: wrap;
            align-items: stretch;
        }
        .action-buttons-grid .btn-call-phone { flex: 1 1 190px; min-width: 0; }
        .action-right { display: flex; gap: 6px; margin-left: auto; }
        .btn-camera {
            background: #fff;
            color: #0f172a;
            border: 1px solid var(--card-border, #cbd5e1);
            border-radius: 8px;
            padding: 7px 12px;
            font-size: 0.85rem;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 6px;
            white-space: nowrap;
        }
        .btn-camera:hover { background: #f1f5f9; }
        .btn-camera .photo-count {
            background: #2563eb;
            color: #fff;
            border-radius: 999px;
            font-size: 0.72rem;
            padding: 0 6px;
            min-width: 18px;
            text-align: center;
        }

        /* ===== Tab nhóm (Phản ánh / Hạ tầng) + chip loại + lưới thuộc tính ca hạ tầng ===== */
        .group-tabs { display: flex; gap: 8px; margin-bottom: 10px; }
        .group-tab { flex: 1; border: 1px solid #e2e8f0; background: #fff; border-radius: 14px; padding: 9px 10px;
            font-weight: 700; font-size: 0.88rem; color: #475569; display: flex; align-items: center;
            justify-content: center; gap: 6px; cursor: pointer; }
        .group-tab .tab-count { background: #f1f5f9; color: #475569; border-radius: 999px; padding: 1px 8px; font-size: 0.75rem; }
        .group-tab.active { background: #2563eb; border-color: #2563eb; color: #fff; box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25); }
        .group-tab.active .tab-count { background: rgba(255, 255, 255, 0.25); color: #fff; }
        .type-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
        .type-chip { border: 1px solid #e2e8f0; background: #fff; border-radius: 999px; padding: 3px 11px;
            font-size: 0.78rem; font-weight: 600; color: #475569; cursor: pointer; }
        .type-chip.active { background: #0f172a; border-color: #0f172a; color: #fff; }
        .attr-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px 12px; font-size: 0.82rem; }
        .attr-grid .attr-label { display: block; color: #64748b; font-size: 0.72rem; }
        .attr-grid .attr-value { font-weight: 600; color: #0f172a; word-break: break-word; }

        /* ===== Chọn nhiều + thanh thao tác hàng loạt (ADMIN) ===== */
        .select-row { display: flex; align-items: center; gap: 8px; font-size: 0.82rem; font-weight: 600;
            color: #475569; margin-bottom: 8px; cursor: pointer; }
        .select-row .form-check-input { width: 1.15rem; height: 1.15rem; cursor: pointer; }
        .ticket-card.is-selected { outline: 2px solid #2563eb; outline-offset: -2px; background: #f8fbff; }
        .bulk-bar { position: fixed; left: 0; right: 0; bottom: calc(66px + env(safe-area-inset-bottom, 0px)); z-index: 1045;
            background: #fff; border-top: 1px solid #e2e8f0; box-shadow: 0 -4px 16px rgba(15, 23, 42, 0.12); padding: 10px 12px; }
        .bulk-bar-top { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between;
            gap: 6px; margin-bottom: 8px; font-size: 0.85rem; }
        .bulk-bar-actions { display: flex; gap: 8px; }
        .bulk-bar-actions .btn { flex: 1; font-weight: 700; }
        body.has-bottom-nav.select-mode { padding-bottom: calc(230px + env(safe-area-inset-bottom, 0px)) !important; }
    </style>
    <!-- Giao diện tông Viettel (đặt sau các khối style của trang để ghi đè màu cũ). Đổi số v khi sửa file để trình duyệt tải lại -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=1" rel="stylesheet">
</head>
<body class="vt-app">

<!-- TOP NAVBAR (dùng chung: fragments/top-navbar.jsp) -->
<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Danh sách ca"/>
    <jsp:param name="navIcon" value="fa-solid fa-list-check"/>
    <jsp:param name="navBack" value="false"/>
    <jsp:param name="navKmz" value="true"/>
</jsp:include>

<div class="container-app">

    <!-- ================= TAB NHÓM + CHIP LOẠI (dựng từ /api/tickets/types) ================= -->
    <div class="group-tabs" id="groupTabs"></div>
    <div class="type-chips" id="typeChips"></div>

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
                    <input type="text" id="keywordInput" class="form-control" placeholder="Tìm tên KH, SĐT, tài khoản, mã tủ, mã phản ánh..." oninput="debounceLoadTickets()">
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
                    <option value="PINNED">⭐ Đã ghim</option>
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
            <div class="col-12 col-md-4" id="priorityFilterCol">
                <select id="priorityFilterSelect" class="form-select form-select-sm" onchange="loadTickets()">
                    <option value="">-- Tất cả Mức độ ưu tiên --</option>
                    <option value="Hot">🔴 Hot</option>
                    <option value="Nguy cơ HOT">🟠 Nguy cơ HOT</option>
                    <option value="Bình thường">🟢 Bình thường</option>
                </select>
            </div>

            <!-- 7. Sắp xếp danh sách -->
            <div class="col-12 col-md-4">
                <select id="sortSelect" class="form-select form-select-sm" onchange="loadTickets()">
                    <option value="DEFAULT">↕ Mặc định (ghim, theo thứ tự)</option>
                    <option value="APPOINTMENT">⏰ Theo giờ hẹn</option>
                    <option value="NEAREST">📍 Gần tôi nhất</option>
                </select>
            </div>
        </div>

        <div class="d-flex justify-content-between align-items-center mt-2 pt-2 border-top text-muted" style="font-size: 0.78rem;">
            <div>Hiển thị: <b class="text-primary" id="ticketCountText">0</b> <span id="ticketCountUnit">ca phản ánh</span></div>
            <div id="autoFilterNotice"></div>
        </div>
    </div>

    <!-- NHẮC HẸN -->

    <!-- NÚT CHỌN NHIỀU (chỉ ADMIN mới thấy) -->
    <div class="d-flex justify-content-end mb-2 d-none" id="bulkToggleRow">
        <button type="button" class="btn btn-sm btn-outline-primary rounded-pill fw-semibold" id="btnBulkMode" onclick="toggleSelectMode()">
            <i class="fa-regular fa-square-check me-1"></i>Chọn nhiều
        </button>
    </div>

    <!-- DANH SÁCH THẺ KHÁCH HÀNG -->
    <div id="ticketListContainer">
        <div class="text-center py-5 text-muted">
            <div class="spinner-border text-primary spinner-border-sm mb-2" role="status"></div>
            <div class="small">Đang tải dữ liệu...</div>
        </div>
    </div>

    <!-- PHÂN TRANG -->
    <div id="paginationBar" class="d-flex justify-content-center align-items-center gap-3 my-3"></div>

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
                    <!-- Các lựa chọn do server cấp từ enum TicketStatus / StatusContext.MAIN (xem loadStatusMeta) -->
                    <select id="modalStatusSelect" class="form-select form-select-sm"></select>
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold">Giờ hẹn từ</label>
                    <input type="datetime-local" id="modalApptStart" class="form-control form-control-sm">
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold">Đến (không bắt buộc)</label>
                    <input type="datetime-local" id="modalApptEnd" class="form-control form-control-sm">
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
    let ticketCache = {};
    let currentPage = 1;
    const PAGE_SIZE = 10; // Số ca mỗi trang (10 - 20 là hợp lý)
    let myPosition = null;
    let currentGroup = 'PHAN_ANH'; // nhóm đang xem (tab)
    let currentType = '';          // loại đang lọc trong nhóm ('' = tất cả loại của nhóm)
    let statusMeta = { statuses: [], statusOptions: {}, archiveReasons: [], historyTabs: [] }; // lấy từ /api/tickets/meta
    let typeInfos = [];            // danh sách loại + số ca, lấy từ /api/tickets/types

    document.addEventListener('DOMContentLoaded', async () => {
        actionModalInstance = new bootstrap.Modal(document.getElementById('actionModal'));
        await loadStatusMeta();
        await initCurrentUserAndStaffFilter();
        // await loadFilterDropdowns();
        await loadTypeTabs(true);
        await loadTickets();
    });

    // Chỉ ADMIN mới thấy nút chọn nhiều ca để thao tác hàng loạt (phần import file đã chuyển sang trang Cá nhân)
    function applyAdminPermissions() {
        if (currentUser && currentUser.admin) {
            document.getElementById('bulkToggleRow').classList.remove('d-none');
        }
    }

    async function initCurrentUserAndStaffFilter() {
        try {
            currentUser = await AppNav.getMe();   // thanh trên cùng (top-navbar) đã lấy sẵn và hiện tên
            applyAdminPermissions();

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

    // ===================== TAB NHÓM + CHIP LOẠI =====================
    // Mọi loại / nhóm đều lấy từ server (enum TicketType): thêm loại mới ở đó là tab và chip tự có thêm.
    async function loadTypeTabs(pickDefault) {
        try {
            const staff = document.getElementById('staffFilterSelect').value || '';
            typeInfos = await apiFetch(`\${CTX}/api/tickets/types?staffFilter=\${encodeURIComponent(staff)}`);
        } catch (e) { console.error(e); return; }

        const groups = getGroups();
        if (pickDefault) {
            // Mặc định vào tab Phản ánh; nếu tab đó trống mà tab khác có ca thì vào tab có ca
            const cur = groups.find(g => g.code === currentGroup);
            if (!cur || (cur.count === 0 && groups.some(g => g.count > 0))) {
                currentGroup = (groups.find(g => g.count > 0) || groups[0] || { code: currentGroup }).code;
            }
        }
        renderGroupTabs();
        renderTypeChips();
        applyGroupUi();
    }

    function getGroups() {
        const groups = [];
        typeInfos.forEach(t => {
            let g = groups.find(x => x.code === t.group);
            if (!g) { g = { code: t.group, label: t.groupLabel, icon: t.groupIcon, count: 0 }; groups.push(g); }
            g.count += t.openCount;
        });
        return groups;
    }

    function renderGroupTabs() {
        document.getElementById('groupTabs').innerHTML = getGroups().map(g => `
            <button type="button" class="group-tab \${g.code === currentGroup ? 'active' : ''}" onclick="selectGroup('\${g.code}')">
                <i class="fa-solid \${escapeHtml(g.icon)}"></i>\${escapeHtml(g.label)}<span class="tab-count">\${g.count}</span>
            </button>`).join('');
    }

    function renderTypeChips() {
        const box = document.getElementById('typeChips');
        const types = typeInfos.filter(t => t.group === currentGroup);
        if (types.length <= 1) { box.innerHTML = ''; currentType = ''; return; }
        if (currentType && !types.some(t => t.code === currentType)) currentType = '';
        const total = types.reduce((sum, t) => sum + t.openCount, 0);
        box.innerHTML = `<button type="button" class="type-chip \${currentType === '' ? 'active' : ''}" onclick="selectType('')">Tất cả (\${total})</button>`
            + types.map(t => `<button type="button" class="type-chip \${currentType === t.code ? 'active' : ''}" onclick="selectType('\${t.code}')">\${escapeHtml(t.label)} (\${t.openCount})</button>`).join('');
    }

    // Bộ lọc mức ưu tiên chỉ có nghĩa với ca phản ánh nên ẩn ở nhóm hạ tầng
    function applyGroupUi() {
        const isComplaint = currentGroup === 'PHAN_ANH';
        document.getElementById('priorityFilterCol').classList.toggle('d-none', !isComplaint);
        if (!isComplaint) document.getElementById('priorityFilterSelect').value = '';
        const g = getGroups().find(x => x.code === currentGroup);
        document.getElementById('ticketCountUnit').textContent = 'ca ' + (g ? g.label.toLowerCase() : '');
    }

    function selectGroup(code) {
        if (code === currentGroup) return;
        currentGroup = code;
        currentType = '';
        renderGroupTabs();
        renderTypeChips();
        applyGroupUi();
        loadTickets();
    }

    function selectType(code) {
        currentType = code;
        renderTypeChips();
        loadTickets();
    }

    // Thuộc tính riêng của ca hạ tầng (nhãn do server gửi theo TicketType): 6 dòng đầu hiện sẵn, còn lại bấm "xem thêm"
    function renderAttributes(item) {
        const attrs = item.attributeItems || [];
        if (attrs.length === 0) return `<div class="text-muted">Chưa có thông số</div>`;
        const cell = a => `<div><span class="attr-label">\${escapeHtml(a.label)}</span><span class="attr-value">\${escapeHtml(a.value)}\${a.unit ? ' ' + escapeHtml(a.unit) : ''}</span></div>`;
        const head = attrs.slice(0, 6).map(cell).join('');
        const rest = attrs.slice(6).map(cell).join('');
        return `<div class="attr-grid">\${head}</div>`
            + (rest ? `<div class="attr-grid d-none mt-2" id="a-\${item.id}">\${rest}</div><a class="see-more-link" onclick="toggleAttrs(\${Number(item.id)}, this)">...xem thêm</a>` : '');
    }

    function toggleAttrs(id, link) {
        const hidden = document.getElementById(`a-\${id}`).classList.toggle('d-none');
        link.textContent = hidden ? '...xem thêm' : 'thu gọn';
    }


    // ===================== CHỌN NHANH + THAO TÁC HÀNG LOẠT (chỉ ADMIN) =====================
    // "Chọn tất cả" luôn gắn với BỘ LỌC hiện tại: server tự lấy các ca khớp bộ lọc rồi trừ các ca bỏ tick.
    // Đổi bộ lọc / từ khóa / tab thì lựa chọn bị xóa. Quyền thật sự do server kiểm tra (SecurityConfig + service).
    let selectMode = false;          // đang ở chế độ "Chọn nhiều"
    let selectAllMatching = false;   // true = chọn TOÀN BỘ ca theo bộ lọc hiện tại (mọi trang)
    const selectedIds = new Set();   // ca được tick riêng (khi selectAllMatching = false)
    const excludedIds = new Set();   // ca bỏ tick (khi selectAllMatching = true)
    let totalMatching = 0;           // tổng số ca khớp bộ lọc ở lần tải gần nhất
    let lastTickets = [];            // các ca đang hiển thị ở trang hiện tại
    let lastFilterBody = null;       // bộ lọc của lần tải gần nhất

    function isSelected(id) { return selectAllMatching ? !excludedIds.has(id) : selectedIds.has(id); }
    function selectedCount() { return selectAllMatching ? Math.max(0, totalMatching - excludedIds.size) : selectedIds.size; }

    function toggleSelectMode() {
        selectMode = !selectMode;
        if (!selectMode) clearSelection(true);
        document.body.classList.toggle('select-mode', selectMode);
        const btn = document.getElementById('btnBulkMode');
        btn.classList.toggle('btn-primary', selectMode);
        btn.classList.toggle('btn-outline-primary', !selectMode);
        btn.innerHTML = selectMode
            ? '<i class="fa-solid fa-xmark me-1"></i>Thoát chọn'
            : '<i class="fa-regular fa-square-check me-1"></i>Chọn nhiều';
        renderTicketCards(lastTickets);
        updateBulkBar();
    }

    function toggleSelect(id, checkbox) {
        const on = checkbox.checked;
        if (selectAllMatching) { if (on) excludedIds.delete(id); else excludedIds.add(id); }
        else { if (on) selectedIds.add(id); else selectedIds.delete(id); }
        const card = checkbox.closest('.ticket-card');
        if (card) card.classList.toggle('is-selected', on);
        updateBulkBar();
    }

    function selectPage() {
        selectAllMatching = false;
        excludedIds.clear();
        selectedIds.clear();
        lastTickets.forEach(t => selectedIds.add(Number(t.id)));
        renderTicketCards(lastTickets);
        updateBulkBar();
    }

    function selectAllByFilter() {
        if (totalMatching === 0) return;
        selectAllMatching = true;
        selectedIds.clear();
        excludedIds.clear();
        renderTicketCards(lastTickets);
        updateBulkBar();
    }

    // silent = true: chỉ xóa lựa chọn, không vẽ lại giao diện (dùng khi sắp tải lại danh sách)
    function clearSelection(silent) {
        selectAllMatching = false;
        selectedIds.clear();
        excludedIds.clear();
        if (silent !== true) {
            renderTicketCards(lastTickets);
            updateBulkBar();
        }
    }

    function updateBulkBar() {
        const bar = document.getElementById('bulkBar');
        if (!bar) return;
        bar.classList.toggle('d-none', !selectMode);
        if (!selectMode) return;

        const n = selectedCount();
        document.getElementById('bulkSelectedCount').textContent = n;
        const allBtn = document.getElementById('btnSelectAllFilter');
        allBtn.textContent = `Chọn tất cả \${totalMatching} ca theo bộ lọc`;
        allBtn.disabled = totalMatching === 0 || (selectAllMatching && excludedIds.size === 0);
        document.getElementById('bulkScopeNote').textContent = selectAllMatching
            ? 'Toàn bộ ca theo bộ lọc hiện tại' + (excludedIds.size ? ', trừ ' + excludedIds.size + ' ca bỏ chọn' : '')
            : 'Chọn riêng lẻ';
        document.getElementById('btnBulkDone').disabled = n === 0;
        document.getElementById('btnBulkDelete').disabled = n === 0;
    }

    // action: 'DONE' (chuyển Đã xử lý) | 'DELETE' (xóa vĩnh viễn kèm ảnh)
    async function bulkAction(action) {
        const count = selectedCount();
        if (count <= 0) { alert('Chưa chọn ca nào'); return; }
        if (selectAllMatching && !lastFilterBody) { alert('Chưa có bộ lọc, hãy tải lại danh sách'); return; }

        const scope = selectAllMatching
            ? ' (toàn bộ ca theo bộ lọc hiện tại' + (excludedIds.size ? ', trừ ' + excludedIds.size + ' ca bỏ chọn' : '') + ')'
            : '';
        let msg;
        if (action === 'DELETE') {
            msg = `XÓA VĨNH VIỄN \${count} ca\${scope}?\n\nCa bị xóa cùng ảnh hiện trường sẽ mất hoàn toàn, KHÔNG khôi phục được. `
                + 'Nếu ca vẫn còn trong file import sau này thì sẽ được nạp lại như ca mới.';
        } else {
            msg = `Chuyển \${count} ca\${scope} sang ĐÃ XỬ LÝ (vào Lịch sử)?`;
        }
        if (!confirm(msg)) return;

        // Xóa nhiều ca: bắt gõ chữ XOA để tránh bấm nhầm
        if (action === 'DELETE' && count > 20) {
            const typed = prompt(`Gõ XOA để xác nhận xóa vĩnh viễn \${count} ca:`);
            if ((typed || '').trim().toUpperCase() !== 'XOA') return;
        }

        const body = { action: action, expectedCount: count };
        if (selectAllMatching) {
            body.criteria = lastFilterBody;
            body.excludedIds = Array.from(excludedIds);
        } else {
            body.ids = Array.from(selectedIds);
        }

        const btns = [document.getElementById('btnBulkDone'), document.getElementById('btnBulkDelete')];
        btns.forEach(b => b.disabled = true);
        try {
            const result = await apiFetch(`\${CTX}/api/tickets/bulk`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            clearSelection(true);
            flashNotice(result.message);
            await loadTickets(true);
        } catch (e) {
            alert(e.message);
            updateBulkBar();
        }
    }

    // keepPage = true: giữ nguyên trang hiện tại (sau khi thao tác trên thẻ).
    // Mọi lần đổi bộ lọc / từ khóa / sắp xếp đều gọi không tham số -> tự về trang 1.
    async function loadTickets(keepPage) {
        if (keepPage !== true) { currentPage = 1; clearSelection(true); } // đổi bộ lọc thì bỏ chọn
        const keyword = document.getElementById('keywordInput').value.trim();
        const staffFilter = document.getElementById('staffFilterSelect').value;
        const statusVal = document.getElementById('statusFilterSelect').value;
        const area = document.getElementById('areaFilterSelect').value;
        const cabinetPrefix = document.getElementById('cabinetPrefixInput').value.trim();
        const priority = document.getElementById('priorityFilterSelect').value;

        let appStatus = 'ALL', onlyPinned = false;
        if (statusVal === 'PINNED') onlyPinned = true;
        else appStatus = statusVal;

        const sortSelect = document.getElementById('sortSelect');
        let sort = sortSelect.value;

        const params = new URLSearchParams({
            staffFilter: staffFilter || '',
            keyword,
            appStatus,
            onlyPinned,
            area: area || '',
            cabinetPrefix: cabinetPrefix || '',
            priority: priority || '',
            ticketGroup: currentGroup,
            ticketType: currentType
        });
        // Bộ lọc này được gửi lên server khi "Chọn tất cả theo bộ lọc" (thao tác hàng loạt)
        lastFilterBody = {
            staffFilter: staffFilter || '', keyword, appStatus, onlyPinned, area: area || '',
            cabinetPrefix: cabinetPrefix || '', priority: priority || '',
            ticketGroup: currentGroup, ticketType: currentType
        };

        // Sắp xếp "Gần tôi nhất": cần lấy vị trí hiện tại của thiết bị
        if (sort === 'NEAREST') {
            try {
                const pos = await getMyPosition();
                params.set('lat', pos.lat);
                params.set('lng', pos.lng);
            } catch (e) {
                alert(e.message);
                sort = 'DEFAULT';
                sortSelect.value = 'DEFAULT';
            }
        }
        params.set('sort', sort);
        params.set('page', currentPage);
        params.set('size', PAGE_SIZE);

        const container = document.getElementById('ticketListContainer');
        try {
            const data = await apiFetch(`\${CTX}/api/tickets?\${params.toString()}`);
            currentPage = data.page; // server tự đưa về trang hợp lệ nếu trang cũ không còn dữ liệu
            document.getElementById('ticketCountText').textContent = data.totalItem;
            lastTickets = data.resultList || [];
            // Đang "chọn tất cả theo bộ lọc" mà tổng số ca đổi (vd. vừa xử lý một ca) thì bỏ chọn để khỏi thao tác nhầm
            if (selectAllMatching && data.totalItem !== totalMatching) clearSelection(true);
            totalMatching = data.totalItem;
            renderTicketCards(data.resultList);
            updateBulkBar();
            renderPagination(data);
            loadTypeTabs(false); // cập nhật số ca trên tab sau mỗi lần tải / thao tác
        } catch (e) {
            container.innerHTML = `<div class="alert alert-danger p-2 small">\${escapeHtml(e.message)}</div>`;
            document.getElementById('paginationBar').innerHTML = '';
        }
    }

    function debounceLoadTickets() {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(function () { loadTickets(); }, 300);
    }

    async function resetFilters() {
        currentType = '';
        renderTypeChips();
        document.getElementById('keywordInput').value = '';
        document.getElementById('areaFilterSelect').value = '';
        document.getElementById('priorityFilterSelect').value = '';
        document.getElementById('statusFilterSelect').value = 'ALL';
        document.getElementById('sortSelect').value = 'DEFAULT';
        await loadCabinetDropdownForStaff(document.getElementById('staffFilterSelect').value, '');
        document.getElementById('cabinetPrefixInput').value = '';
        loadTickets();
    }

    function renderTicketCards(tickets) {
        const container = document.getElementById('ticketListContainer');
        ticketCache = {};
        (tickets || []).forEach(t => { ticketCache[t.id] = t; });
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
                    kmzHtml = `<a href="\${escapeHtml(item.googleMapsUrl)}" target="_blank" rel="noopener" class="btn-kmz-map has-kmz"><i class="fa-solid fa-map-pin"></i>Tủ: \${escapeHtml(item.cabinetCode)}</a>`;
                } else {
                    kmzHtml = `<span class="btn-kmz-map no-kmz"><i class="fa-solid fa-server"></i>Tủ: \${escapeHtml(item.cabinetCode)} (Chưa có KMZ)</span>`;
                }
            }

            const complaint = escapeHtml(item.complaintContent || 'Không có nội dung');
            const isLong = complaint.length > 90;
            const isInfra = item.ticketGroup === 'HA_TANG';
            // Ca phản ánh: đoạn nội dung khiếu nại; ca hạ tầng: lưới thông số kỹ thuật (xem renderAttributes)
            const contentHtml = isInfra
                ? renderAttributes(item)
                : `<div class="\${isLong ? 'complaint-collapsed' : ''}" id="c-\${item.id}">\${complaint}</div>`
                  + (isLong ? `<a class="see-more-link" onclick="toggleC(\${item.id})">...xem thêm</a>` : '');
            const refLabel = isInfra
                ? `<span class="badge text-bg-\${escapeHtml(item.ticketTypeColor || 'secondary')}" style="font-size:0.72rem;">\${escapeHtml(item.ticketTypeLabel)}</span>`
                : `PA: #\${escapeHtml(item.ticketCode)}`;

            let phoneButtons = '';
            if (item.contactPhone) {
                phoneButtons += `<a href="tel:\${escapeHtml(item.contactPhone)}" class="btn-call-phone"><i class="fa-solid fa-phone"></i>Gọi KH (\${escapeHtml(item.contactPhone)})</a>`;
            }
            if (item.reportPhone && item.reportPhone !== item.contactPhone) {
                phoneButtons += `<a href="tel:\${escapeHtml(item.reportPhone)}" class="btn-call-phone btn-call-report"><i class="fa-solid fa-phone-volume"></i>Báo cáo</a>`;
            }

            const isPinned = item.isPinned === true;
            const distBadge = (item.distanceKm !== null && item.distanceKm !== undefined)
                ? `<span class="badge-distance"><i class="fa-solid fa-route me-1"></i>\${item.distanceKm} km</span>` : '';

            return `
            <div class="ticket-card \${selectMode && isSelected(Number(item.id)) ? 'is-selected' : ''}">
                \${selectMode ? `<label class="select-row"><input type="checkbox" class="form-check-input m-0" \${isSelected(Number(item.id)) ? 'checked' : ''} onchange="toggleSelect(\${Number(item.id)}, this)"><span>Chọn ca này</span></label>` : ''}
                <div class="card-header-row">
                    <div>
                        <span class="customer-title">\${escapeHtml(item.customerName || 'Khách hàng')}</span>
                        <span class="badge-viettel ms-1">Viettel</span>
                        \${item.priorityLevel ? `<span class="badge ms-1" style="font-size:0.7rem;\${priorityStyle(item.priorityLevel)}">\${escapeHtml(item.priorityLevel)}</span>` : ''}
        </div>
            <div>\${statusBadge}</div>
        </div>

            <div class="meta-info-row">
                <div>
                    <span class="account-tag">\${escapeHtml(item.subscriberAccount || item.ticketCode)}</span>
                    \${dupBadge}
                </div>
                <span class="text-muted" style="font-size: 0.78rem;">\${refLabel}</span>
            </div>

            \${appTime}

            <div class="location-section">
                <div><i class="fa-solid fa-location-dot text-danger me-1"></i>\${escapeHtml(item.installAddress || 'Chưa có địa chỉ')}</div>
                <div>\${distBadge}\${kmzHtml}</div>
            </div>

            <div class="complaint-box">
                \${contentHtml}
                \${item.callNote ? `<div class="text-muted fst-italic mt-1" style="font-size: 0.78rem;"><i class="fa-regular fa-note-sticky me-1"></i>\${escapeHtml(item.callNote)}</div>` : ''}
            </div>

            <div class="action-buttons-grid">
                \${phoneButtons}
                <div class="action-right">
                    <button type="button" class="btn-camera" title="Chụp ảnh hiện trường" onclick="openCamera(\${Number(item.id)})">
                        <i class="fa-solid fa-camera"></i>\${item.photoCount > 0 ? `<span class="photo-count">\${Number(item.photoCount)}</span>` : ''}
                    </button>
                    <button type="button" class="btn-action-trigger" onclick="openModal(\${Number(item.id)}, '\${safeStatus(item.appStatus)}')">
                        <i class="fa-regular fa-pen-to-square"></i>Xử lý
                    </button>
                </div>
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
                <span>\${isInfra ? 'NV triển khai' : 'Phụ trách'}: <b class="text-dark">\${escapeHtml(item.techStaffName || (isInfra ? '—' : 'Chưa phân công'))}</b></span>
            </div>
        </div>`;
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
        loadTickets(true);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function toggleC(id) {
        const el = document.getElementById(`c-\${id}`);
        el.classList.toggle('complaint-collapsed');
    }

    // Màu nền theo mức ưu tiên: Hot = đỏ, Nguy cơ HOT = vàng, Bình thường = xanh
    function priorityStyle(level) {
        const v = String(level || '').trim().toUpperCase();
        if (v.includes('NGUY CƠ')) return 'background:#facc15;color:#422006;';
        if (v.includes('HOT'))     return 'background:#dc2626;color:#fff;';
        if (v.includes('BÌNH THƯỜNG')) return 'background:#16a34a;color:#fff;';
        return 'background:#f1f5f9;color:#475569;border:1px solid #e2e8f0;';
    }

    // Trạng thái xử lý (nhãn, biểu tượng, màu, các lựa chọn trong hộp "Cập nhật xử lý ca") do server khai báo
    // bằng enum TicketStatus + StatusContext; giao diện chỉ việc đọc từ /api/tickets/meta
    async function loadStatusMeta() {
        try {
            statusMeta = await apiFetch(CTX + '/api/tickets/meta');
            const sel = document.getElementById('modalStatusSelect');
            sel.innerHTML = '';
            (statusMeta.statusOptions.MAIN || []).forEach(function (o) {
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

    // Chỉ chấp nhận các trạng thái đã biết khi đưa vào thuộc tính onclick
    function safeStatus(s) {
        return findStatus(s) ? s : 'DANG_XU_LY';
    }

    function renderStatusBadge(status) {
        const info = findStatus(status) || findStatus('DANG_XU_LY');
        if (!info) return '';
        return '<span class="status-badge ' + escapeHtml(info.badgeClass) + '"><i class="' + escapeHtml(info.icon) + '"></i>'
            + escapeHtml(info.label) + '</span>';
    }

    // "2026-10-18T14:00:00" -> "2026-10-18T14:00" cho ô datetime-local
    function toInputValue(v) {
        return v ? String(v).substring(0, 16) : '';
    }

    function openModal(id, status) {
        const item = ticketCache[id] || {};
        document.getElementById('modalTicketId').value = id;
        document.getElementById('modalStatusSelect').value = status || 'DANG_XU_LY';
        document.getElementById('modalApptStart').value = toInputValue(item.appointmentStart);
        document.getElementById('modalApptEnd').value = toInputValue(item.appointmentEnd);
        document.getElementById('modalCallNote').value = item.callNote || '';
        actionModalInstance.show();
    }

    async function submitModalAction() {
        const id = document.getElementById('modalTicketId').value;
        const startVal = document.getElementById('modalApptStart').value;
        const endVal = document.getElementById('modalApptEnd').value;
        if (endVal && !startVal) {
            return alert('Hãy chọn giờ bắt đầu hẹn trước.');
        }
        const payload = {
            appStatus: document.getElementById('modalStatusSelect').value,
            callNote: document.getElementById('modalCallNote').value
        };
        if (startVal) {
            payload.appointmentStart = startVal;
            payload.appointmentEnd = endVal || null;
        } else if (ticketCache[id] && ticketCache[id].appointmentStart) {
            // Trước đó có giờ hẹn mà giờ để trống -> xóa giờ hẹn
            payload.clearAppointment = true;
        }
        if (await sendTicketAction(id, payload)) {
            actionModalInstance.hide();
            // Ca đã hẹn không còn nằm ở danh sách này mà chuyển sang trang "Đã hẹn"
            if (payload.appStatus === 'HEN_GIO' || payload.appStatus === 'HEN_KHAC') {
                flashNotice('Đã chuyển khách sang mục "Đã hẹn"');
            }
        }
        loadTickets(true);
    }

    async function quickKLL(id) {
        if (!confirm('Không liên lạc được khách hàng này? Ca sẽ được chuyển vào Lịch sử.')) return;
        await sendTicketAction(id, { appStatus: 'KLL', callNote: 'Không liên lạc được' });
        loadTickets(true);
    }

    async function togglePin(id, isPinned) {
        await sendTicketAction(id, { isPinned });
        loadTickets(true);
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
            let data = null;
            if (res.status === 413) {
                msg = 'File quá lớn, vượt giới hạn cho phép';
            } else {
                try {
                    data = await res.json();
                    msg = data.message || data.detail || msg;
                } catch (_) { /* không có JSON */ }
            }
            const error = new Error(msg);
            error.status = res.status;
            error.data = data;
            throw error;
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

    // Lấy vị trí hiện tại của thiết bị (chỉ hoạt động trên HTTPS hoặc localhost)
    function getMyPosition() {
        return new Promise((resolve, reject) => {
            if (!window.isSecureContext) {
                return reject(new Error('Trình duyệt chỉ cho lấy vị trí khi mở trang bằng HTTPS hoặc localhost.'));
            }
            if (!navigator.geolocation) {
                return reject(new Error('Thiết bị không hỗ trợ định vị.'));
            }
            navigator.geolocation.getCurrentPosition(
                pos => {
                    myPosition = { lat: pos.coords.latitude, lng: pos.coords.longitude };
                    resolve(myPosition);
                },
                err => reject(new Error(err.code === 1
                    ? 'Bạn chưa cho phép truy cập vị trí. Hãy bật quyền vị trí cho trang này rồi thử lại.'
                    : 'Không lấy được vị trí hiện tại, vui lòng thử lại sau.')),
                { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
            );
        });
    }

    // Thông báo nhỏ nổi phía trên thanh điều hướng, tự tắt
    function flashNotice(message) {
        const old = document.getElementById('flashNotice');
        if (old) old.remove();
        const el = document.createElement('div');
        el.id = 'flashNotice';
        el.textContent = message;
        el.style.cssText = 'position:fixed;left:50%;bottom:calc(96px + env(safe-area-inset-bottom,0px));transform:translateX(-50%);'
            + 'z-index:1060;max-width:90%;padding:9px 16px;border-radius:10px;font-size:.86rem;text-align:center;'
            + 'background:rgba(15,23,42,.93);color:#fff;box-shadow:0 6px 18px rgba(0,0,0,.25);';
        document.body.appendChild(el);
        setTimeout(() => el.remove(), 3000);
    }

    function escapeHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
</script>
<%-- Thanh điều hướng dưới cùng (đã gồm camera-capture.js: chụp ảnh hiện trường, chữ giờ/GPS/địa chỉ in lên ảnh) --%>
<!-- THANH THAO TÁC HÀNG LOẠT (chỉ hiện khi ADMIN bật "Chọn nhiều") -->
<div class="bulk-bar d-none" id="bulkBar">
    <div class="bulk-bar-top">
        <div>
            <b id="bulkSelectedCount">0</b> ca đã chọn
            <div class="text-muted" style="font-size:0.74rem;" id="bulkScopeNote"></div>
        </div>
        <div class="d-flex flex-wrap gap-1">
            <button type="button" class="btn btn-sm btn-outline-secondary" onclick="selectPage()">Chọn trang này</button>
            <button type="button" class="btn btn-sm btn-outline-primary" id="btnSelectAllFilter" onclick="selectAllByFilter()">Chọn tất cả</button>
            <button type="button" class="btn btn-sm btn-outline-secondary" onclick="clearSelection()">Bỏ chọn</button>
        </div>
    </div>
    <div class="bulk-bar-actions">
        <button type="button" class="btn btn-success" id="btnBulkDone" onclick="bulkAction('DONE')"><i class="fa-solid fa-check me-1"></i>Đã xử lý</button>
        <button type="button" class="btn btn-danger" id="btnBulkDelete" onclick="bulkAction('DELETE')"><i class="fa-regular fa-trash-can me-1"></i>Xóa</button>
    </div>
</div>

<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp"/>
</body>
</html>