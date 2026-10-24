/*
 * app-common.js - Hàm dùng chung cho các trang mới (Đã hẹn, Cá nhân...).
 * Cần khai báo biến CTX (context path) trong trang TRƯỚC khi nạp file này.
 */

function escapeHtml(str) {
    return String(str === null || str === undefined ? '' : str)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// Gọi API trả JSON. Lỗi -> ném Error(message) kèm status/data; hết phiên hoặc bị buộc đổi mật khẩu -> chuyển trang phù hợp
async function apiFetch(url, options) {
    const res = await fetch(url, options);
    if (res.redirected && res.url.indexOf('/login') !== -1) {
        window.location.href = CTX + '/login';
        throw new Error('Phiên đăng nhập đã hết hạn');
    }
    if (!res.ok) {
        let msg = 'Có lỗi xảy ra (mã ' + res.status + ')';
        let data = null;
        if (res.status === 413) {
            msg = 'File quá lớn, vượt giới hạn cho phép';
        } else {
            try {
                data = await res.json();
                msg = data.message || data.detail || msg;
            } catch (_) { /* không có JSON */ }
        }
        if (data && data.code === 'PASSWORD_CHANGE_REQUIRED') {
            window.location.href = CTX + '/change-password';
        }
        const error = new Error(msg);
        error.status = res.status;
        error.data = data;
        throw error;
    }
    return res.json();
}

// Thông báo nhỏ nổi phía trên thanh điều hướng, tự tắt
function showToast(message, isError) {
    const old = document.getElementById('appToast');
    if (old) old.remove();
    const el = document.createElement('div');
    el.id = 'appToast';
    el.className = 'app-toast' + (isError ? ' error' : '');
    el.textContent = message;
    document.body.appendChild(el);
    setTimeout(function () { if (el.parentNode) el.remove(); }, isError ? 4200 : 2800);
}

// Màu nền theo mức ưu tiên: Hot = đỏ, Nguy cơ HOT = vàng, Bình thường = xanh
function priorityStyle(level) {
    const v = String(level || '').trim().toUpperCase();
    if (v.indexOf('NGUY CƠ') !== -1) return 'background:#facc15;color:#422006;';
    if (v.indexOf('HOT') !== -1) return 'background:#dc2626;color:#fff;';
    if (v.indexOf('BÌNH THƯỜNG') !== -1) return 'background:#16a34a;color:#fff;';
    return 'background:#f1f5f9;color:#475569;border:1px solid #e2e8f0;';
}

// "2026-10-18T14:00:00" -> "2026-10-18T14:00" cho ô datetime-local
function toInputValue(v) {
    return v ? String(v).substring(0, 16) : '';
}

function pad2(n) { return String(n).padStart(2, '0'); }

function formatDateTime(v) {
    if (!v) return '';
    const d = new Date(v);
    return pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ' ' + pad2(d.getDate()) + '/' + pad2(d.getMonth() + 1) + '/' + d.getFullYear();
}

// Ca đã hẹn nhưng đã quá giờ hẹn (hết giờ kết thúc, hoặc quá giờ bắt đầu nếu không có giờ kết thúc)
function isOverdue(item) {
    if (!item.appointmentStart) return false;
    return new Date(item.appointmentEnd || item.appointmentStart).getTime() < Date.now();
}

// Chữ cái đầu của tên để làm ảnh đại diện
function initialsOf(name) {
    const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
    if (!parts.length) return '?';
    return parts[parts.length - 1].charAt(0).toUpperCase();
}
