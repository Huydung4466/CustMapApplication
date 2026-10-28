/*
 * nav-tabs.js - Hàng tab biểu tượng trong fragments/top-navbar.jsp.
 * Tab giữ chỗ (có thuộc tính data-soon) chưa có chức năng: bấm vào chỉ hiện thông báo ngắn "đang phát triển".
 * Khi làm xong một chức năng: đổi tab đó trong top-navbar.jsp thành <a class="nav-tab" href="..."> và bỏ data-soon.
 * Tab Thông báo (chuông) do notifications.js xử lý.
 */
(function () {
    var row = document.getElementById('appNavTabs');
    if (!row) return;

    var toast = null;
    var timer = null;

    function showSoon(name) {
        if (!toast) {
            toast = document.createElement('div');
            toast.className = 'nav-soon-toast';
            toast.setAttribute('role', 'status');
            document.body.appendChild(toast);
        }
        toast.textContent = '“' + name + '” đang phát triển';   // textContent: không chèn HTML
        // đọc offsetWidth để hiệu ứng mờ dần chạy lại khi bấm liên tiếp
        toast.classList.remove('show');
        void toast.offsetWidth;
        toast.classList.add('show');
        clearTimeout(timer);
        timer = setTimeout(function () { toast.classList.remove('show'); }, 1800);
    }

    row.addEventListener('click', function (e) {
        var tab = e.target.closest('.nav-tab[data-soon]');
        if (!tab) return;
        showSoon(tab.getAttribute('data-soon') || 'Chức năng này');
    });
})();
