/*
 * report-form.js - Trang trung gian trước khi nộp báo cáo (Google Form).
 *  1) Thông tin của ca được điền sẵn (sửa nhanh được).
 *  2) Chọn "Nhóm nhiệm vụ chính" (mẫu đã lưu xếp theo nhóm này).
 *  3) 4 mục viết: chọn mẫu đã lưu hoặc tự viết rồi bấm Lưu để dùng lại lần sau.
 *     Nhân viên thêm / sửa / xóa mẫu thì chờ admin duyệt; để trống mục nào thì form để trống mục đó.
 * Cần các biến CTX, TICKET_ID (khai báo trong report-form.jsp) và app-common.js.
 */

// Mã entry của các mục thông tin ca trong Google Form (đổi form thì cập nhật lại các mã này).
// Mã của "Nhóm nhiệm vụ chính" và 4 mục viết do server cấp (ReportFormConfig / ReportField).
const REPORT_ENTRY = {
    date: '232984631',
    staff: '1386363437',
    area: '977003287',
    serviceType: '1313171066',
    account: '2037624764',
    customerName: '1327480577',
    phone: '464690292',
    address: '1666902213',
    cabinet: '1383431432',
    contractor: '1226051476'
};

// 24 địa bàn trong form (phải ghi đúng như trong form thì Google mới nhận)
const REPORT_AREAS = ['Buôn Ma Thuột', 'Ea Hleo', 'Ea Súp', 'Krông Năng', 'Krông Búk', 'Buôn Đôn', 'Cư Mgar', 'Ea Kar',
    'Mđrắk', 'Krông Pắk', 'Krông A Na', 'Krông Bông', 'Lắk', 'Cư Kuin', 'Buôn Hồ', 'Tuy Hòa', 'Đồng Xuân', 'Sông Cầu',
    'Tuy An', 'Sơn Hòa', 'Sông Hinh', 'Phú Hòa', 'Tây Hòa', 'Đông Hòa'];

// Ô "Loại dịch vụ" của form chỉ cho chọn MỘT giá trị
const SERVICE_TYPES = ['FTTH', 'Truyền hình', 'Mesh', 'Camera', 'Khác'];

// Loại dịch vụ điền sẵn theo loại ca hạ tầng (khai báo ở TicketType phía server)
const SERVICE_BY_TICKET_TYPE = {
    PORT_KEM: 'FTTH',              // Port kém GPON
    LAN_HOMEWIFI_KEM: 'Mesh',      // LAN / HomeWifi kém
    LAP_THIEU_HOMEWIFI: 'Mesh'     // Lắp thiếu HomeWifi
};

let ticket = null;
let meta = null;
let me = null;
const snippets = {};   // mã mục -> danh sách mẫu người này được thấy (mọi nhóm, lọc theo nhóm ở trình duyệt)

function $(id) { return document.getElementById(id); }

document.addEventListener('DOMContentLoaded', init);

async function init() {
    try {
        const r = await Promise.all([
            AppNav.getMe(),
            apiFetch(CTX + '/api/report-form/meta'),
            apiFetch(CTX + '/api/tickets/' + encodeURIComponent(TICKET_ID))
        ]);
        me = r[0];
        meta = r[1];
        ticket = r[2];

        renderInfo();
        renderGroupSelect();
        renderFieldCards();
        await Promise.all(meta.fields.map(function (f) { return loadSnippets(f.code); }));

        if (me && me.admin) $('adminLink').classList.remove('d-none');
        $('rfLoading').classList.add('d-none');
        $('rfMain').classList.remove('d-none');
    } catch (e) {
        $('rfLoading').classList.add('d-none');
        const box = $('rfError');
        box.textContent = e.message;
        box.classList.remove('d-none');
    }
}

// ---------------------------------------------------------------- thông tin ca

// Bỏ dấu, chữ thường, bỏ khoảng trắng / dấu nháy để so khớp (VD "Cư M'gar" = "Cư Mgar", "Ea H'leo" = "Ea Hleo")
function normalizeText(str) {
    return String(str || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd').replace(/Đ/g, 'd').toLowerCase().replace(/[^a-z0-9]/g, '');
}

// Tìm địa bàn của form khớp với khu vực của ca; thử lần lượt: khu vực, cụm xã, xã. Không khớp thì trả null
function guessReportArea(item) {
    const candidates = [item.area, item.communeCluster, item.commune];
    // Tên dài xét trước để các tên gần giống nhau ("Tuy An", "Sơn Hòa", "Phú Hòa"...) không bị nhầm
    const sorted = REPORT_AREAS.slice().sort(function (x, y) { return normalizeText(y).length - normalizeText(x).length; });
    for (let i = 0; i < candidates.length; i++) {
        const text = normalizeText(candidates[i]);
        if (!text) continue;
        for (let j = 0; j < sorted.length; j++) {
            if (text.indexOf(normalizeText(sorted[j])) !== -1) return sorted[j];
        }
    }
    return null;
}

// Ca hạ tầng: theo bảng trên. Ca phản ánh: đoán từ "Tên dịch vụ" (cột H trong file Excel), không đoán được thì để trống
function guessServiceType(item) {
    const byType = SERVICE_BY_TICKET_TYPE[item.ticketType];
    if (byType) return byType;
    const t = (item.serviceName || '').toLowerCase();
    if (/ftth|internet|cáp quang|cap quang|fiber|gpon/.test(t)) return 'FTTH';
    if (/truyền hình|truyen hinh|\btv\b|mytv|nexttv/.test(t)) return 'Truyền hình';
    if (/\bmesh\b|homewifi|home wifi/.test(t)) return 'Mesh';
    if (/camera/.test(t)) return 'Camera';
    return '';
}

function fillSelect(sel, values, selected) {
    sel.innerHTML = '';
    const blank = document.createElement('option');
    blank.value = '';
    blank.textContent = '-- Chưa chọn --';
    sel.appendChild(blank);
    values.forEach(function (v) {
        const o = document.createElement('option');
        o.value = v;
        o.textContent = v;
        sel.appendChild(o);
    });
    sel.value = selected || '';
}

function renderInfo() {
    // Ngày thực hiện = ngày ca được xử lý xong (chuyển vào lịch sử), dạng yyyy-mm-dd
    const d = ticket.archivedAt ? new Date(ticket.archivedAt) : new Date();
    $('infoDate').value = d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
    $('infoStaff').value = ticket.assignedStaff || (me && me.fullName) || '';
    fillSelect($('infoArea'), REPORT_AREAS, guessReportArea(ticket));
    fillSelect($('infoService'), SERVICE_TYPES, guessServiceType(ticket));
    $('infoAccount').value = ticket.subscriberAccount || '';
    $('infoName').value = ticket.customerName || '';
    $('infoPhone').value = ticket.contactPhone || ticket.reportPhone || '';
    $('infoCabinet').value = ticket.cabinetCode || '';
    $('infoAddress').value = ticket.installAddress || '';
    $('infoFt').value = ticket.techStaffName || '';
}

// ---------------------------------------------------------------- nhóm nhiệm vụ

function renderGroupSelect() {
    const sel = $('rfGroup');
    sel.innerHTML = '<option value="">-- Chọn nhóm nhiệm vụ --</option>';
    meta.taskGroups.forEach(function (g) {
        const o = document.createElement('option');
        o.value = g.code;
        o.textContent = g.label;
        sel.appendChild(o);
    });
    sel.value = (meta.suggestedGroupByTicketType && meta.suggestedGroupByTicketType[ticket.ticketType]) || '';
}

function onGroupChange() {
    meta.fields.forEach(function (f) { renderPicker(f.code, null); });
}

// ---------------------------------------------------------------- 4 mục viết

function renderFieldCards() {
    $('rfFields').innerHTML = meta.fields.map(function (f) {
        return '<div class="rf-card" data-field="' + f.code + '">'
            + '<label class="rf-label" for="rfText-' + f.code + '">' + escapeHtml(f.label) + '</label>'
            + '<select class="form-select form-select-sm" id="rfPick-' + f.code + '" onchange="onPick(\'' + f.code + '\')"></select>'
            + '<textarea class="form-control form-control-sm mt-2" id="rfText-' + f.code + '" rows="2" maxlength="' + meta.maxContentLength + '"'
            + ' placeholder="Chọn mẫu ở trên hoặc tự viết. Để trống thì form để trống mục này."></textarea>'
            + '<div class="rf-actions">'
            +   '<button type="button" class="btn btn-outline-success" onclick="doSave(\'' + f.code + '\')"><i class="fa-regular fa-floppy-disk me-1"></i>Lưu thành mẫu</button>'
            +   '<button type="button" class="btn btn-outline-primary" onclick="doEdit(\'' + f.code + '\')"><i class="fa-regular fa-pen-to-square me-1"></i>Sửa mẫu đã chọn</button>'
            +   '<button type="button" class="btn btn-outline-danger" onclick="doDelete(\'' + f.code + '\')"><i class="fa-regular fa-trash-can me-1"></i>Xóa mẫu đã chọn</button>'
            + '</div>'
            + '<div class="rf-note" id="rfNote-' + f.code + '"></div>'
            + '</div>';
    }).join('');
}

async function loadSnippets(fieldCode, selectId) {
    snippets[fieldCode] = await apiFetch(CTX + '/api/report-snippets?field=' + encodeURIComponent(fieldCode));
    renderPicker(fieldCode, selectId);
}

// Mẫu hiển thị theo nhóm đang chọn: mẫu của nhóm đó + mẫu dùng chung; chưa chọn nhóm thì hiện tất cả
function visibleSnippets(fieldCode) {
    const g = $('rfGroup').value;
    return (snippets[fieldCode] || []).filter(function (s) { return !g || !s.taskGroup || s.taskGroup === g; });
}

function optionLabel(s, showGroup) {
    let t = s.content.length > 80 ? s.content.substring(0, 80) + '…' : s.content;
    if (showGroup && s.taskGroupLabel) t = '[' + s.taskGroupLabel + '] ' + t;
    // Nhãn trạng thái do server gửi (SnippetState / SnippetChange), không viết cứng ở đây
    if (s.state && s.state !== 'APPROVED') {
        t += '  ⏳ ' + s.stateLabel + (s.state === 'PENDING' && s.requestLabel ? ' (' + s.requestLabel.toLowerCase() + ')' : '');
    }
    return t;
}

function renderPicker(fieldCode, selectId) {
    const sel = $('rfPick-' + fieldCode);
    const keep = selectId || sel.value;
    const list = visibleSnippets(fieldCode);
    const showGroup = !$('rfGroup').value;
    sel.innerHTML = '';
    const blank = document.createElement('option');
    blank.value = '';
    blank.textContent = list.length ? '-- Chọn mẫu đã lưu (' + list.length + ') --' : '-- Chưa có mẫu nào, hãy viết rồi bấm Lưu --';
    sel.appendChild(blank);
    list.forEach(function (s) {
        const o = document.createElement('option');
        o.value = s.id;
        o.textContent = optionLabel(s, showGroup);
        sel.appendChild(o);
    });
    sel.value = list.some(function (s) { return String(s.id) === String(keep); }) ? String(keep) : '';
    updateNote(fieldCode);
}

function selectedSnippet(fieldCode) {
    const id = $('rfPick-' + fieldCode).value;
    if (!id) return null;
    return (snippets[fieldCode] || []).find(function (s) { return String(s.id) === String(id); }) || null;
}

function updateNote(fieldCode) {
    const note = $('rfNote-' + fieldCode);
    const s = selectedSnippet(fieldCode);
    note.className = 'rf-note';
    if (!s) { note.textContent = ''; return; }
    if (s.stateNote) {
        // Câu giải thích theo loại yêu cầu do server gửi (SnippetChange)
        note.textContent = '⏳ ' + s.stateNote + (s.requestType === 'EDIT' ? ' Nội dung đề nghị: «' + s.pendingContent + '».' : '');
    } else {
        note.classList.add('is-info');
        note.textContent = 'Mẫu đã duyệt' + (s.taskGroupLabel ? ' · nhóm: ' + s.taskGroupLabel : ' · dùng chung mọi nhóm')
            + (s.createdByName ? ' · tạo bởi ' + s.createdByName : '');
    }
}

function onPick(fieldCode) {
    const s = selectedSnippet(fieldCode);
    if (s) $('rfText-' + fieldCode).value = s.content;
    updateNote(fieldCode);
}

async function runAction(fieldCode, call) {
    try {
        const r = await call();
        showToast(r.message, false);
        await loadSnippets(fieldCode, r.snippet ? r.snippet.id : null);
        if (!r.snippet) $('rfPick-' + fieldCode).value = '';
        updateNote(fieldCode);
    } catch (e) {
        showToast(e.message, true);
    }
}

function jsonBody(method, obj) {
    return { method: method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(obj) };
}

function doSave(fieldCode) {
    const text = $('rfText-' + fieldCode).value.trim();
    if (!text) { showToast('Hãy nhập nội dung trước khi lưu', true); return; }
    const group = $('rfGroup').value;
    const toGroup = $('rfSaveToGroup').checked && group;
    runAction(fieldCode, function () {
        return apiFetch(CTX + '/api/report-snippets', jsonBody('POST',
            { field: fieldCode, taskGroup: toGroup ? group : null, content: text }));
    });
}

function doEdit(fieldCode) {
    const s = selectedSnippet(fieldCode);
    if (!s) { showToast('Hãy chọn một mẫu để sửa', true); return; }
    const text = $('rfText-' + fieldCode).value.trim();
    if (!text) { showToast('Nội dung mới không được để trống', true); return; }
    runAction(fieldCode, function () {
        return apiFetch(CTX + '/api/report-snippets/' + s.id, jsonBody('PUT', { content: text }));
    });
}

function doDelete(fieldCode) {
    const s = selectedSnippet(fieldCode);
    if (!s) { showToast('Hãy chọn một mẫu để xóa', true); return; }
    // directDelete do server tính: admin, hoặc người tạo xóa mẫu chưa duyệt; còn lại phải chờ admin duyệt
    const msg = s.directDelete
        ? 'Xóa mẫu này?'
        : 'Gửi yêu cầu xóa mẫu này? Mẫu chỉ bị xóa sau khi admin duyệt.';
    if (!confirm(msg)) return;
    runAction(fieldCode, function () {
        return apiFetch(CTX + '/api/report-snippets/' + s.id, { method: 'DELETE' });
    });
}

// ---------------------------------------------------------------- mở Google Form

function buildReportUrl() {
    const params = new URLSearchParams();
    params.set('usp', 'pp_url');
    const add = function (entry, value) {
        if (value !== null && value !== undefined && String(value).trim() !== '') params.append('entry.' + entry, String(value).trim());
    };
    add(REPORT_ENTRY.date, $('infoDate').value);
    add(REPORT_ENTRY.staff, $('infoStaff').value);
    add(REPORT_ENTRY.area, $('infoArea').value);
    add(REPORT_ENTRY.serviceType, $('infoService').value);
    add(REPORT_ENTRY.account, $('infoAccount').value);
    add(REPORT_ENTRY.customerName, $('infoName').value);
    add(REPORT_ENTRY.phone, $('infoPhone').value);
    add(REPORT_ENTRY.address, $('infoAddress').value);
    add(REPORT_ENTRY.cabinet, $('infoCabinet').value);
    add(REPORT_ENTRY.contractor, $('infoFt').value);

    const group = meta.taskGroups.find(function (g) { return g.code === $('rfGroup').value; });
    if (group) add(meta.taskGroupEntry, group.label);
    // 4 mục viết: ô nào để trống thì không điền
    meta.fields.forEach(function (f) { add(f.entryId, $('rfText-' + f.code).value); });
    return meta.formUrl + '?' + params.toString();
}

function openGoogleForm() {
    window.open(buildReportUrl(), '_blank', 'noopener');
}
