<%--
  Created by IntelliJ IDEA.
  User: MiDouHsk
  Date: 17/10/2026
  Time: 6:06 CH
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/login.css?v=1" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau các khối style của trang để ghi đè màu cũ). Đổi số v khi sửa file để trình duyệt tải lại -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-auth">

<div class="login-card">
    <div class="text-center mb-4">
        <span class="brand-badge"><i class="fa-solid fa-bolt me-1"></i>CSKH & KT ĐỊA BÀN</span>
        <h4 class="fw-bold text-dark mb-1">Đăng Nhập Hệ Thống</h4>
        <p class="text-muted small mb-0">Tự động lọc khách hàng theo tên nhân viên</p>
    </div>

    <!-- Thông báo lỗi / đăng xuất -->
    <div id="alertBox"></div>

    <form action="${pageContext.request.contextPath}/do-login" method="post">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
        <div class="mb-3">
            <label class="form-label fw-semibold small text-secondary">Tài khoản (Username)</label>
            <div class="input-group">
                <span class="input-group-text bg-light"><i class="fa-regular fa-user text-secondary"></i></span>
                <input type="text" id="usernameInput" name="username" class="form-control" placeholder="Nhập username (VD: dunglh)" required autofocus>
            </div>
        </div>

        <div class="mb-3">
            <label class="form-label fw-semibold small text-secondary">Mật khẩu</label>
            <div class="input-group">
                <span class="input-group-text bg-light"><i class="fa-solid fa-lock text-secondary"></i></span>
                <input type="password" id="passwordInput" name="password" class="form-control" placeholder="Nhập mật khẩu" required>
            </div>
        </div>

        <div class="form-check mb-4">
            <input class="form-check-input" type="checkbox" id="rememberMe" name="remember-me" checked>
            <label class="form-check-label small text-secondary" for="rememberMe">Ghi nhớ đăng nhập</label>
        </div>

        <button type="submit" class="btn btn-primary btn-login w-100 text-white">
            <i class="fa-solid fa-right-to-bracket me-2"></i>Đăng nhập
        </button>
    </form>

</div>

<script src="${pageContext.request.contextPath}/js/csrf.js?v=1"></script>
<script src="${pageContext.request.contextPath}/js/login.js?v=1"></script>
</body>
</html>
