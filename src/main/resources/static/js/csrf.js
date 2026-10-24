/*
 * Tự gắn token CSRF vào mọi lệnh gọi ghi (POST/PUT/PATCH/DELETE) cùng nguồn gốc.
 * Token do Spring Security đặt trong cookie XSRF-TOKEN; header gửi lên là X-XSRF-TOKEN.
 * Nạp file này TRƯỚC các script có gọi fetch (đã đưa vào top-navbar.jsp, login.jsp, change-password.jsp).
 */
(function () {
    if (window.__csrfPatched) return;
    window.__csrfPatched = true;

    function readCookie(name) {
        var m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
        return m ? decodeURIComponent(m[1]) : '';
    }

    var SAFE_METHOD = /^(GET|HEAD|OPTIONS|TRACE)$/i;

    function isSameOrigin(url) {
        try { return new URL(url, window.location.href).origin === window.location.origin; }
        catch (e) { return false; }
    }

    var originalFetch = window.fetch;
    window.fetch = function (input, init) {
        init = init || {};
        var method = init.method || (input && input.method) || 'GET';
        var url = typeof input === 'string' ? input : ((input && input.url) || '');
        if (!SAFE_METHOD.test(method) && isSameOrigin(url)) {
            var token = readCookie('XSRF-TOKEN');
            if (token) {
                var headers = new Headers(init.headers || (input && input.headers) || undefined);
                if (!headers.has('X-XSRF-TOKEN')) headers.set('X-XSRF-TOKEN', token);
                init.headers = headers;
            }
        }
        return originalFetch.call(this, input, init);
    };

    // Form POST thường (VD: đăng nhập): bảo đảm có trường ẩn _csrf
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (!form || !form.method || form.method.toLowerCase() !== 'post') return;
        var input = form.querySelector('input[name="_csrf"]');
        if (!input) {
            input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            form.appendChild(input);
        }
        if (!input.value) input.value = readCookie('XSRF-TOKEN');
    }, true);
})();
