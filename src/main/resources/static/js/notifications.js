/*
 * notifications.js - Chuông thông báo trên thanh trên cùng (nạp bởi fragments/top-navbar.jsp, SAU csrf.js và app-nav.js).
 *  - Hiện số thông báo CHƯA ĐỌC trên tab chuông, và chấm đỏ ở tab Cá nhân của thanh dưới (gọi lại định kỳ, và khi quay lại tab).
 *  - Bấm tab chuông: mở danh sách thông báo mới nhất (mỗi trang do server quyết định, hiện 5; có "Tải thêm"); bấm một thông báo: đánh dấu đã đọc rồi mở mẫu liên quan.
 *  - "Đánh dấu tất cả đã đọc". Thông báo lưu ở server nên tải lại trang / đăng nhập lại vẫn còn.
 * Cho trang khác gọi: AppNotify.refresh() để cập nhật số ngay (VD sau khi duyệt một yêu cầu).
 * Mọi nội dung đưa vào trang đều được mã hóa HTML.
 */
(function () {
    var nav = document.getElementById('appTopNav');
    var btn = document.getElementById('navBellBtn');
    var badge = document.getElementById('navBellBadge');
    if (!nav || !btn || !badge) return;

    var ctx = nav.getAttribute('data-ctx') || '';
    var POLL_MS = 45000;
    var panel = null;
    var listEl = null;
    var moreBtn = null;
    var readAllBtn = null;
    var page = 0;
    var loading = false;
    var stopped = false;
    var timer = null;
    var lastUnread = 0;

    function esc(s) {
        return String(s === null || s === undefined ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function pad(n) { return String(n).padStart(2, '0'); }

    function fmtTime(v) {
        if (!v) return '';
        var d = new Date(v);
        if (isNaN(d.getTime())) return '';
        return pad(d.getHours()) + ':' + pad(d.getMinutes()) + ' ' + pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + d.getFullYear();
    }

    // Gọi API JSON; hết phiên thì dừng gọi lại (trang sẽ tự chuyển về đăng nhập ở lần thao tác tiếp theo)
    function call(url, options) {
        return fetch(ctx + url, options).then(function (res) {
            if (res.redirected && res.url.indexOf('/login') !== -1) { stopped = true; throw new Error('session'); }
            if (res.status === 401 || res.status === 403) { stopped = true; throw new Error('denied'); }
            if (!res.ok) throw new Error('http ' + res.status);
            return res.json();
        });
    }

    // Chấm đỏ ở tab Cá nhân (thanh dưới); phần tử này nằm cuối trang nên luôn tìm lại khi cập nhật
    function updateDot() {
        var dot = document.getElementById('bnProfileDot');
        if (dot) dot.classList.toggle('d-none', lastUnread <= 0);
    }

    function setBadge(n) {
        n = Number(n) || 0;
        lastUnread = n;
        updateDot();
        if (n > 0) {
            badge.textContent = n > 99 ? '99+' : String(n);
            badge.classList.remove('d-none');
            btn.title = 'Thông báo (' + n + ' chưa đọc)';
        } else {
            badge.classList.add('d-none');
            btn.title = 'Thông báo';
        }
        if (readAllBtn) readAllBtn.disabled = n === 0;
    }

    function refresh() {
        if (stopped) return Promise.resolve();
        return call('/api/notifications/unread-count', { headers: { 'Accept': 'application/json' } })
            .then(function (d) { setBadge(d.count); })
            .catch(function () { /* mất mạng tạm thời: lần sau thử lại */ });
    }

    function startPolling() {
        if (timer) clearInterval(timer);
        timer = setInterval(function () { if (!document.hidden) refresh(); }, POLL_MS);
    }

    document.addEventListener('DOMContentLoaded', updateDot);
    document.addEventListener('visibilitychange', function () { if (!document.hidden) refresh(); });

    // ---------------------------------------------------------------- bảng danh sách

    function buildPanel() {
        panel = document.createElement('div');
        panel.className = 'nt-panel';
        panel.setAttribute('role', 'dialog');
        panel.setAttribute('aria-label', 'Danh sách thông báo');
        panel.innerHTML = '<div class="nt-head"><span>Thông báo</span>'
            + '<button type="button" id="ntReadAll">Đánh dấu tất cả đã đọc</button></div>'
            + '<div class="nt-list" id="ntList"></div>'
            + '<button type="button" class="nt-more d-none" id="ntMore">Tải thêm</button>';
        document.body.appendChild(panel);
        listEl = panel.querySelector('#ntList');
        moreBtn = panel.querySelector('#ntMore');
        readAllBtn = panel.querySelector('#ntReadAll');
        readAllBtn.addEventListener('click', readAll);
        moreBtn.addEventListener('click', function () { loadPage(page + 1); });
        listEl.addEventListener('click', onItemClick);
        document.addEventListener('click', function (e) {
            if (panel.classList.contains('open') && !panel.contains(e.target) && !btn.contains(e.target)) closePanel();
        });
        document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closePanel(); });
    }

    // Bảng nằm ngay dưới hàng tab; hàng tab đã cuộn đi thì nằm ngay dưới thanh đỏ
    function positionPanel() {
        var bar = nav.getBoundingClientRect().bottom;
        var row = document.getElementById('appNavTabs');
        var rowBottom = row ? row.getBoundingClientRect().bottom : bar;
        panel.style.top = (Math.max(bar, rowBottom) + 4) + 'px';
    }

    function openPanel() {
        if (!panel) buildPanel();
        positionPanel();
        panel.classList.add('open');
        btn.classList.add('active');
        btn.setAttribute('aria-expanded', 'true');
        loadPage(0);
    }

    function closePanel() {
        if (!panel) return;
        panel.classList.remove('open');
        btn.classList.remove('active');
        btn.setAttribute('aria-expanded', 'false');
    }

    btn.addEventListener('click', function () {
        if (panel && panel.classList.contains('open')) closePanel(); else openPanel();
    });

    function itemHtml(n) {
        return '<div class="nt-item' + (n.read ? '' : ' unread') + '" data-id="' + esc(n.id) + '" data-link="' + esc(n.link || '') + '">'
            + '<div class="nt-icon ' + esc(n.colorClass) + '"><i class="' + esc(n.icon) + '"></i></div>'
            + '<div class="nt-body"><div class="nt-title">' + esc(n.title) + '</div>'
            + '<div class="nt-msg">' + esc(n.message) + '</div>'
            + '<div class="nt-time">' + esc(fmtTime(n.createdAt)) + '</div></div>'
            + (n.read ? '' : '<span class="nt-dot"></span>')
            + '</div>';
    }

    function loadPage(p) {
        if (loading) return;
        loading = true;
        moreBtn.disabled = true;
        if (p === 0) listEl.innerHTML = '<div class="nt-empty">Đang tải...</div>';
        call('/api/notifications?page=' + p, { headers: { 'Accept': 'application/json' } })
            .then(function (d) {
                page = p;
                var html = d.items.map(itemHtml).join('');
                if (p === 0) {
                    listEl.innerHTML = html || '<div class="nt-empty"><i class="fa-regular fa-bell-slash d-block mb-1" style="font-size:1.4rem"></i>Chưa có thông báo nào</div>';
                } else {
                    listEl.insertAdjacentHTML('beforeend', html);
                }
                moreBtn.classList.toggle('d-none', !d.hasMore);
                setBadge(d.unreadCount);
            })
            .catch(function () {
                if (p === 0) listEl.innerHTML = '<div class="nt-empty">Không tải được thông báo, vui lòng thử lại.</div>';
            })
            .then(function () { loading = false; moreBtn.disabled = false; });
    }

    function onItemClick(e) {
        var item = e.target.closest('.nt-item');
        if (!item) return;
        var id = item.getAttribute('data-id');
        var link = item.getAttribute('data-link');
        var go = function () { if (link) window.location.href = ctx + link; };
        if (!item.classList.contains('unread')) { go(); return; }
        item.classList.remove('unread');
        var dot = item.querySelector('.nt-dot');
        if (dot) dot.remove();
        call('/api/notifications/' + encodeURIComponent(id) + '/read', { method: 'POST', headers: { 'Accept': 'application/json' } })
            .then(function (d) { setBadge(d.unread); })
            .catch(function () { /* vẫn mở mẫu; số trên chuông sẽ đồng bộ ở lần gọi sau */ })
            .then(go);
    }

    function readAll() {
        readAllBtn.disabled = true;
        call('/api/notifications/read-all', { method: 'POST', headers: { 'Accept': 'application/json' } })
            .then(function () {
                setBadge(0);
                listEl.querySelectorAll('.nt-item.unread').forEach(function (el) { el.classList.remove('unread'); });
                listEl.querySelectorAll('.nt-dot').forEach(function (el) { el.remove(); });
            })
            .catch(function () { refresh(); });
    }

    window.AppNotify = { refresh: refresh };
    refresh();
    startPolling();
})();
