<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Hệ Thống Quản Lý Phản Ánh Khách Hàng</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/dashboard.css?v=1" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau các khối style của trang để ghi đè màu cũ). Đổi số v khi sửa file để trình duyệt tải lại -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
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
                    <span class="input-group-text bg-light text-muted vt-fs-75">Mã Tủ</span>
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

        <div class="d-flex justify-content-between align-items-center mt-2 pt-2 border-top text-muted vt-fs-78">
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
</script>
<script src="${pageContext.request.contextPath}/js/dashboard.js?v=1"></script>
<%-- Thanh điều hướng dưới cùng (đã gồm camera-capture.js: chụp ảnh hiện trường, chữ giờ/GPS/địa chỉ in lên ảnh) --%>
<!-- THANH THAO TÁC HÀNG LOẠT (chỉ hiện khi ADMIN bật "Chọn nhiều") -->
<div class="bulk-bar d-none" id="bulkBar">
    <div class="bulk-bar-top">
        <div>
            <b id="bulkSelectedCount">0</b> ca đã chọn
            <div class="text-muted vt-fs-74" id="bulkScopeNote"></div>
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