<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Cá nhân - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=2" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Cá nhân"/>
    <jsp:param name="navIcon" value="fa-solid fa-user"/>
</jsp:include>

<div class="container-app">

    <!-- ===== THÔNG TIN TÀI KHOẢN CỦA TÔI ===== -->
    <div class="card border-0 shadow-sm rounded-4 bg-white p-3 mb-3">
        <div class="d-flex align-items-center gap-3 mb-3">
            <div class="avatar-circle" id="avatar">?</div>
            <div class="min-w-0">
                <div class="fw-bold fs-5 text-dark" id="pFullName">...</div>
                <div class="text-muted small" id="pUsername"></div>
                <div class="mt-1" id="pRoles"></div>
            </div>
        </div>

        <div class="small mb-3">
            <div class="d-flex justify-content-between py-1 border-bottom"><span class="text-muted">Trạng thái</span><span id="pStatus">...</span></div>
        </div>

        <label class="form-label small fw-bold" for="phoneInput">Số điện thoại</label>
        <div class="input-group input-group-sm mb-1">
            <span class="input-group-text bg-light"><i class="fa-solid fa-phone text-muted"></i></span>
            <input type="tel" id="phoneInput" class="form-control" placeholder="VD: 0912345678" maxlength="20" autocomplete="tel">
            <button type="button" class="btn btn-primary" id="btnSavePhone" onclick="savePhone()">Lưu</button>
        </div>
        <div class="text-muted mb-3 vt-fs-75">
            Họ tên và tên đăng nhập do quản trị viên quản lý (họ tên dùng để ghép ca phản ánh với nhân viên).
        </div>

        <div class="d-grid gap-2">
            <a href="${pageContext.request.contextPath}/change-password" class="btn btn-outline-secondary btn-sm"><i class="fa-solid fa-key me-1"></i>Đổi mật khẩu</a>
            <a href="${pageContext.request.contextPath}/history" class="btn btn-outline-secondary btn-sm"><i class="fa-solid fa-circle-check me-1"></i>Lịch sử: ca đã hoàn thành</a>
            <a href="${pageContext.request.contextPath}/history/issues" class="btn btn-outline-secondary btn-sm"><i class="fa-solid fa-phone-slash me-1"></i>Lịch sử: không liên lạc được &amp; khác</a>
            <a href="${pageContext.request.contextPath}/report-snippets" class="btn btn-outline-secondary btn-sm"><i class="fa-solid fa-clipboard-check me-1"></i>Quản lý mẫu báo cáo<span class="badge bg-danger rounded-pill ms-2 d-none" id="snippetPendingBadge" title="Yêu cầu đang chờ duyệt">0</span></a>
            <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm"><i class="fa-solid fa-power-off me-1"></i>Đăng xuất</a>
        </div>
    </div>

    <!-- ===== NẠP DỮ LIỆU HỆ THỐNG (import Excel chỉ ROLE_ADMIN; KMZ mọi tài khoản) ===== -->
    <div class="card border-0 shadow-sm rounded-4 bg-white p-3 mb-3" id="importSection">
        <h6 class="fw-bold text-primary mb-3"><i class="fa-solid fa-file-arrow-up me-1"></i>Nạp dữ liệu hệ thống</h6>
        <div class="row g-2">
            <div class="col-12 col-md-6 d-none" id="excelImportCol">
                <div class="p-2 border rounded-3 bg-light h-100">
                    <label class="form-label small fw-bold text-success mb-1" for="excelFileInput">File dữ liệu (.xlsx) - phản ánh hoặc hạ tầng, tự nhận diện</label>
                    <input type="file" id="excelFileInput" accept=".xlsx,.xls" class="form-control form-control-sm mb-2">
                    <button type="button" class="btn btn-sm btn-success w-100 fw-semibold" onclick="uploadExcelFile()" id="btnUploadExcel">
                        <i class="fa-solid fa-upload me-1"></i>Import Excel
                    </button>
                </div>
            </div>
            <div class="col-12 col-md-6" id="kmzImportCol">
                <div class="p-2 border rounded-3 bg-light h-100">
                    <label class="form-label small fw-bold text-primary mb-1" for="kmzFileInput">File Tọa độ Tủ (.kmz / .kml)</label>
                    <input type="file" id="kmzFileInput" accept=".kmz,.kml" class="form-control form-control-sm mb-2">
                    <button type="button" class="btn btn-sm btn-primary w-100 fw-semibold" onclick="uploadKmzFile()" id="btnUploadKmz">
                        <i class="fa-solid fa-upload me-1"></i>Import KMZ
                    </button>
                </div>
            </div>
        </div>
        <!-- Đang import: thanh chạy liên tục + dòng báo, hiện cho tới khi server xử lý xong -->
        <div class="d-none mt-3" id="importBusy" role="status" aria-live="polite">
            <div class="progress vt-h-8">
                <div class="progress-bar progress-bar-striped progress-bar-animated w-100"></div>
            </div>
            <div class="small text-muted mt-1">
                <span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>
                <span id="importBusyText">Đang tải lên và xử lý, vui lòng không đóng trang...</span>
            </div>
        </div>
        <div id="importResultAlert" class="mt-2"></div>
    </div>

    <!-- ===== QUẢN LÝ TÀI KHOẢN (chỉ ROLE_ADMIN) ===== -->
    <div class="card border-0 shadow-sm rounded-4 bg-white p-3 mb-3 d-none" id="adminSection">
        <div class="d-flex justify-content-between align-items-center mb-2">
            <h6 class="fw-bold text-primary mb-0"><i class="fa-solid fa-users-gear me-1"></i>Quản lý tài khoản <span class="badge bg-light text-secondary border" id="userCount">0</span></h6>
            <button type="button" class="btn btn-sm btn-primary rounded-pill px-3" onclick="openCreate()"><i class="fa-solid fa-user-plus me-1"></i>Tạo tài khoản</button>
        </div>
        <div class="input-group input-group-sm mb-2">
            <span class="input-group-text bg-light"><i class="fa-solid fa-magnifying-glass text-muted"></i></span>
            <input type="text" id="userKeyword" class="form-control" placeholder="Tìm theo tên đăng nhập hoặc họ tên..." oninput="debounceLoadUsers()">
        </div>
        <div id="userList"><div class="text-center text-muted small py-3">Đang tải...</div></div>
        <div class="text-muted mt-2 vt-fs-75">
            Quản trị viên chỉ đặt lại được mật khẩu của tài khoản nhân viên (ROLE_STAFF). Nhân viên sẽ phải đổi mật khẩu khi đăng nhập lần tới.
        </div>
    </div>

    <!-- ===== CẤU HÌNH CAMERA (chỉ ROLE_ADMIN) ===== -->
    <div class="card border-0 shadow-sm rounded-4 bg-white p-3 mb-3 d-none" id="cameraSection">
        <h6 class="fw-bold text-primary mb-1"><i class="fa-solid fa-camera me-1"></i>Cấu hình camera</h6>
        <div class="text-muted mb-2 vt-fs-78">Bật/tắt các thông tin được in lên ảnh khi nhân viên chụp. Áp dụng cho mọi tài khoản ở lần chụp kế tiếp.</div>
        <div id="cameraSwitches"></div>
        <div class="text-muted small mt-3 mb-1">Xem trước (ví dụ):</div>
        <div class="camera-preview" id="cameraPreview"></div>
    </div>
</div>

<!-- MODAL TẠO TÀI KHOẢN -->
<div class="modal fade" id="createModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom-0 pb-0">
                <h6 class="modal-title fw-bold text-primary">Tạo tài khoản nhân viên</h6>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body pt-2">
                <div class="mb-2">
                    <label class="form-label small fw-bold" for="cUsername">Tên đăng nhập</label>
                    <input type="text" id="cUsername" class="form-control form-control-sm" autocomplete="off" maxlength="50" placeholder="VD: dunglh">
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold" for="cFullName">Họ tên</label>
                    <input type="text" id="cFullName" class="form-control form-control-sm" autocomplete="off" maxlength="100" placeholder="Đúng như cột &quot;Giao nhân viên&quot; trong file Excel">
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold" for="cPhone">Số điện thoại (không bắt buộc)</label>
                    <input type="tel" id="cPhone" class="form-control form-control-sm" autocomplete="off" maxlength="20">
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold" for="cPassword">Mật khẩu ban đầu</label>
                    <div class="input-group input-group-sm">
                        <input type="text" id="cPassword" class="form-control" autocomplete="off" spellcheck="false">
                        <button type="button" class="btn btn-outline-secondary" onclick="fillCreatePassword()" title="Tạo ngẫu nhiên"><i class="fa-solid fa-dice"></i></button>
                    </div>
                    <div class="form-text">Ít nhất 8 ký tự, gồm cả chữ và số. Nhân viên sẽ phải đổi ở lần đăng nhập đầu tiên.</div>
                </div>
                <div class="text-danger small" id="createError"></div>
            </div>
            <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-sm btn-secondary rounded-pill px-3" data-bs-dismiss="modal">Đóng</button>
                <button type="button" class="btn btn-sm btn-primary rounded-pill px-4 fw-bold" id="btnCreate" onclick="submitCreate()">Tạo</button>
            </div>
        </div>
    </div>
</div>

<!-- MODAL ĐẶT LẠI MẬT KHẨU -->
<div class="modal fade" id="resetModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom-0 pb-0">
                <h6 class="modal-title fw-bold text-primary">Đặt lại mật khẩu</h6>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body pt-2">
                <div class="small mb-2">Tài khoản: <b id="resetWho"></b></div>
                <label class="form-label small fw-bold" for="rPassword">Mật khẩu mới</label>
                <div class="input-group input-group-sm">
                    <input type="text" id="rPassword" class="form-control" autocomplete="off" spellcheck="false">
                    <button type="button" class="btn btn-outline-secondary" onclick="fillResetPassword()" title="Tạo ngẫu nhiên"><i class="fa-solid fa-dice"></i></button>
                </div>
                <div class="form-text">Ít nhất 8 ký tự, gồm cả chữ và số. Hãy gửi mật khẩu này cho nhân viên; họ sẽ phải đổi ngay khi đăng nhập.</div>
                <div class="text-danger small mt-2" id="resetError"></div>
            </div>
            <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-sm btn-secondary rounded-pill px-3" data-bs-dismiss="modal">Đóng</button>
                <button type="button" class="btn btn-sm btn-primary rounded-pill px-4 fw-bold" id="btnReset" onclick="submitReset()">Đặt lại</button>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>const CTX = '${pageContext.request.contextPath}';</script>
<script src="${pageContext.request.contextPath}/js/app-common.js?v=1"></script>
<script src="${pageContext.request.contextPath}/js/profile.js?v=3"></script>
<script>
    // Admin: hiện số yêu cầu mẫu báo cáo đang chờ duyệt trên nút (nhân viên không gọi API này)
    AppNav.getMe().then(function (me) {
        if (!me || !me.admin) return;
        return apiFetch(CTX + '/api/report-snippets/pending-count').then(function (d) {
            if (d && d.count > 0) {
                var b = document.getElementById('snippetPendingBadge');
                b.textContent = d.count;
                b.classList.remove('d-none');
            }
        });
    }).catch(function (e) { console.error(e); });
</script>

<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp">
    <jsp:param name="active" value="profile"/>
</jsp:include>
</body>
</html>
