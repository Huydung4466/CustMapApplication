if (FORCED) {
    document.getElementById('forcedNotice').classList.remove('d-none');
    // Đang bị buộc đổi mật khẩu thì chưa được quay lại trang chính
    document.getElementById('backLink').classList.add('d-none');
}

function showAlert(type, message) {
    var box = document.getElementById('alertBox');
    var div = document.createElement('div');
    div.className = 'alert alert-' + type + ' py-2 small';
    div.textContent = message;
    box.innerHTML = '';
    box.appendChild(div);
}

async function submitChange() {
    var current = document.getElementById('currentPassword').value;
    var newPw = document.getElementById('newPassword').value;
    var confirmPw = document.getElementById('confirmPassword').value;

    if (!current || !newPw || !confirmPw) {
        return showAlert('danger', 'Vui lòng nhập đầy đủ các ô.');
    }
    if (newPw.length < 8 || !/[A-Za-z]/.test(newPw) || !/[0-9]/.test(newPw)) {
        return showAlert('danger', 'Mật khẩu mới cần ít nhất 8 ký tự, gồm cả chữ và số.');
    }
    if (newPw !== confirmPw) {
        return showAlert('danger', 'Mật khẩu xác nhận không khớp.');
    }

    var btn = document.getElementById('btnSubmit');
    btn.disabled = true;
    try {
        var res = await fetch(CTX + '/api/auth/change-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ currentPassword: current, newPassword: newPw, confirmPassword: confirmPw })
        });
        var data = {};
        try { data = await res.json(); } catch (e) { /* không có JSON */ }

        if (!res.ok) {
            btn.disabled = false;
            return showAlert('danger', data.message || ('Có lỗi xảy ra (mã ' + res.status + ')'));
        }
        showAlert('success', data.message || 'Đổi mật khẩu thành công.');
        setTimeout(function () { window.location.href = CTX + '/login?pwchanged=true'; }, 1200);
    } catch (e) {
        btn.disabled = false;
        showAlert('danger', 'Không kết nối được máy chủ, vui lòng thử lại.');
    }
}

document.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') submitChange();
});
