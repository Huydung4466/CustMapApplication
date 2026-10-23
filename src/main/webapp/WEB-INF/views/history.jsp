<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>${historyTabLabel} - Lịch sử ca</title>
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

        .history-card { opacity: 0.97; }
        .reason-done { background:#f0fdf4; color:#15803d; border:1px solid #dcfce7; }
        .reason-closed { background:#f1f5f9; color:#475569; border:1px solid #e2e8f0; }
        .reason-kll { background:#fffbeb; color:#b45309; border:1px solid #fde68a; }
    </style>
    <!-- Giao diện tông Viettel (đặt sau các khối style của trang để ghi đè màu cũ). Đổi số v khi sửa file để trình duyệt tải lại -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=1" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="${historyTabLabel}"/>
    <jsp:param name="navIcon" value="${historyTabIcon}"/>
</jsp:include>

<div class="container-app">

    <!-- Chuyển giữa 2 trang Lịch sử (dựng từ enum HistoryTab qua /api/tickets/meta) -->
    <div class="btn-group w-100 mb-3" id="historyTabs" role="group" aria-label="Chọn trang lịch sử"></div>

    <div class="filter-panel-bg">
        <div class="row g-2">
            <div class="col-12 col-md-6">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light"><i class="fa-solid fa-magnifying-glass text-muted"></i></span>
                    <input type="text" id="keywordInput" class="form-control" placeholder="Tìm tên KH, SĐT, mã phản ánh, địa chỉ..." oninput="debounceLoadHistory()">
                </div>
            </div>
            <div class="col-12 col-md-6 d-none" id="staffCol">
                <select id="staffFilterSelect" class="form-select form-select-sm fw-semibold text-primary" onchange="loadHistory()"></select>
            </div>
            <div class="col-6 col-md-4">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light text-muted" style="font-size:0.75rem;">Từ</span>
                    <input type="date" id="fromDateInput" class="form-control" onchange="loadHistory()">
                </div>
            </div>
            <div class="col-6 col-md-4">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light text-muted" style="font-size:0.75rem;">Đến</span>
                    <input type="date" id="toDateInput" class="form-control" onchange="loadHistory()">
                </div>
            </div>
            <div class="col-12 col-md-4 text-end">
                <button class="btn btn-sm btn-link text-decoration-none text-muted" onclick="resetHistoryFilters()">Xóa lọc</button>
            </div>
        </div>
        <div class="mt-2 pt-2 border-top text-muted" style="font-size: 0.78rem;">
            Tổng: <b class="text-primary" id="historyCountText">0</b> ca
        </div>
    </div>

    <div id="historyList">
        <div class="text-center py-5 text-muted">
            <div class="spinner-border text-primary spinner-border-sm mb-2" role="status"></div>
            <div class="small">Đang tải dữ liệu...</div>
        </div>
    </div>

    <div id="paginationBar" class="d-flex justify-content-center align-items-center gap-3 my-3"></div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const CTX = '${pageContext.request.contextPath}';
    let currentUser = null;
    let searchTimer = null;
    let currentPage = 1;
    const PAGE_SIZE = 10;
    // Trang lịch sử đang xem: COMPLETED (đã hoàn thành) hoặc ISSUES (không liên lạc được & khác), do server gán
    const HISTORY_TAB = '${historyTab}';
    let meta = null; // trạng thái / lý do lưu trữ / các trang lịch sử, lấy từ /api/tickets/meta (enum phía server)

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
                +   '<span class="text-muted" style="font-size:0.78rem;">PA: #' + escapeHtml(item.ticketCode) + '</span></div>'
                + '<div class="text-muted small mb-1"><i class="fa-regular fa-calendar-check me-1"></i>Chuyển vào lịch sử lúc ' + escapeHtml(formatDateTime(item.archivedAt)) + '</div>'
                + (item.appointmentTime ? '<div class="text-success small fw-bold mb-1"><i class="fa-regular fa-clock me-1"></i>' + escapeHtml(item.appointmentTime) + '</div>' : '')
                + '<div class="location-section"><div><i class="fa-solid fa-location-dot text-danger me-1"></i>' + escapeHtml(item.installAddress || 'Chưa có địa chỉ') + '</div></div>'
                + '<div class="complaint-box"><div class="complaint-collapsed" id="h-' + item.id + '">' + complaint + '</div>'
                +   '<a class="see-more-link" onclick="toggleHistoryText(' + item.id + ')">...xem thêm</a>'
                +   (item.callNote ? '<div class="text-muted fst-italic mt-1" style="font-size:0.78rem;"><i class="fa-regular fa-note-sticky me-1"></i>' + escapeHtml(item.callNote) + '</div>' : '')
                + '</div>'
                + '<div class="action-buttons-grid">' + phones
                +   '<button type="button" class="btn-action-trigger ms-auto" onclick="restoreTicket(' + item.id + ')"><i class="fa-solid fa-rotate-left"></i>Đưa về danh sách</button>'
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
</script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp"/>
</body>
</html>
