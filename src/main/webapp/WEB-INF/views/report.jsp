<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Báo cáo - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=2" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=1" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/report.css?v=1" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Báo cáo"/>
    <jsp:param name="navIcon" value="fa-solid fa-chart-column"/>
</jsp:include>

<div class="container-app">

    <!-- Tài khoản nhân viên -->
    <div class="card border-0 shadow-sm rounded-4 bg-white placeholder-card d-none" id="notAdmin">
        <div class="big-icon"><i class="fa-solid fa-lock"></i></div>
        <h6 class="fw-bold mb-1">Chỉ dành cho quản trị viên</h6>
        <p class="small text-muted mb-0">Xuất báo cáo Excel do quản trị viên thực hiện.</p>
    </div>

    <!-- ===== XUẤT BÁO CÁO EXCEL (ADMIN) ===== -->
    <div class="card border-0 shadow-sm rounded-4 bg-white p-3 mb-3 d-none" id="exportSection">
        <h6 class="fw-bold text-primary mb-1"><i class="fa-solid fa-file-excel me-1"></i>Xuất báo cáo Excel</h6>
        <div class="text-muted mb-3" style="font-size:0.8rem;">
            File gồm 3 sheet: <b>Nhân viên</b> (thống kê theo từng nhân viên), <b>Phản ánh</b> (toàn bộ ca, mỗi dòng tô màu theo trạng thái)
            và <b>Đã xử lý &amp; KLL</b> (kết quả, thời điểm xử lý, vị trí ảnh chụp).
        </div>

        <div class="row g-2 mb-3">
            <div class="col-12 col-md-6">
                <label class="form-label small fw-bold mb-1" for="exStaff">Nhân viên</label>
                <select id="exStaff" class="form-select form-select-sm"><option value="ALL">Tất cả nhân viên</option></select>
            </div>
            <div class="col-12 col-md-6">
                <label class="form-label small fw-bold mb-1" for="exScope">Phạm vi ca</label>
                <select id="exScope" class="form-select form-select-sm">
                    <option value="ALL">Tất cả ca</option>
                    <option value="CHANGED">Chỉ ca có thay đổi (đã xử lý, KLL, hẹn, không còn trong file)</option>
                </select>
            </div>
        </div>

        <div class="small fw-bold mb-1">Màu đánh dấu trong file:</div>
        <div class="d-flex flex-wrap gap-1 mb-3" id="legend"></div>

        <button type="button" class="btn btn-primary fw-bold" id="btnExport" onclick="doExport()">
            <i class="fa-solid fa-file-arrow-down me-1"></i><span>Xuất file Excel</span>
        </button>
    </div>

    <!-- ===== FILE ĐÃ XUẤT (ADMIN) ===== -->
    <div class="card border-0 shadow-sm rounded-4 bg-white p-3 mb-3 d-none" id="filesSection">
        <h6 class="fw-bold text-primary mb-2"><i class="fa-regular fa-folder-open me-1"></i>File đã xuất <span class="badge bg-light text-secondary border" id="fileCount">0</span></h6>
        <div class="alert alert-warning py-2 px-3 small mb-3">
            <i class="fa-solid fa-triangle-exclamation me-1"></i>
            File chỉ được lưu trên hệ thống <b><span id="ttlText">30</span> phút</b> kể từ lúc xuất, sau đó <b>tự động xóa</b>.
            Hãy tải về máy ngay; file đã tải về máy bạn vẫn dùng bình thường.
        </div>
        <div id="fileList"><div class="text-center text-muted small py-3">Đang tải...</div></div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>const CTX = '${pageContext.request.contextPath}';</script>
<script src="${pageContext.request.contextPath}/js/app-common.js?v=1"></script>
<script src="${pageContext.request.contextPath}/js/report.js?v=2"></script>

<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp">
    <jsp:param name="active" value="report"/>
</jsp:include>
</body>
</html>
