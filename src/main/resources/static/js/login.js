const params = new URLSearchParams(window.location.search);
const alertBox = document.getElementById('alertBox');
if (params.has('locked')) {
    alertBox.innerHTML = '<div class="alert alert-warning py-2 small"><i class="fa-solid fa-lock me-1"></i>Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.</div>';
} else if (params.has('error')) {
    alertBox.innerHTML = '<div class="alert alert-danger py-2 small"><i class="fa-solid fa-triangle-exclamation me-1"></i>Sai tài khoản hoặc mật khẩu!</div>';
} else if (params.has('pwchanged')) {
    alertBox.innerHTML = '<div class="alert alert-success py-2 small"><i class="fa-solid fa-check me-1"></i>Đổi mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.</div>';
} else if (params.has('logout')) {
    alertBox.innerHTML = '<div class="alert alert-success py-2 small"><i class="fa-solid fa-check me-1"></i>Đã đăng xuất thành công.</div>';
}

// Bấm Back từ dashboard có thể khôi phục trang login từ bộ nhớ đệm của trình duyệt -> tải lại để server chuyển hướng
window.addEventListener('pageshow', function (e) {
    if (e.persisted) window.location.reload();
});
