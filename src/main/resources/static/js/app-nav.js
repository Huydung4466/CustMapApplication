/*
 * app-nav.js - Đi kèm thanh trên cùng dùng chung (fragments/top-navbar.jsp).
 *  - Lấy thông tin tài khoản đang đăng nhập MỘT lần cho cả trang, hiện tên lên thanh.
 *    Các trang dùng AppNav.getMe() thay vì tự gọi /api/auth/me.
 *  - Trang bật số tủ KMZ thì tự nạp số đó; sau khi import KMZ gọi AppNav.refreshKmzCount() để cập nhật.
 * Không cần biến CTX của trang (đọc từ thuộc tính data-ctx của thanh).
 */
(function () {
    var nav = document.getElementById('appTopNav');
    var ctx = nav ? (nav.getAttribute('data-ctx') || '') : '';
    var mePromise = null;

    // Thông tin tài khoản (id, username, fullName, admin, roles); chỉ gọi server lần đầu
    function getMe() {
        if (!mePromise) {
            mePromise = fetch(ctx + '/api/auth/me', { headers: { 'Accept': 'application/json' } })
                .then(function (res) {
                    // Hết phiên đăng nhập: server chuyển hướng sang trang login
                    if (res.redirected && res.url.indexOf('/login') !== -1) {
                        window.location.href = ctx + '/login';
                        throw new Error('Phiên đăng nhập đã hết hạn');
                    }
                    if (!res.ok) {
                        return res.json().catch(function () { return null; }).then(function (d) {
                            if (d && d.code === 'PASSWORD_CHANGE_REQUIRED') window.location.href = ctx + '/change-password';
                            throw new Error((d && d.message) || ('Không lấy được thông tin tài khoản (mã ' + res.status + ')'));
                        });
                    }
                    return res.json();
                })
                .catch(function (e) { mePromise = null; throw e; });   // lỗi thì lần sau thử lại
        }
        return mePromise;
    }

    function refreshKmzCount() {
        var el = document.getElementById('kmzTotalCount');
        if (!el) return Promise.resolve();
        return fetch(ctx + '/api/kmz/count', { headers: { 'Accept': 'application/json' } })
            .then(function (res) { return res.ok ? res.json() : null; })
            .then(function (d) { if (d) el.textContent = d.totalLocations || 0; })
            .catch(function (e) { console.error(e); });
    }

    window.AppNav = { getMe: getMe, refreshKmzCount: refreshKmzCount };

    if (!nav) return;
    getMe().then(function (me) {
        var el = document.getElementById('currentUserFullName');
        if (el && me) el.textContent = me.fullName || '';
    }).catch(function (e) {
        console.error(e);
        var el = document.getElementById('currentUserFullName');
        if (el) el.textContent = '...';
    });
    if (nav.getAttribute('data-kmz') === 'true') refreshKmzCount();
})();
