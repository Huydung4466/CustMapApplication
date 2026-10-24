<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Hệ Thống Quản Lý Phản Ánh Khách Hàng</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/old-dashboard.css?v=1" rel="stylesheet">
</head>
<body>

<!-- TOP NAVBAR -->
<div class="top-navbar py-2 mb-3">
    <div class="container-app d-flex justify-content-between align-items-center">
        <div class="d-flex align-items-center gap-2">
            <span class="badge bg-primary rounded-pill px-3 py-1.5 vt-fs-85">
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
                    <span class="input-group-text bg-light text-muted vt-fs-75">Mã Tủ</span>
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

        <div class="d-flex justify-content-between align-items-center mt-2 pt-2 border-top text-muted vt-fs-78">
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
</script>
<script src="${pageContext.request.contextPath}/js/old-dashboard.js?v=1"></script>
</body>
</html>