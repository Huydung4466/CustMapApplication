<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đổi mật khẩu - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/change-password.css?v=1" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau các khối style của trang để ghi đè màu cũ). Đổi số v khi sửa file để trình duyệt tải lại -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-auth">

<div class="pw-card">
    <div class="text-center mb-3">
        <span class="brand-badge"><i class="fa-solid fa-key me-1"></i>BẢO MẬT TÀI KHOẢN</span>
        <h4 class="fw-bold text-dark mb-1">Đổi mật khẩu</h4>
    </div>

    <div id="forcedNotice" class="alert alert-warning py-2 small d-none">
        <i class="fa-solid fa-triangle-exclamation me-1"></i>
        Tài khoản của bạn đang dùng mật khẩu do quản trị viên cấp. Vui lòng đặt mật khẩu mới để tiếp tục sử dụng.
    </div>

    <div id="alertBox"></div>

    <div class="mb-3">
        <label class="form-label fw-semibold small text-secondary">Mật khẩu hiện tại</label>
        <input type="password" id="currentPassword" class="form-control" autocomplete="current-password" autofocus>
    </div>
    <div class="mb-3">
        <label class="form-label fw-semibold small text-secondary">Mật khẩu mới</label>
        <input type="password" id="newPassword" class="form-control" autocomplete="new-password">
    </div>
    <div class="mb-3">
        <label class="form-label fw-semibold small text-secondary">Nhập lại mật khẩu mới</label>
        <input type="password" id="confirmPassword" class="form-control" autocomplete="new-password">
    </div>

    <div class="rule-box text-muted mb-3">
        Mật khẩu mới cần: ít nhất 8 ký tự, gồm cả chữ và số, khác mật khẩu hiện tại và không trùng tên đăng nhập.
    </div>

    <button type="button" id="btnSubmit" class="btn btn-primary btn-submit w-100 text-white" onclick="submitChange()">
        <i class="fa-solid fa-check me-2"></i>Đổi mật khẩu
    </button>

    <div class="text-center mt-3 small">
        <a id="backLink" href="${pageContext.request.contextPath}/dashboard" class="text-decoration-none me-3">Quay lại</a>
        <a href="${pageContext.request.contextPath}/logout" class="text-decoration-none text-danger">Đăng xuất</a>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/csrf.js?v=1"></script>
<script>
    const CTX = '${pageContext.request.contextPath}';
    const FORCED = ${forced ? 'true' : 'false'};
</script>
<script src="${pageContext.request.contextPath}/js/change-password.js?v=1"></script>
</body>
</html>
