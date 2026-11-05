<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Nghe nhạc - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=2" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Nghe nhạc"/>
    <jsp:param name="navIcon" value="fa-solid fa-music"/>
</jsp:include>

<div class="container-app pb-5">
    <div class="card border-0 shadow-sm rounded-4 bg-white text-center p-4">
        <div class="mb-2 text-secondary" style="font-size:2rem"><i class="fa-solid fa-lock"></i></div>
        <h6 class="fw-bold mb-1">Bạn chưa có quyền nghe nhạc</h6>
        <p class="small text-muted mb-3">Liên hệ quản trị viên để được cấp quyền Nhạc, sau đó tải lại trang.</p>
        <div class="d-flex justify-content-center gap-2">
            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/music">Tải lại</a>
            <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/dashboard">Về trang chủ</a>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp"/>
</body>
</html>
