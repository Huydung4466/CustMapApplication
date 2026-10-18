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
    <style>
        body {
            background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            padding: 16px;
        }
        .login-card {
            background: #ffffff;
            border-radius: 16px;
            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.25);
            width: 100%;
            max-width: 420px;
            padding: 32px 28px;
        }
        .brand-badge {
            background: #ffe4e6;
            color: #e11d48;
            font-weight: 700;
            font-size: 0.85rem;
            padding: 4px 12px;
            border-radius: 999px;
            display: inline-block;
            margin-bottom: 8px;
        }
        .btn-login {
            background: #2563eb;
            border: none;
            border-radius: 10px;
            padding: 12px;
            font-weight: 600;
            font-size: 1rem;
        }
        .btn-login:hover {
            background: #1d4ed8;
        }
        .quick-acc {
            font-size: 0.82rem;
            background: #f8fafc;
            border: 1px dashed #cbd5e1;
            border-radius: 10px;
            padding: 10px 12px;
            margin-top: 20px;
        }
        .quick-acc code {
            cursor: pointer;
            color: #2563eb;
            font-weight: 600;
        }
    </style>
</head>
<body>

<div class="login-card">
    <div class="text-center mb-4">
        <span class="brand-badge"><i class="fa-solid fa-bolt me-1"></i>CSKH & KT ĐỊA BÀN</span>
        <h4 class="fw-bold text-dark mb-1">Đăng Nhập Hệ Thống</h4>
        <p class="text-muted small mb-0">Tự động lọc khách hàng theo tên nhân viên</p>
    </div>

    <!-- Thông báo lỗi / đăng xuất -->
    <div id="alertBox"></div>

    <form action="${pageContext.request.contextPath}/do-login" method="post">
        <div class="mb-3">
            <label class="form-label fw-semibold small text-secondary">Tài khoản (Username)</label>
            <div class="input-group">
                <span class="input-group-text bg-light"><i class="fa-regular fa-user text-secondary"></i></span>
                <input type="text" id="usernameInput" name="username" class="form-control" placeholder="Nhập username (VD: dunglh)" required autofocus>
            </div>
        </div>

        <div class="mb-4">
            <label class="form-label fw-semibold small text-secondary">Mật khẩu</label>
            <div class="input-group">
                <span class="input-group-text bg-light"><i class="fa-solid fa-lock text-secondary"></i></span>
                <input type="password" id="passwordInput" name="password" class="form-control" placeholder="Nhập mật khẩu" required>
            </div>
        </div>

        <button type="submit" class="btn btn-primary btn-login w-100 text-white">
            <i class="fa-solid fa-right-to-bracket me-2"></i>Đăng nhập
        </button>
    </form>

</div>

<script>
    const params = new URLSearchParams(window.location.search);
    const alertBox = document.getElementById('alertBox');
    if (params.has('error')) {
        alertBox.innerHTML = '<div class="alert alert-danger py-2 small"><i class="fa-solid fa-triangle-exclamation me-1"></i>Sai tài khoản hoặc mật khẩu!</div>';
    } else if (params.has('pwchanged')) {
        alertBox.innerHTML = '<div class="alert alert-success py-2 small"><i class="fa-solid fa-check me-1"></i>Đổi mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.</div>';
    } else if (params.has('logout')) {
        alertBox.innerHTML = '<div class="alert alert-success py-2 small"><i class="fa-solid fa-check me-1"></i>Đã đăng xuất thành công.</div>';
    }
</script>
</body>
</html>
