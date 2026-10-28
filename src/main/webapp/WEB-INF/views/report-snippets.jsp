<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Mẫu báo cáo - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=2" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/report-form.css?v=1" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/report-snippets.css?v=1" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Mẫu báo cáo"/>
    <jsp:param name="navIcon" value="fa-solid fa-clipboard-check"/>
</jsp:include>

<div class="container-app pb-5">

    <!-- Thêm mẫu mới: admin có hiệu lực ngay, nhân viên chờ admin duyệt (nhãn nút do JS đặt theo quyền) -->
    <div class="rf-card">
        <div class="rf-title"><i class="fa-solid fa-plus text-primary"></i>Thêm mẫu mới</div>
        <div class="row g-2 mb-2">
            <div class="col-12 col-md-6">
                <label class="rf-label" for="addField">Mục báo cáo</label>
                <select id="addField" class="form-select form-select-sm"></select>
            </div>
            <div class="col-12 col-md-6">
                <label class="rf-label" for="addGroup">Nhóm nhiệm vụ</label>
                <select id="addGroup" class="form-select form-select-sm"></select>
            </div>
        </div>
        <textarea id="addContent" class="form-control form-control-sm" rows="2" placeholder="Nội dung mẫu"></textarea>
        <div class="rf-counter"><span id="addCounter">0</span>/<span id="addMax">500</span></div>
        <div class="rf-actions">
            <button type="button" class="btn btn-primary" id="btnAdd" onclick="addSnippet()"><i class="fa-solid fa-paper-plane me-1"></i><span id="btnAddText">Gửi mẫu</span></button>
        </div>
        <div class="rf-note is-info" id="addNote"></div>
    </div>

    <!-- Chỉ admin: yêu cầu đang chờ duyệt (mẫu mới, yêu cầu sửa, yêu cầu xóa; chưa xử lý thì còn đọng ở đây) -->
    <div id="pendingWrap" class="d-none">
        <div class="rf-title px-1"><i class="fa-solid fa-hourglass-half text-warning"></i>Cần duyệt
            <span class="badge bg-warning text-dark rounded-pill" id="pendingCount">0</span></div>
        <div id="pendingList"><div class="text-center text-muted small py-3">Đang tải...</div></div>
    </div>

    <!-- Chỉ nhân viên: mẫu và yêu cầu của chính mình chưa xong (chờ duyệt, bị từ chối) -->
    <div id="mineWrap" class="d-none">
        <div class="rf-title px-1 mt-3"><i class="fa-solid fa-user-clock text-primary"></i>Yêu cầu của tôi
            <span class="badge bg-light text-secondary border rounded-pill" id="mineCount">0</span></div>
        <div id="mineList"></div>
    </div>

    <!-- Tất cả mẫu -->
    <div class="rf-title px-1 mt-4"><i class="fa-solid fa-list text-primary"></i>Tất cả mẫu
        <span class="badge bg-light text-secondary border rounded-pill" id="allCount">0</span></div>
    <div class="row g-2 mb-2">
        <div class="col-12 col-md-4">
            <select id="fieldFilter" class="form-select form-select-sm" onchange="renderAll()"></select>
        </div>
        <div class="col-12 col-md-4">
            <select id="groupFilter" class="form-select form-select-sm" onchange="renderAll()"></select>
        </div>
        <div class="col-12 col-md-4">
            <select id="stateFilter" class="form-select form-select-sm" onchange="renderAll()"></select>
        </div>
    </div>
    <div id="allList"></div>

    <!-- Chỉ admin: nhật ký thao tác gần đây trên mọi mẫu -->
    <div id="logWrap" class="d-none">
        <div class="rf-title px-1 mt-4"><i class="fa-solid fa-clock-rotate-left text-primary"></i>Nhật ký gần đây
            <button type="button" class="btn btn-sm btn-outline-secondary ms-auto" id="btnLog" onclick="toggleLog()">Xem</button></div>
        <div id="logList" class="d-none"></div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const CTX = '${pageContext.request.contextPath}';
</script>
<script src="${pageContext.request.contextPath}/js/app-common.js?v=1"></script>
<script src="${pageContext.request.contextPath}/js/report-snippets.js?v=2"></script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp"/>
</body>
</html>
