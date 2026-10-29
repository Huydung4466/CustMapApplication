/*
 * report-snippets.js - Trang quản lý mẫu báo cáo (/report-snippets).
 *  - Mọi tài khoản: thêm mẫu, xem mẫu đã duyệt, sửa / xóa mẫu của mình, gửi đề nghị sửa / xóa (chờ admin duyệt),
 *    xem lý do khi mẫu bị từ chối và sửa gửi lại, xem lịch sử mẫu do mình tạo.
 *  - Admin: duyệt, sửa rồi duyệt, từ chối (kèm lý do), sửa / xóa trực tiếp, xem nhật ký thao tác.
 * Nhãn / biểu tượng / màu trạng thái do server gửi kèm từng mẫu (SnippetState), trang không viết cứng.
 * Quyền thật được kiểm tra ở server; ở đây chỉ ẩn / hiện nút cho gọn.
 * Cần biến CTX (khai báo trong report-snippets.jsp) và app-common.js; me lấy từ AppNav.getMe().
 */
let meta = null;
let me = null;
let items = [];
let logOpen = false;

function $(id) { return document.getElementById(id); }

document.addEventListener('DOMContentLoaded', init);

async function init() {
    try {
        const r = await Promise.all([AppNav.getMe(), apiFetch(CTX + '/api/report-form/meta')]);
        me = r[0];
        meta = r[1];
        fillForms();
        if (me.admin) {
            $('pendingWrap').classList.remove('d-none');
            $('logWrap').classList.remove('d-none');
        } else {
            $('mineWrap').classList.remove('d-none');
        }
        await reload();
        focusFromUrl();
    } catch (e) {
        $('allList').innerHTML = '<div class="alert alert-danger small">' + escapeHtml(e.message) + '</div>';
    }
}

function optionHtml(value, label) {
    return '<option value="' + escapeHtml(value) + '">' + escapeHtml(label) + '</option>';
}

function fillForms() {
    const fields = meta.fields.map(function (x) { return optionHtml(x.code, x.label); }).join('');
    const groups = meta.taskGroups.map(function (x) { return optionHtml(x.code, x.label); }).join('');
    $('addField').innerHTML = fields;
    $('addGroup').innerHTML = optionHtml('', 'Dùng chung mọi nhóm') + groups;
    $('fieldFilter').innerHTML = optionHtml('', 'Tất cả mục') + fields;
    $('groupFilter').innerHTML = optionHtml('', 'Tất cả nhóm') + optionHtml('__COMMON__', 'Dùng chung mọi nhóm') + groups;
    // Mẫu đã xóa thật không còn dòng nào nên không đưa trạng thái "đã xóa" vào bộ lọc
    $('stateFilter').innerHTML = optionHtml('', 'Mọi trạng thái') + meta.states
        .filter(function (s) { return s.code !== 'DELETED'; })
        .map(function (s) { return optionHtml(s.code, s.label); }).join('');

    $('addContent').maxLength = meta.maxContentLength;
    $('addMax').textContent = meta.maxContentLength;
    $('addContent').addEventListener('input', function () { $('addCounter').textContent = $('addContent').value.length; });
    $('btnAddText').textContent = me.admin ? 'Lưu mẫu' : 'Gửi mẫu';
    $('addNote').textContent = me.admin ? 'Mẫu bạn thêm có hiệu lực ngay.' : 'Mẫu bạn gửi sẽ chờ admin duyệt; chỉ bạn và admin thấy cho tới khi được duyệt.';
}

async function reload() {
    items = await apiFetch(CTX + '/api/report-snippets/manage');
    renderPending();
    renderMine();
    renderAll();
    if (logOpen) await loadLog();
    if (window.AppNotify) AppNotify.refresh();
}

// ---------------------------------------------------------------- hiển thị

function groupText(s) { return s.taskGroupLabel ? 'Nhóm: ' + s.taskGroupLabel : 'Dùng chung mọi nhóm'; }

function stateBadge(s) {
    return '<span class="st-badge ' + escapeHtml(s.stateBadge) + '"><i class="' + escapeHtml(s.stateIcon) + '"></i>' + escapeHtml(s.stateLabel) + '</span>';
}

function requestPill(s) {
    return s.requestType ? '<span class="' + escapeHtml(s.requestPill) + '">' + escapeHtml(s.requestLabel) + '</span>' : '';
}

// Nội dung chính của thẻ: yêu cầu sửa thì hiện nội dung cũ (gạch) và nội dung đề xuất
function bodyHtml(s) {
    if (s.requestType === 'EDIT') {
        return '<div class="sn-old">' + escapeHtml(s.content) + '</div>'
            + '<div class="sn-text mt-1"><i class="fa-solid fa-arrow-right text-primary me-1"></i>' + escapeHtml(s.pendingContent) + '</div>';
    }
    return '<div class="sn-text">' + escapeHtml(s.content) + '</div>';
}

function metaLine(s) {
    let t = 'Tạo bởi <b>' + escapeHtml(s.createdByName || '') + '</b> · ' + escapeHtml(formatDateTime(s.createdAt));
    if (s.requestType === 'EDIT' || s.requestType === 'DELETE') {
        t += '<br>Người đề nghị: <b>' + escapeHtml(s.changeByName || '') + '</b> · ' + escapeHtml(formatDateTime(s.changeAt));
    }
    if (s.approvedByName && s.state === 'APPROVED') {
        t += '<br>Duyệt bởi <b>' + escapeHtml(s.approvedByName) + '</b> · ' + escapeHtml(formatDateTime(s.approvedAt));
    }
    return t;
}

function btnHtml(cls, icon, text, onclick) {
    return '<button type="button" class="btn ' + cls + '" onclick="' + onclick + '"><i class="' + icon + ' me-1"></i>' + text + '</button>';
}

// Nút theo quyền: có hiệu lực ngay thì "Sửa / Xóa", cần admin duyệt thì "Đề nghị sửa / xóa"
function actionButtons(s) {
    const b = [];
    const waiting = !!s.pendingChange;
    if (s.state === 'REJECTED') {
        if (s.owner) b.push(btnHtml('btn-outline-primary', 'fa-solid fa-rotate-right', 'Sửa &amp; gửi lại', 'editSnippet(' + s.id + ')'));
    } else if (s.directEdit) {
        b.push(btnHtml('btn-outline-primary', 'fa-regular fa-pen-to-square', 'Sửa', 'editSnippet(' + s.id + ')'));
    } else if (s.status === 'APPROVED' && !waiting) {
        b.push(btnHtml('btn-outline-primary', 'fa-regular fa-pen-to-square', 'Đề nghị sửa', 'editSnippet(' + s.id + ')'));
    }
    if (s.directDelete) {
        b.push(btnHtml('btn-outline-danger', 'fa-regular fa-trash-can', 'Xóa', 'deleteSnippet(' + s.id + ')'));
    } else if (s.status === 'APPROVED' && !waiting) {
        b.push(btnHtml('btn-outline-danger', 'fa-regular fa-trash-can', 'Đề nghị xóa', 'deleteSnippet(' + s.id + ')'));
    }
    if (me.admin || s.owner) {
        b.push(btnHtml('btn-outline-secondary', 'fa-solid fa-clock-rotate-left', 'Lịch sử', 'toggleHistory(' + s.id + ', this)'));
    }
    return b.join('');
}

function snippetCard(s) {
    const reject = s.state === 'REJECTED'
        ? '<div class="sn-reject"><b>Lý do từ chối:</b> ' + escapeHtml(s.rejectReason || '')
            + '<div class="sn-meta mt-1">Admin ' + escapeHtml(s.rejectedByName || '') + ' · ' + escapeHtml(formatDateTime(s.rejectedAt)) + '</div></div>'
        : '';
    const wait = s.stateNote && !me.admin && s.mine ? '<div class="sn-wait">' + escapeHtml(s.stateNote) + '</div>' : '';
    return '<div class="sn-item" data-sn="' + s.id + '">'
        + '<div class="sn-meta mb-1">' + stateBadge(s) + ' ' + requestPill(s) + ' <b>' + escapeHtml(s.fieldLabel) + '</b> · ' + escapeHtml(groupText(s)) + '</div>'
        + bodyHtml(s) + reject + wait
        + '<div class="sn-meta mt-1">' + metaLine(s) + '</div>'
        + '<div class="rf-actions">' + actionButtons(s) + '</div>'
        + '<div class="sn-hist d-none"></div>'
        + '</div>';
}

function emptyCard(text) {
    return '<div class="card border-0 shadow-sm rounded-4 p-3 text-center bg-white text-muted small">' + text + '</div>';
}

// Admin: yêu cầu đang chờ (mẫu mới, yêu cầu sửa, yêu cầu xóa); cũ nhất (đọng lâu nhất) lên đầu
function renderPending() {
    if (!me.admin) return;
    const list = items.filter(function (s) { return !!s.requestType; }).sort(function (a, b) {
        const ta = new Date(a.requestType === 'ADD' ? a.createdAt : a.changeAt).getTime();
        const tb = new Date(b.requestType === 'ADD' ? b.createdAt : b.changeAt).getTime();
        return ta - tb;
    });
    $('pendingCount').textContent = list.length;
    if (list.length === 0) {
        $('pendingList').innerHTML = emptyCard('Không có yêu cầu nào đang chờ duyệt.');
        return;
    }
    $('pendingList').innerHTML = list.map(function (s) {
        const who = s.requestType === 'ADD' ? s.createdByName : s.changeByName;
        const when = s.requestType === 'ADD' ? s.createdAt : s.changeAt;
        const canEditApprove = s.requestType !== 'DELETE';
        return '<div class="sn-item" data-sn="' + s.id + '">'
            + '<div class="sn-meta mb-1">' + stateBadge(s) + ' ' + requestPill(s) + ' <b>' + escapeHtml(s.fieldLabel) + '</b> · ' + escapeHtml(groupText(s)) + '</div>'
            + bodyHtml(s)
            + '<div class="sn-meta mt-1">Người gửi: <b>' + escapeHtml(who || '') + '</b> · ' + escapeHtml(formatDateTime(when)) + '</div>'
            + '<div class="rf-actions">'
            +   btnHtml('btn-success', 'fa-solid fa-check', 'Duyệt', 'approveSnippet(' + s.id + ')')
            +   (canEditApprove ? btnHtml('btn-outline-primary', 'fa-solid fa-pen', 'Sửa &amp; duyệt', 'editApprove(' + s.id + ')') : '')
            +   btnHtml('btn-outline-danger', 'fa-solid fa-xmark', 'Từ chối', 'rejectSnippet(' + s.id + ')')
            +   btnHtml('btn-outline-secondary', 'fa-solid fa-clock-rotate-left', 'Lịch sử', 'toggleHistory(' + s.id + ', this)')
            + '</div>'
            + '<div class="sn-hist d-none"></div>'
            + '</div>';
    }).join('');
}

// Nhân viên: mẫu / yêu cầu của chính mình chưa xong (chờ duyệt, chờ xóa, bị từ chối)
function renderMine() {
    if (me.admin) return;
    const list = items.filter(function (s) { return s.mine && s.state !== 'APPROVED'; });
    $('mineCount').textContent = list.length;
    $('mineList').innerHTML = list.length === 0
        ? emptyCard('Bạn không có mẫu hay yêu cầu nào đang chờ xử lý.')
        : list.map(snippetCard).join('');
}

function renderAll() {
    const field = $('fieldFilter').value;
    const group = $('groupFilter').value;
    const state = $('stateFilter').value;
    const list = items.filter(function (s) {
        if (field && s.field !== field) return false;
        if (state && s.state !== state) return false;
        if (group === '__COMMON__') return !s.taskGroup;
        if (group) return s.taskGroup === group;
        return true;
    });
    $('allCount').textContent = list.length;
    $('allList').innerHTML = list.length === 0 ? emptyCard('Chưa có mẫu nào.') : list.map(snippetCard).join('');
}

// ---------------------------------------------------------------- thao tác

// Chạy một thao tác rồi nạp lại danh sách. Lỗi do dữ liệu đã đổi (đã được xử lý / không còn) thì nạp lại để thấy kết quả mới.
async function run(call) {
    try {
        const r = await call();
        showToast(r.message, false);
        await reload();
        return true;
    } catch (e) {
        showToast(e.message, true);
        if (e.status === 409 || e.status === 404) {
            try { await reload(); } catch (_) { /* giữ nguyên thông báo lỗi ở trên */ }
        }
        return false;
    }
}

function jsonBody(method, obj) {
    return { method: method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(obj) };
}

function findItem(id) {
    return items.find(function (x) { return x.id === id; });
}

async function addSnippet() {
    const content = $('addContent').value.trim();
    if (!content) { showToast('Hãy nhập nội dung mẫu', true); return; }
    $('btnAdd').disabled = true;
    const ok = await run(function () {
        return apiFetch(CTX + '/api/report-snippets', jsonBody('POST', {
            field: $('addField').value, taskGroup: $('addGroup').value || null, content: content
        }));
    });
    $('btnAdd').disabled = false;
    if (ok) { $('addContent').value = ''; $('addCounter').textContent = '0'; }
}

function approveSnippet(id) {
    return run(function () {
        return apiFetch(CTX + '/api/report-snippets/' + id + '/approve', jsonBody('POST', {}));
    });
}

// Admin chỉnh nội dung đề xuất rồi mới duyệt
function editApprove(id) {
    const s = findItem(id);
    if (!s) return;
    const current = s.requestType === 'EDIT' ? s.pendingContent : s.content;
    const text = prompt('Chỉnh nội dung trước khi duyệt:', current);
    if (text === null) return;
    return run(function () {
        return apiFetch(CTX + '/api/report-snippets/' + id + '/approve', jsonBody('POST', { content: text }));
    });
}

// Từ chối phải có lý do để người gửi biết cần sửa gì
function rejectSnippet(id) {
    const reason = prompt('Lý do từ chối (bắt buộc, người gửi sẽ nhìn thấy):', '');
    if (reason === null) return;
    if (!reason.trim()) { showToast('Vui lòng nhập lý do từ chối', true); return; }
    return run(function () {
        return apiFetch(CTX + '/api/report-snippets/' + id + '/reject', jsonBody('POST', { reason: reason }));
    });
}

function editSnippet(id) {
    const s = findItem(id);
    if (!s) return;
    let title;
    if (s.state === 'REJECTED') title = 'Sửa nội dung rồi gửi lại cho admin duyệt:';
    else if (s.directEdit) title = 'Sửa nội dung mẫu (có hiệu lực ngay):';
    else title = 'Nội dung đề nghị sửa (chờ admin duyệt):';
    const text = prompt(title, s.content);
    if (text === null) return;
    return run(function () {
        return apiFetch(CTX + '/api/report-snippets/' + id, jsonBody('PUT', { content: text }));
    });
}

function deleteSnippet(id) {
    const s = findItem(id);
    if (!s) return;
    const msg = s.directDelete ? 'Xóa mẫu này? Mẫu bị xóa ngay lập tức.' : 'Gửi yêu cầu xóa mẫu này? Mẫu chỉ bị xóa sau khi admin duyệt.';
    if (!confirm(msg)) return;
    return run(function () {
        return apiFetch(CTX + '/api/report-snippets/' + id, { method: 'DELETE' });
    });
}

// ---------------------------------------------------------------- lịch sử

function historyHtml(list, showField) {
    if (!list.length) return '<div class="text-muted small">Chưa có lịch sử.</div>';
    return list.map(function (h) {
        const head = showField
            ? '<b>' + escapeHtml(h.fieldLabel || '') + '</b>' + (h.taskGroupLabel ? ' · ' + escapeHtml(h.taskGroupLabel) : '') + ' · '
            : '';
        let lines = '';
        if (h.contentBefore) lines += '<div class="sn-hist-line"><span class="lbl">Trước:</span> ' + escapeHtml(h.contentBefore) + '</div>';
        if (h.contentAfter) lines += '<div class="sn-hist-line"><span class="lbl">Sau:</span> ' + escapeHtml(h.contentAfter) + '</div>';
        if (h.note) lines += '<div class="sn-hist-line"><span class="lbl">Ghi chú / lý do:</span> ' + escapeHtml(h.note) + '</div>';
        return '<div class="sn-hist-row">'
            + '<div class="sn-hist-top">' + head
            +   '<i class="' + escapeHtml(h.actionIcon) + '"></i><b>' + escapeHtml(h.actionLabel) + '</b>'
            +   '<span class="st-badge ' + escapeHtml(h.resultStateBadge) + '">' + escapeHtml(h.resultStateLabel) + '</span>'
            +   '<span class="sn-hist-time">' + escapeHtml(h.actorName || '') + ' · ' + escapeHtml(formatDateTime(h.createdAt)) + '</span>'
            + '</div>' + lines + '</div>';
    }).join('');
}

// Mở / đóng lịch sử ngay dưới thẻ chứa nút vừa bấm
async function toggleHistory(id, btn) {
    const card = btn.closest('.sn-item');
    const box = card.querySelector('.sn-hist');
    if (!box.classList.contains('d-none')) { box.classList.add('d-none'); return; }
    box.classList.remove('d-none');
    box.innerHTML = '<div class="text-muted small">Đang tải...</div>';
    try {
        box.innerHTML = historyHtml(await apiFetch(CTX + '/api/report-snippets/' + id + '/history'), false);
    } catch (e) {
        box.innerHTML = '<div class="text-danger small">' + escapeHtml(e.message) + '</div>';
    }
}

async function loadLog() {
    try {
        $('logList').innerHTML = '<div class="sn-item">' + historyHtml(await apiFetch(CTX + '/api/report-snippets/history?limit=100'), true) + '</div>';
    } catch (e) {
        $('logList').innerHTML = '<div class="alert alert-danger small">' + escapeHtml(e.message) + '</div>';
    }
}

async function toggleLog() {
    logOpen = !logOpen;
    $('logList').classList.toggle('d-none', !logOpen);
    $('btnLog').textContent = logOpen ? 'Ẩn' : 'Xem';
    if (logOpen) await loadLog();
}

// ---------------------------------------------------------------- mở từ thông báo

// Bấm thông báo sẽ vào trang này kèm ?focus=<id mẫu>: cuộn tới mẫu và làm nổi bật
function focusFromUrl() {
    const id = new URLSearchParams(window.location.search).get('focus');
    if (!id) return;
    history.replaceState(null, '', window.location.pathname);
    const el = document.querySelector('[data-sn="' + String(id).replace(/[^0-9]/g, '') + '"]');
    if (!el) { showToast('Mẫu này không còn trong danh sách', true); return; }
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    el.classList.add('sn-focus');
    setTimeout(function () { el.classList.remove('sn-focus'); }, 3000);
}
