/*
 * ticket-card.js - Thẻ khách (ca) DÙNG CHUNG cho Danh sách ca (dashboard) và Khách đã hẹn (appointments).
 * Hai trang cùng gọi TicketCard.build(item, options) nên phần thông tin khách luôn giống hệt nhau:
 *   tên + mức ưu tiên + ghim, trạng thái, tài khoản + "Trùng xN", loại ca / mã phản ánh, địa chỉ, tủ KMZ, khoảng cách,
 *   nội dung: ca PHẢN ÁNH = đoạn nội dung khiếu nại; ca HẠ TẦNG = 4 ô số liệu nổi bật (xem TILE_CONFIG) + "xem thêm" các thông số còn lại,
 *   ghi chú gọi, nút gọi điện, chụp ảnh, nút Cập nhật ca, khu vực + người phụ trách.
 * Mỗi trang chỉ khác ở phần riêng của mình, truyền qua options (không đổi phần thông tin ở trên):
 *   options.statusBadge   HTML nhãn trạng thái của trang
 *   options.cardClass     class thêm cho thẻ (vd. "overdue", "is-selected")
 *   options.topHtml       HTML đặt trên cùng thẻ (vd. ô chọn nhiều của dashboard)
 *   options.apptHtml      HTML giờ hẹn; options.apptFirst = true thì đặt ngay dưới tên khách, ngược lại đặt dưới dòng tài khoản
 *   options.processCall   hàm (item) => chuỗi gọi hàm khi bấm nút Cập nhật ca, vd. "openModal(5)"
 *   options.toolbarHtml   HTML thanh thao tác nhanh của trang (Hẹn giờ / KLL / ... hay Bỏ hẹn / KLL / Hoàn tất)
 * Cần: Font Awesome, css/ticket-card.css (kiểu thẻ mới, lớp tc2) cùng CSS thẻ cũ của trang. Tự chứa, không phụ thuộc hàm của trang.
 * Hàm onclick toàn cục: toggleC (xem thêm nội dung khiếu nại), toggleAttrs (xem thêm thông số hạ tầng),
 * openTicketDetail (nút "Chi tiết": mở hộp thoại dùng chung fragments/ticket-detail-modal.jsp; trang cần include fragment đó
 * và có biến ticketCache như dashboard / appointments).
 */
const TicketCard = (function () {
    /*
     * 4 ô số liệu nổi bật của từng loại ca hạ tầng, khớp theo NHÃN thông số mà server gửi (TicketType trong enum):
     *   label: nhãn thông số ở server; title: nhãn ngắn hiện trên ô (bỏ qua = dùng nhãn server);
     *   alert: true = giá trị là số > 0 thì tô đỏ (vd. đang thiếu thiết bị).
     * Thông số không nằm trong 4 ô sẽ vào phần "xem thêm". Loại không khai báo ở đây, hoặc nhãn trên server đã đổi
     * (khớp được ít hơn 2 ô), thì tự lấy 4 thông số đầu tiên.
     */
    const TILE_CONFIG = {
        LAP_THIEU_HOMEWIFI: [
            { label: 'HomeWifi theo gói cước', title: 'Theo gói cước' },
            { label: 'HomeWifi đang sử dụng', title: 'Đang sử dụng' },
            { label: 'Hiện tại thiếu so với gói', title: 'Hiện thiếu so với gói', alert: true },
            { label: 'Lắp lúc triển khai' }
        ],
        PORT_KEM: [
            { label: 'Công suất thu' },
            { label: 'Thời gian tồn' },
            { label: 'Thiết bị' },
            { label: 'Port' }
        ],
        LAN_HOMEWIFI_KEM: [
            { label: 'Lỗi' },
            { label: 'Trạng thái xử lý' },
            { label: 'Số ngày liên tiếp không đạt', title: 'Ngày liên tiếp không đạt' },
            { label: 'Trạng thái online' }
        ]
    };

    function esc(s) {
        return String(s === null || s === undefined ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    // Màu nền theo mức ưu tiên: Hot = đỏ, Nguy cơ HOT = vàng, Bình thường = xanh
    function priorityStyle(level) {
        const v = String(level || '').trim().toUpperCase();
        if (v.indexOf('NGUY CƠ') !== -1) return 'background:#facc15;color:#422006;';
        if (v.indexOf('HOT') !== -1) return 'background:#dc2626;color:#fff;';
        if (v.indexOf('BÌNH THƯỜNG') !== -1) return 'background:#16a34a;color:#fff;';
        return 'background:#f1f5f9;color:#475569;border:1px solid #e2e8f0;';
    }

    function isNumeric(v) {
        return /^[-+]?\d+([.,]\d+)?$/.test(String(v === null || v === undefined ? '' : v).trim());
    }

    function tileHtml(t) {
        const a = t.attr;
        const raw = a.value === null || a.value === undefined ? '' : String(a.value).trim();
        const shown = raw === '' ? '—' : raw + (a.unit ? ' ' + a.unit : '');
        const numeric = raw !== '' && isNumeric(raw);
        let cls = 'tc-tile-value';
        if (!numeric || shown.length > 10) cls += ' is-text';
        if (t.alert && numeric && Number(raw.replace(',', '.')) > 0) cls += ' is-alert';
        return '<div class="tc-tile"><div class="tc-tile-label">' + esc(t.title) + '</div>'
            + '<div class="' + cls + '">' + esc(shown) + '</div></div>';
    }

    function detailCell(a) {
        return '<div><span class="attr-label">' + esc(a.label) + '</span><span class="attr-value">'
            + esc(a.value) + (a.unit ? ' ' + esc(a.unit) : '') + '</span></div>';
    }

    // Ca hạ tầng: 4 ô số liệu nổi bật + "xem thêm" các thông số còn lại
    function attributesHtml(item) {
        const attrs = item.attributeItems || [];
        if (attrs.length === 0) return '<div class="text-muted">Chưa có thông số</div>';

        let tiles = [];
        (TILE_CONFIG[item.ticketType] || []).forEach(function (c) {
            const attr = attrs.find(function (x) { return x.label === c.label; });
            if (attr) tiles.push({ attr: attr, title: c.title || attr.label, alert: !!c.alert });
        });
        if (tiles.length < 2) {
            tiles = attrs.slice(0, 4).map(function (a) { return { attr: a, title: a.label, alert: false }; });
        }
        const used = tiles.map(function (t) { return t.attr; });
        const rest = attrs.filter(function (a) { return used.indexOf(a) === -1; });

        return '<div class="tc-tiles">' + tiles.map(tileHtml).join('') + '</div>'
            + (rest.length ? '<div class="attr-grid d-none mt-2" id="a-' + Number(item.id) + '">' + rest.map(detailCell).join('') + '</div>'
                + '<a class="see-more-link" onclick="toggleAttrs(' + Number(item.id) + ', this)">...xem thêm</a>' : '');
    }

    function build(item, o) {
        o = o || {};
        const id = Number(item.id);
        const isInfra = item.ticketGroup === 'HA_TANG';
        const dupBadge = (item.duplicateCount && item.duplicateCount >= 2)
            ? '<span class="badge-dup">Trùng x' + Number(item.duplicateCount) + '</span>' : '';
        const priority = item.priorityLevel
            ? '<span class="badge ms-1 vt-fs-70" style="' + priorityStyle(item.priorityLevel) + '">' + esc(item.priorityLevel) + '</span>' : '';
        const pinned = item.isPinned === true ? '<i class="fa-solid fa-star text-warning ms-1" title="Đã ghim"></i>' : '';

        let kmzHtml = '';
        if (item.cabinetCode) {
            kmzHtml = item.googleMapsUrl
                ? '<a href="' + esc(item.googleMapsUrl) + '" target="_blank" rel="noopener" class="btn-kmz-map has-kmz"><i class="fa-solid fa-map-pin"></i>Tủ: ' + esc(item.cabinetCode) + '</a>'
                : '<span class="btn-kmz-map no-kmz"><i class="fa-solid fa-server"></i>Tủ: ' + esc(item.cabinetCode) + ' (Chưa có KMZ)</span>';
        }
        const distBadge = (item.distanceKm !== null && item.distanceKm !== undefined)
            ? '<span class="badge-distance"><i class="fa-solid fa-route me-1"></i>' + esc(item.distanceKm) + ' km</span>' : '';

        // Ca phản ánh: đoạn nội dung khiếu nại; ca hạ tầng: ô số liệu + thông số
        const complaint = esc(item.complaintContent || 'Không có nội dung');
        const isLong = complaint.length > 90;
        const contentHtml = isInfra
            ? attributesHtml(item)
            : '<div class="complaint-box"><div class="' + (isLong ? 'complaint-collapsed' : '') + '" id="c-' + id + '">' + complaint + '</div>'
                + (isLong ? '<a class="see-more-link" onclick="toggleC(' + id + ')">...xem thêm</a>' : '') + '</div>';
        const refLabel = isInfra
            ? '<span class="badge text-bg-' + esc(item.ticketTypeColor || 'secondary') + ' vt-fs-72">' + esc(item.ticketTypeLabel) + '</span>'
            : 'PA: #' + esc(item.ticketCode);

        let phones = '';
        if (item.contactPhone) {
            phones += '<a href="tel:' + esc(item.contactPhone) + '" class="btn-call-phone"><i class="fa-solid fa-phone"></i>Gọi KH (' + esc(item.contactPhone) + ')</a>';
        }
        if (item.reportPhone && item.reportPhone !== item.contactPhone) {
            phones += '<a href="tel:' + esc(item.reportPhone) + '" class="btn-call-phone btn-call-report"><i class="fa-solid fa-phone-volume"></i>Báo cáo</a>';
        }

        const appt = o.apptHtml || '';
        const process = typeof o.processCall === 'function' ? o.processCall(item) : 'openModal(' + id + ')';

        return '<div class="ticket-card tc2 ' + esc(o.cardClass || '') + '">'
            + (o.topHtml || '')
            + '<div class="card-header-row"><div>'
            +   '<span class="customer-title">' + esc(item.customerName || 'Khách hàng') + '</span>' + priority + pinned
            + '</div><div>' + (o.statusBadge || '') + '</div></div>'
            + (o.apptFirst ? appt : '')
            + '<div class="meta-info-row"><div><span class="account-tag">' + esc(item.subscriberAccount || item.ticketCode) + '</span>' + dupBadge + '</div>'
            +   '<span class="text-muted vt-fs-78">' + refLabel + '</span></div>'
            + (o.apptFirst ? '' : appt)
            + '<div class="location-section">'
            +   '<div><i class="fa-solid fa-location-dot text-danger me-1"></i>' + esc(item.installAddress || 'Chưa có địa chỉ') + '</div>'
            +   '<div>' + distBadge + kmzHtml + '</div>'
            + '</div>'
            + '<div class="tc-body">' + contentHtml
            +   (item.callNote ? '<div class="text-muted fst-italic mt-1 vt-fs-78"><i class="fa-regular fa-note-sticky me-1"></i>' + esc(item.callNote) + '</div>' : '')
            + '</div>'
            + '<div class="tc-actions">'
            +   (phones ? '<div class="tc-phones">' + phones + '</div>' : '')
            +   '<div class="tc-main">'
            +     '<button type="button" class="btn-camera" title="Chụp ảnh hiện trường" onclick="openCamera(' + id + ')"><i class="fa-solid fa-camera"></i>'
            +       (item.photoCount > 0 ? '<span class="photo-count">' + Number(item.photoCount) + '</span>' : '') + '</button>'
            +     '<button type="button" class="btn-detail" title="Chi tiết" aria-label="Xem chi tiết ca" onclick="openTicketDetail(' + id + ')"><i class="fa-solid fa-circle-info"></i></button>'
            +     '<button type="button" class="btn-action-trigger" onclick="' + esc(process) + '"><i class="fa-regular fa-pen-to-square"></i>Cập nhật ca</button>'
            +   '</div>'
            + '</div>'
            + (o.toolbarHtml || '')
            + '<div class="card-footer-info"><span>Khu vực: <b>' + esc(item.area || '') + '</b></span>'
            +   '<span>' + (isInfra ? 'NV triển khai' : 'Phụ trách') + ': <b class="text-dark">' + esc(item.techStaffName || (isInfra ? '—' : 'Chưa phân công')) + '</b></span></div>'
            + '</div>';
    }

    return { build: build, attributesHtml: attributesHtml };
})();

// Xem thêm / thu gọn nội dung khiếu nại (ca phản ánh)
function toggleC(id) {
    const el = document.getElementById('c-' + id);
    if (el) el.classList.toggle('complaint-collapsed');
}

// Xem thêm / thu gọn thông số kỹ thuật (ca hạ tầng)
function toggleAttrs(id, link) {
    const box = document.getElementById('a-' + id);
    if (!box) return;
    const hidden = box.classList.toggle('d-none');
    link.textContent = hidden ? '...xem thêm' : 'thu gọn';
}

// Nút "Chi tiết" trên thẻ: mở hộp thoại chi tiết (js/ticket-detail.js) từ ca đã có sẵn trong ticketCache của trang.
// Trạng thái hiện theo danh mục statusMeta của trang (nếu có); ca đã lưu trữ thì kèm lý do.
function openTicketDetail(id) {
    const item = (typeof ticketCache !== 'undefined' && ticketCache) ? ticketCache[id] : null;
    if (!item || typeof TicketDetail === 'undefined') return;
    const meta = (typeof statusMeta !== 'undefined' && statusMeta) ? statusMeta : null;
    const st = meta && meta.statuses ? meta.statuses.find(function (x) { return x.code === item.appStatus; }) : null;
    const rs = item.archivedAt && meta && meta.archiveReasons
        ? meta.archiveReasons.find(function (x) { return x.code === (item.archiveReason || 'DONE'); }) : null;
    TicketDetail.open(item, { statusLabel: st ? st.label : '', reasonLabel: rs ? rs.label : '' });
}
