/*
 * bottom-nav.js - Thanh điều hướng dưới cùng (fragments/bottom-nav.jsp).
 * Hiện SỐ KHÁCH ĐÃ HẸN (1, 2, 3...) ngay trên tab "Đã hẹn"; không có khách đã hẹn thì ẩn số.
 * Lấy từ /api/tickets/appointments/count (cùng cách tính với trang Khách đã hẹn). Tự cập nhật khi mở trang, khi quay lại tab,
 * và các trang gọi BottomNav.refreshAppointments() sau khi đổi trạng thái ca (dashboard.js, appointments.js).
 */
(function () {
    var nav = document.getElementById('appBottomNav');
    var badge = document.getElementById('bnApptBadge');
    if (!nav || !badge) return;
    var ctx = nav.getAttribute('data-ctx') || '';
    var stopped = false;

    function render(n) {
        n = Number(n) || 0;
        if (n > 0) {
            badge.textContent = n > 99 ? '99+' : String(n);
            badge.classList.remove('d-none');
            badge.title = n + ' khách đã hẹn';
        } else {
            badge.classList.add('d-none');
        }
    }

    function refreshAppointments() {
        if (stopped) return Promise.resolve();
        return fetch(ctx + '/api/tickets/appointments/count', { headers: { 'Accept': 'application/json' } })
            .then(function (res) {
                if ((res.redirected && res.url.indexOf('/login') !== -1) || res.status === 401 || res.status === 403) {
                    stopped = true;   // hết phiên: dừng gọi, trang sẽ tự chuyển về đăng nhập ở thao tác tiếp theo
                    throw new Error('session');
                }
                if (!res.ok) throw new Error('http ' + res.status);
                return res.json();
            })
            .then(function (d) { render(d.count); })
            .catch(function () { /* mất mạng tạm thời: giữ số cũ, lần sau thử lại */ });
    }

    document.addEventListener('visibilitychange', function () { if (!document.hidden) refreshAppointments(); });
    window.BottomNav = { refreshAppointments: refreshAppointments };
    refreshAppointments();
})();
