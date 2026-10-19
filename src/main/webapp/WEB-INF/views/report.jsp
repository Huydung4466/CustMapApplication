<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Báo cáo - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=1" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=1" rel="stylesheet">
</head>
<body class="vt-app">

<div class="top-navbar py-2 mb-3">
    <div class="container-app nav-inner">
        <div class="nav-left">
            <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-sm btn-outline-primary nav-icon-btn" title="Về danh sách ca" aria-label="Về danh sách ca">
                <i class="fa-solid fa-arrow-left"></i>
            </a>
            <span class="nav-title"><i class="fa-solid fa-chart-column me-1"></i>Báo cáo</span>
        </div>
    </div>
</div>

<div class="container-app">
    <div class="card border-0 shadow-sm rounded-4 bg-white placeholder-card">
        <div class="big-icon"><i class="fa-solid fa-chart-column"></i></div>
        <h6 class="fw-bold mb-1">Báo cáo</h6>
        <p class="small text-muted mb-0">Tính năng đang được phát triển.</p>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp">
    <jsp:param name="active" value="report"/>
</jsp:include>
</body>
</html>
