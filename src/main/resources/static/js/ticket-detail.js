/*
 * ticket-detail.js - Hộp thoại "Chi tiết ca" dùng chung (đi kèm fragments/ticket-detail-modal.jsp).
 * Gọi: TicketDetail.open(item, { statusLabel, reasonLabel, showReport, onNotify }); đóng: TicketDetail.close().
 * Mọi tùy chọn đều không bắt buộc: bỏ trống thì dòng tương ứng tự ẩn. Không truyền onNotify thì tự hiện thông báo nhỏ riêng.
 * Đang dùng ở: Lịch sử (history.js), Danh sách ca và Khách đã hẹn (nút "Chi tiết" trong ticket-card.js).
 * item là TicketResponseDTO đã có sẵn trong danh sách của trang, nên không gọi thêm API.
 * Tự lo: đóng bằng nút X / nút Đóng / bấm nền mờ / phím Esc, trả focus về nút đã mở, sao chép mã phản ánh.
 */
const TicketDetail = (function () {
    const $ = function (id) { return document.getElementById(id); };
    let opener = null;
    let notify = null;
    let toastTimer = null;

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

        $('tdTitle').textContent = item.customerName || 'Chi tiết ca';
        $('tdSub').textContent = 'Chi tiết ca · ' + (item.subscriberAccount || item.ticketCode || '');
        $('tdBody').innerHTML = html;
        $('tdBody').scrollTop = 0;
        $('tdOverlay').classList.add('show');
        $('tdOverlay').setAttribute('aria-hidden', 'false');
        $('tdClose').focus();
    }

    function close() {
        const o = $('tdOverlay');
        if (!o || !o.classList.contains('show')) return;
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
        document.addEventListener('keydown', function (e) { if (e.key === 'Escape') close(); });
    }

    return { open: open, close: close };
})();
