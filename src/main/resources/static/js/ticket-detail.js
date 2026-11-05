/*
 * ticket-detail.js - Hộp thoại "Chi tiết ca" dùng chung (đi kèm fragments/ticket-detail-modal.jsp).
 * Gọi: TicketDetail.open(item, { statusLabel, reasonLabel, showReport, onNotify }); đóng: TicketDetail.close().
 * Mọi tùy chọn đều không bắt buộc: bỏ trống thì dòng tương ứng tự ẩn. Không truyền onNotify thì tự hiện thông báo nhỏ riêng.
 * Đang dùng ở: Lịch sử (history.js), Danh sách ca và Khách đã hẹn (nút "Chi tiết" trong ticket-card.js).
 * item là TicketResponseDTO đã có sẵn trong danh sách của trang, nên không gọi thêm API.
 * Tự lo: đóng bằng nút X / nút Đóng / bấm nền mờ / phím Esc, trả focus về nút đã mở, sao chép mã phản ánh.
 * Ảnh hiện trường: mỗi lần mở, tự gọi GET /api/tickets/{id}/photos để hiện toàn bộ ảnh chụp bằng camera của ca đó
 * (lưới ảnh thu nhỏ, bấm vào để xem lớn, có nút trước / sau). Cần biến CTX của trang (context path); không có thì coi là rỗng.
 */
const TicketDetail = (function () {
    const $ = function (id) { return document.getElementById(id); };
    let opener = null;
    let notify = null;
    let toastTimer = null;
    let photos = [];       // ảnh của ca đang mở
    let photoToken = 0;    // tăng mỗi lần mở / đóng để bỏ qua kết quả tải ảnh của lần mở cũ
    let lbIndex = -1;      // ảnh đang xem lớn (-1 = đang đóng)

    function ctx() { return typeof CTX !== 'undefined' ? CTX : ''; }

    function esc(str) {
        return String(str === null || str === undefined ? '' : str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function fmt(v) {
        if (!v) return '';
        const d = new Date(v);
        const p = function (n) { return String(n).padStart(2, '0'); };
        return p(d.getHours()) + ':' + p(d.getMinutes()) + ' ' + p(d.getDate()) + '/' + p(d.getMonth() + 1) + '/' + d.getFullYear();
    }

    // Một ô "nhãn + giá trị"; giá trị rỗng thì bỏ qua ô. value là HTML đã được escape sẵn
    function row(label, value, wide) {
        if (value === null || value === undefined || String(value).trim() === '') return '';
        return '<div' + (wide ? ' class="wide"' : '') + '><small>' + esc(label) + '</small><strong>' + value + '</strong></div>';
    }

    function phoneLink(p) {
        return p ? '<a href="tel:' + esc(p) + '">' + esc(p) + '</a>' : '';
    }

    // Thông báo nhỏ: dùng hàm của trang nếu có, không thì tự hiện (trang như Dashboard không có sẵn hàm toast)
    function say(msg, isError) {
        if (typeof notify === 'function') { notify(msg, isError); return; }
        let el = $('tdToast');
        if (!el) {
            el = document.createElement('div');
            el.id = 'tdToast';
            el.setAttribute('role', 'status');
            document.body.appendChild(el);
        }
        el.className = 'td-toast' + (isError ? ' error' : '');
        el.textContent = msg;
        el.style.display = 'block';
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () { el.style.display = 'none'; }, isError ? 4200 : 2800);
    }

    function copyText(text) {
        const fallback = function () {
            const ta = document.createElement('textarea');
            ta.value = text;
            ta.style.cssText = 'position:fixed;opacity:0';
            document.body.appendChild(ta);
            ta.select();
            try { document.execCommand('copy'); say('Đã sao chép mã phản ánh.'); } catch (_) { say('Không sao chép được', true); }
            ta.remove();
        };
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(function () { say('Đã sao chép mã phản ánh.'); }).catch(fallback);
        } else {
            fallback();
        }
    }


    // ===================== ẢNH HIỆN TRƯỜNG =====================
    function photoSection(inner) {
        return '<div class="td-sec" id="tdPhotos"><h4><i class="fa-solid fa-camera me-1"></i>Ảnh hiện trường</h4>' + inner + '</div>';
    }

    function setPhotoHtml(inner) {
        const box = $('tdPhotos');
        if (box) box.innerHTML = '<h4><i class="fa-solid fa-camera me-1"></i>Ảnh hiện trường'
            + (photos.length ? ' <span class="td-ph-n">' + photos.length + '</span>' : '') + '</h4>' + inner;
    }

    async function loadPhotos(ticketId) {
        const token = ++photoToken;
        photos = [];
        try {
            const res = await fetch(ctx() + '/api/tickets/' + encodeURIComponent(ticketId) + '/photos', {
                headers: { 'Accept': 'application/json' }, credentials: 'same-origin'
            });
            if (token !== photoToken) return;
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();
            if (token !== photoToken) return;
            photos = Array.isArray(data) ? data : [];
        } catch (_) {
            if (token !== photoToken) return;
            setPhotoHtml('<p class="td-ph-state">Không tải được ảnh.</p>');
            return;
        }
        if (!photos.length) {
            setPhotoHtml('<p class="td-ph-state">Ca này chưa có ảnh chụp.</p>');
            return;
        }
        setPhotoHtml('<div class="td-ph-grid">' + photos.map(function (p, i) {
            return '<button type="button" class="td-ph" data-ph="' + i + '" aria-label="Xem ảnh ' + (i + 1) + '">'
                + '<img src="' + esc(ctx() + (p.thumbUrl || p.url)) + '" loading="lazy" alt="Ảnh hiện trường ' + (i + 1) + '">'
                + '<span>' + esc(fmt(p.takenAt)) + '</span></button>';
        }).join('') + '</div>');
    }

    function showPhoto(i) {
        if (!photos.length) return;
        lbIndex = (i + photos.length) % photos.length;
        const p = photos[lbIndex];
        const img = $('tdLbImg');
        img.onerror = function () { img.alt = 'Ảnh không còn trên máy chủ (có thể đã tự xóa).'; };
        img.alt = 'Ảnh hiện trường';
        img.src = ctx() + (p.url || p.fileUrl);
        let meta = '<b>' + esc(p.takenByName || p.takenByUsername || '—') + '</b> · ' + esc(fmt(p.takenAt))
            + ' · ' + (lbIndex + 1) + '/' + photos.length;
        if (p.address) meta += '<br>' + esc(p.address);
        if (p.latitude !== null && p.latitude !== undefined && p.longitude !== null && p.longitude !== undefined) {
            meta += '<br><a href="https://www.google.com/maps?q=' + encodeURIComponent(p.latitude + ',' + p.longitude)
                + '" target="_blank" rel="noopener">' + esc(Number(p.latitude).toFixed(6) + ', ' + Number(p.longitude).toFixed(6)) + '</a>';
        }
        $('tdLbMeta').innerHTML = meta;
        const many = photos.length > 1;
        $('tdLbPrev').style.display = many ? '' : 'none';
        $('tdLbNext').style.display = many ? '' : 'none';
        $('tdLb').classList.add('show');
        $('tdLb').setAttribute('aria-hidden', 'false');
    }

    function closePhoto() {
        const lb = $('tdLb');
        if (!lb || !lb.classList.contains('show')) return false;
        lb.classList.remove('show');
        lb.setAttribute('aria-hidden', 'true');
        $('tdLbImg').removeAttribute('src');
        lbIndex = -1;
        return true;
    }

    function open(item, opts) {
        if (!item || !$('tdOverlay')) return;
        opts = opts || {};
        notify = opts.onNotify || null;
        opener = document.activeElement;

        const maps = item.cabinetCode
            ? (item.googleMapsUrl
                ? '<a href="' + esc(item.googleMapsUrl) + '" target="_blank" rel="noopener"><i class="fa-solid fa-map-pin me-1"></i>' + esc(item.cabinetCode) + '</a>'
                : esc(item.cabinetCode) + ' (chưa có KMZ)')
            : '';
        const tech = item.techStaffName
            ? esc(item.techStaffName) + (item.techStaffPhone ? ' · ' + phoneLink(item.techStaffPhone) : '') : '';
        const code = item.ticketCode
            ? esc('#' + item.ticketCode) + ' <button type="button" class="td-copy" data-copy="' + esc(item.ticketCode) + '"><i class="fa-regular fa-copy"></i> Sao chép</button>'
            : '';

        let html = '<div class="td-grid">'
            + row('Khách hàng', esc(item.customerName || ''))
            + row('Tài khoản', esc(item.subscriberAccount || ''))
            + row('Mã phản ánh', code, true)
            + row('SĐT khách', phoneLink(item.contactPhone))
            + row('SĐT nhập báo cáo', item.reportPhone !== item.contactPhone ? phoneLink(item.reportPhone) : '')
            + row('Địa chỉ', esc(item.installAddress || 'Chưa có địa chỉ'), true)
            + row('Khu vực', esc([item.area, item.commune].filter(Boolean).join(' · ')))
            + row('NV quản lý', esc(item.assignedStaff || ''))
            + row('Kỹ thuật phụ trách', tech)
            + row('Tủ kết nối', maps)
            + row('Loại ca', esc(item.ticketTypeLabel || ''))
            + row('Mức ưu tiên', esc(item.priorityLevel || ''))
            + row('Trạng thái', esc(opts.statusLabel || ''))
            + row('Trùng lặp', item.duplicateCount >= 2 ? esc('Trùng x' + item.duplicateCount) : '')
            + row('Lý do vào lịch sử', esc(opts.reasonLabel || ''))
            + row('Thời gian vào lịch sử', esc(fmt(item.archivedAt)))
            + row('Giờ hẹn', esc(item.appointmentTime || ''))
            + (opts.showReport ? row('Báo cáo', esc(item.reportStatusLabel || 'Chưa nộp') + (item.reportUpdatedAt ? ' · ' + esc(fmt(item.reportUpdatedAt)) : '')) : '')
            + '</div>';

        if (item.complaintContent) html += '<div class="td-sec"><h4>Nội dung phản ánh</h4><p>' + esc(item.complaintContent) + '</p></div>';
        if (item.gnocProgress) html += '<div class="td-sec"><h4>Tiến độ GNOC</h4><p>' + esc(item.gnocProgress) + '</p></div>';
        if (item.attributeItems && item.attributeItems.length) {
            html += '<div class="td-sec"><h4>Thông số</h4><div class="td-attrs">' + item.attributeItems.map(function (a) {
                return '<div><small>' + esc(a.label) + '</small><strong>' + esc(a.value) + (a.unit ? ' ' + esc(a.unit) : '') + '</strong></div>';
            }).join('') + '</div></div>';
        }
        if (item.callNote) html += '<div class="td-sec"><h4>Ghi chú cuộc gọi</h4><p>' + esc(item.callNote) + '</p></div>';

        // Ảnh hiện trường: khung hiện ngay, ảnh tải xong thì điền vào
        html += photoSection('<p class="td-ph-state">Đang tải ảnh...</p>');

        $('tdTitle').textContent = item.customerName || 'Chi tiết ca';
        $('tdSub').textContent = 'Chi tiết ca · ' + (item.subscriberAccount || item.ticketCode || '');
        $('tdBody').innerHTML = html;
        $('tdBody').scrollTop = 0;
        $('tdOverlay').classList.add('show');
        $('tdOverlay').setAttribute('aria-hidden', 'false');
        $('tdClose').focus();
        loadPhotos(item.id);
    }

    function close() {
        const o = $('tdOverlay');
        if (!o || !o.classList.contains('show')) return;
        closePhoto();
        photoToken++;
        photos = [];
        o.classList.remove('show');
        o.setAttribute('aria-hidden', 'true');
        if (opener && typeof opener.focus === 'function' && document.contains(opener)) opener.focus();
        opener = null;
    }

    if ($('tdOverlay')) {
        $('tdClose').addEventListener('click', close);
        $('tdOk').addEventListener('click', close);
        $('tdOverlay').addEventListener('click', function (e) { if (e.target === this) close(); });
        $('tdBody').addEventListener('click', function (e) {
            const c = e.target.closest('[data-copy]');
            if (c) copyText(c.dataset.copy);
        });
        $('tdBody').addEventListener('click', function (e) {
            const b = e.target.closest('[data-ph]');
            if (b) showPhoto(Number(b.dataset.ph));
        });
        $('tdLbClose').addEventListener('click', closePhoto);
        $('tdLbPrev').addEventListener('click', function () { showPhoto(lbIndex - 1); });
        $('tdLbNext').addEventListener('click', function () { showPhoto(lbIndex + 1); });
        $('tdLb').addEventListener('click', function (e) { if (e.target === this) closePhoto(); });
        document.addEventListener('keydown', function (e) {
            const lbOpen = $('tdLb').classList.contains('show');
            if (e.key === 'Escape') {
                if (lbOpen) { closePhoto(); e.stopPropagation(); } else { close(); }
            } else if (lbOpen && e.key === 'ArrowLeft') { showPhoto(lbIndex - 1); }
            else if (lbOpen && e.key === 'ArrowRight') { showPhoto(lbIndex + 1); }
        });
    }

    return { open: open, close: close };
})();
