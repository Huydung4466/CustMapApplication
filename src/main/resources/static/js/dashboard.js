let currentUser = null;
let searchTimer = null;
let actionModalInstance = null;
let ticketCache = {};
let currentPage = 1;
const PAGE_SIZE = 10; // Số ca mỗi trang (10 - 20 là hợp lý)
let myPosition = null;
let currentGroup = 'PHAN_ANH'; // nhóm đang xem (tab)
let currentType = '';          // loại đang lọc trong nhóm ('' = tất cả loại của nhóm)
let statusMeta = { statuses: [], statusOptions: {}, archiveReasons: [], historyTabs: [], listFilters: [] }; // lấy từ /api/tickets/meta
let typeInfos = [];            // danh sách loại + số ca, lấy từ /api/tickets/types
let defaultStaff = '';         // nhân viên mặc định của ô chọn nhân viên (chính mình), để tính số bộ lọc đang bật và để "Xóa tất cả"
let savedFilters = {};         // bộ lọc đã nhớ từ lần trước (localStorage), nạp lại sau khi tải lại trang
let sheetSnapshot = null;      // giá trị các ô lọc lúc mở tấm bộ lọc, để đóng mà không "Áp dụng" thì trả lại như cũ

document.addEventListener('DOMContentLoaded', async () => {
    actionModalInstance = new bootstrap.Modal(document.getElementById('actionModal'));
    await loadStatusMeta();
    await initCurrentUserAndStaffFilter();
    const restoredGroup = restoreSimpleFilters();   // nạp lại bộ lọc đã nhớ (tải lại trang không làm mất bộ lọc)
    updateFilterBadge();
    await loadTypeTabs(!restoredGroup);
    await loadTickets();

    // Bấm chip trong tấm bộ lọc = chọn giá trị cho ô chọn tương ứng (chưa tải lại danh sách cho tới khi "Áp dụng")
    document.getElementById('filterSheet').addEventListener('click', e => {
        const chip = e.target.closest('.fs-chip');
        if (!chip) return;
        const sel = document.getElementById(chip.closest('[data-chips-for]').getAttribute('data-chips-for'));
        sel.value = chip.getAttribute('data-value');
        buildFilterChips();
        updateFilterBadge();
    });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeFilterSheet(); });
});

// Chỉ ADMIN mới thấy nút chọn nhiều ca để thao tác hàng loạt (phần import file đã chuyển sang trang Cá nhân)
function applyAdminPermissions() {
    if (currentUser && currentUser.admin) {
        document.getElementById('bulkToggleRow').classList.remove('d-none');
    }
}

async function initCurrentUserAndStaffFilter() {
    try {
        currentUser = await AppNav.getMe();   // thanh trên cùng (top-navbar) đã lấy sẵn và hiện tên
        applyAdminPermissions();

        const staffSelect = document.getElementById('staffFilterSelect');
        staffSelect.innerHTML = '';

        if (currentUser.admin) {
            const staffRes = await fetch(`${CTX}/api/tickets/staffs`);
            const staffs = await staffRes.json();
            const optAll = document.createElement('option');
            optAll.value = 'ALL';
            optAll.textContent = '-- Tất cả nhân viên --';
            staffSelect.appendChild(optAll);

            staffs.forEach(name => {
                const opt = document.createElement('option');
                opt.value = name;
                opt.textContent = `NV: ${name}`;
                if (name.trim().toUpperCase() === currentUser.fullName.trim().toUpperCase()) opt.selected = true;
                staffSelect.appendChild(opt);
            });
        } else {
            const opt = document.createElement('option');
            opt.value = currentUser.fullName;
            opt.textContent = `Địa bàn: ${currentUser.fullName}`;
            opt.selected = true;
            staffSelect.appendChild(opt);
            staffSelect.disabled = true;
        }
        defaultStaff = staffSelect.value;
        savedFilters = readSavedFilters();
        // Admin: nhân viên đã chọn lần trước (nếu còn trong danh sách) thay cho mặc định
        if (currentUser.admin) setSelectIfExists('staffFilterSelect', savedFilters.staff);
        // Load danh sách khu vực phù hợp với nhân viên đang chọn, rồi nạp khu vực / mã tủ đã nhớ
        await loadAreaDropdownForStaff(staffSelect.value);
        setSelectIfExists('areaFilterSelect', savedFilters.area);
        await loadCabinetDropdownForStaff(staffSelect.value, document.getElementById('areaFilterSelect').value);
        setSelectIfExists('cabinetPrefixInput', savedFilters.cabinet);
    } catch (e) { console.error(e); }
}

// Chỉ load các khu vực mà nhân viên đó quản lý (VD: Long chỉ có Buôn Hồ và Cư Mgar)
async function loadAreaDropdownForStaff(staffName) {
    try {
        const res = await fetch(`${CTX}/api/tickets/areas?staffName=${encodeURIComponent(staffName)}`);
        const areas = await res.json();
        const areaSelect = document.getElementById('areaFilterSelect');
        areaSelect.innerHTML = '<option value="">-- Tất cả Khu vực của NV --</option>';
        areas.forEach(area => {
            if (area) {
                const opt = document.createElement('option');
                opt.value = area;
                opt.textContent = `Khu vực: ${area}`;
                areaSelect.appendChild(opt);
            }
        });
    } catch (e) { console.error(e); }
}

async function loadCabinetDropdownForStaff(staffName, area) {
    try {
        const res = await fetch(`${CTX}/api/tickets/cabinet-prefixes?staffName=${encodeURIComponent(staffName || '')}&area=${encodeURIComponent(area || '')}`);
        const prefixes = await res.json();
        const sel = document.getElementById('cabinetPrefixInput');
        sel.innerHTML = '<option value="">-- Tất cả --</option>';
        prefixes.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p;
            opt.textContent = p;
            sel.appendChild(opt);
        });
    } catch (e) { console.error(e); }
}

async function onStaffChange() {
    const staff = document.getElementById('staffFilterSelect').value;
    await Promise.all([loadAreaDropdownForStaff(staff), loadCabinetDropdownForStaff(staff, '')]);
    updateFilterBadge();   // danh sách ca tải lại khi bấm "Áp dụng bộ lọc"
}

async function onAreaChange() {
    const staff = document.getElementById('staffFilterSelect').value;
    const area = document.getElementById('areaFilterSelect').value;
    await loadCabinetDropdownForStaff(staff, area);
    updateFilterBadge();
}

// ===================== TAB NHÓM + CHIP LOẠI =====================
// Mọi loại / nhóm đều lấy từ server (enum TicketType): thêm loại mới ở đó là tab và chip tự có thêm.
async function loadTypeTabs(pickDefault) {
    try {
        const staff = document.getElementById('staffFilterSelect').value || '';
        typeInfos = await apiFetch(`${CTX}/api/tickets/types?staffFilter=${encodeURIComponent(staff)}`);
    } catch (e) { console.error(e); return; }

    const groups = getGroups();
    if (groups.length && !groups.some(g => g.code === currentGroup)) currentGroup = groups[0].code;
    if (pickDefault) {
        // Mặc định vào tab Phản ánh; nếu tab đó trống mà tab khác có ca thì vào tab có ca
        const cur = groups.find(g => g.code === currentGroup);
        if (!cur || (cur.count === 0 && groups.some(g => g.count > 0))) {
            currentGroup = (groups.find(g => g.count > 0) || groups[0] || { code: currentGroup }).code;
        }
    }
    renderGroupTabs();
    renderTypeChips();
    applyGroupUi();
}

function getGroups() {
    const groups = [];
    typeInfos.forEach(t => {
        let g = groups.find(x => x.code === t.group);
        if (!g) { g = { code: t.group, label: t.groupLabel, icon: t.groupIcon, count: 0 }; groups.push(g); }
        g.count += t.openCount;
    });
    return groups;
}

function renderGroupTabs() {
    document.getElementById('groupTabs').innerHTML = getGroups().map(g => `
        <button type="button" class="group-tab ${g.code === currentGroup ? 'active' : ''}" onclick="selectGroup('${g.code}')">
            <i class="fa-solid ${escapeHtml(g.icon)}"></i>${escapeHtml(g.label)}<span class="tab-count">${g.count}</span>
        </button>`).join('');
}

function renderTypeChips() {
    const box = document.getElementById('typeChips');
    const types = typeInfos.filter(t => t.group === currentGroup);
    if (types.length <= 1) { box.innerHTML = ''; currentType = ''; return; }
    if (currentType && !types.some(t => t.code === currentType)) currentType = '';
    const total = types.reduce((sum, t) => sum + t.openCount, 0);
    box.innerHTML = `<button type="button" class="type-chip ${currentType === '' ? 'active' : ''}" onclick="selectType('')">Tất cả (${total})</button>`
        + types.map(t => `<button type="button" class="type-chip ${currentType === t.code ? 'active' : ''}" onclick="selectType('${t.code}')">${escapeHtml(t.label)} (${t.openCount})</button>`).join('');
}

// Bộ lọc mức ưu tiên chỉ có nghĩa với ca phản ánh nên ẩn ở nhóm hạ tầng
function applyGroupUi() {
    const isComplaint = currentGroup === 'PHAN_ANH';
    document.getElementById('priorityFilterCol').classList.toggle('d-none', !isComplaint);
    if (!isComplaint) document.getElementById('priorityFilterSelect').value = '';
    const g = getGroups().find(x => x.code === currentGroup);
    document.getElementById('ticketCountUnit').textContent = 'ca ' + (g ? g.label.toLowerCase() : '');
}

function selectGroup(code) {
    if (code === currentGroup) return;
    currentGroup = code;
    currentType = '';
    renderGroupTabs();
    renderTypeChips();
    applyGroupUi();
    loadTickets();
}

function selectType(code) {
    currentType = code;
    renderTypeChips();
    loadTickets();
}

// Thẻ khách (gồm thông số ca hạ tầng, xem thêm / thu gọn) dùng chung với trang Khách đã hẹn: xem js/ticket-card.js


// ===================== CHỌN NHANH + THAO TÁC HÀNG LOẠT (chỉ ADMIN) =====================
// "Chọn tất cả" luôn gắn với BỘ LỌC hiện tại: server tự lấy các ca khớp bộ lọc rồi trừ các ca bỏ tick.
// Đổi bộ lọc / từ khóa / tab thì lựa chọn bị xóa. Quyền thật sự do server kiểm tra (SecurityConfig + service).
let selectMode = false;          // đang ở chế độ "Chọn nhiều"
let selectAllMatching = false;   // true = chọn TOÀN BỘ ca theo bộ lọc hiện tại (mọi trang)
const selectedIds = new Set();   // ca được tick riêng (khi selectAllMatching = false)
const excludedIds = new Set();   // ca bỏ tick (khi selectAllMatching = true)
let totalMatching = 0;           // tổng số ca khớp bộ lọc ở lần tải gần nhất
let lastTickets = [];            // các ca đang hiển thị ở trang hiện tại
let lastFilterBody = null;       // bộ lọc của lần tải gần nhất

function isSelected(id) { return selectAllMatching ? !excludedIds.has(id) : selectedIds.has(id); }
function selectedCount() { return selectAllMatching ? Math.max(0, totalMatching - excludedIds.size) : selectedIds.size; }

function toggleSelectMode() {
    selectMode = !selectMode;
    if (!selectMode) clearSelection(true);
    document.body.classList.toggle('select-mode', selectMode);
    const btn = document.getElementById('btnBulkMode');
    btn.classList.toggle('btn-primary', selectMode);
    btn.classList.toggle('btn-outline-primary', !selectMode);
    btn.innerHTML = selectMode
        ? '<i class="fa-solid fa-xmark me-1"></i>Thoát chọn'
        : '<i class="fa-regular fa-square-check me-1"></i>Chọn nhiều';
    renderTicketCards(lastTickets);
    updateBulkBar();
}

function toggleSelect(id, checkbox) {
    const on = checkbox.checked;
    if (selectAllMatching) { if (on) excludedIds.delete(id); else excludedIds.add(id); }
    else { if (on) selectedIds.add(id); else selectedIds.delete(id); }
    const card = checkbox.closest('.ticket-card');
    if (card) card.classList.toggle('is-selected', on);
    updateBulkBar();
}

function selectPage() {
    selectAllMatching = false;
    excludedIds.clear();
    selectedIds.clear();
    lastTickets.forEach(t => selectedIds.add(Number(t.id)));
    renderTicketCards(lastTickets);
    updateBulkBar();
}

function selectAllByFilter() {
    if (totalMatching === 0) return;
    selectAllMatching = true;
    selectedIds.clear();
    excludedIds.clear();
    renderTicketCards(lastTickets);
    updateBulkBar();
}

// silent = true: chỉ xóa lựa chọn, không vẽ lại giao diện (dùng khi sắp tải lại danh sách)
function clearSelection(silent) {
    selectAllMatching = false;
    selectedIds.clear();
    excludedIds.clear();
    if (silent !== true) {
        renderTicketCards(lastTickets);
        updateBulkBar();
    }
}

function updateBulkBar() {
    const bar = document.getElementById('bulkBar');
    if (!bar) return;
    bar.classList.toggle('d-none', !selectMode);
    if (!selectMode) return;

    const n = selectedCount();
    document.getElementById('bulkSelectedCount').textContent = n;
    const allBtn = document.getElementById('btnSelectAllFilter');
    allBtn.textContent = `Chọn tất cả ${totalMatching} ca theo bộ lọc`;
    allBtn.disabled = totalMatching === 0 || (selectAllMatching && excludedIds.size === 0);
    document.getElementById('bulkScopeNote').textContent = selectAllMatching
        ? 'Toàn bộ ca theo bộ lọc hiện tại' + (excludedIds.size ? ', trừ ' + excludedIds.size + ' ca bỏ chọn' : '')
        : 'Chọn riêng lẻ';
    document.getElementById('btnBulkDone').disabled = n === 0;
    document.getElementById('btnBulkDelete').disabled = n === 0;
}

// action: 'DONE' (chuyển Đã xử lý) | 'DELETE' (xóa vĩnh viễn kèm ảnh)
async function bulkAction(action) {
    const count = selectedCount();
    if (count <= 0) { alert('Chưa chọn ca nào'); return; }
    if (selectAllMatching && !lastFilterBody) { alert('Chưa có bộ lọc, hãy tải lại danh sách'); return; }

    const scope = selectAllMatching
        ? ' (toàn bộ ca theo bộ lọc hiện tại' + (excludedIds.size ? ', trừ ' + excludedIds.size + ' ca bỏ chọn' : '') + ')'
        : '';
    let msg;
    if (action === 'DELETE') {
        msg = `XÓA VĨNH VIỄN ${count} ca${scope}?\n\nCa bị xóa cùng ảnh hiện trường sẽ mất hoàn toàn, KHÔNG khôi phục được. `
            + 'Nếu ca vẫn còn trong file import sau này thì sẽ được nạp lại như ca mới.';
    } else {
        msg = `Chuyển ${count} ca${scope} sang ĐÃ XỬ LÝ (vào Lịch sử)?`;
    }
    if (!confirm(msg)) return;

    // Xóa nhiều ca: bắt gõ chữ XOA để tránh bấm nhầm
    if (action === 'DELETE' && count > 20) {
        const typed = prompt(`Gõ XOA để xác nhận xóa vĩnh viễn ${count} ca:`);
        if ((typed || '').trim().toUpperCase() !== 'XOA') return;
    }

    const body = { action: action, expectedCount: count };
    if (selectAllMatching) {
        body.criteria = lastFilterBody;
        body.excludedIds = Array.from(excludedIds);
    } else {
        body.ids = Array.from(selectedIds);
    }

    const btns = [document.getElementById('btnBulkDone'), document.getElementById('btnBulkDelete')];
    btns.forEach(b => b.disabled = true);
    try {
        const result = await apiFetch(`${CTX}/api/tickets/bulk`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        clearSelection(true);
        flashNotice(result.message);
        if (window.BottomNav) BottomNav.refreshAppointments();
        await loadTickets(true);
    } catch (e) {
        alert(e.message);
        updateBulkBar();
    }
}

// keepPage = true: giữ nguyên trang hiện tại (sau khi thao tác trên thẻ).
// Mọi lần đổi bộ lọc / từ khóa / sắp xếp đều gọi không tham số -> tự về trang 1.
async function loadTickets(keepPage) {
    if (keepPage !== true) { currentPage = 1; clearSelection(true); } // đổi bộ lọc thì bỏ chọn
    const keyword = document.getElementById('keywordInput').value.trim();
    const staffFilter = document.getElementById('staffFilterSelect').value;
    const statusVal = document.getElementById('statusFilterSelect').value;
    const area = document.getElementById('areaFilterSelect').value;
    const cabinetPrefix = document.getElementById('cabinetPrefixInput').value.trim();
    const priority = document.getElementById('priorityFilterSelect').value;

    const sortSelect = document.getElementById('sortSelect');
    let sort = sortSelect.value;

    // Nhớ bộ lọc để tải lại trang không bị mất (chỉ mất khi bấm "Xóa lọc"). "Gần tôi nhất" cần xin quyền vị trí nên không nhớ.
    saveFilters({
        group: currentGroup, type: currentType, keyword, staff: staffFilter || '', area: area || '',
        cabinet: cabinetPrefix || '', status: statusVal, priority: priority || '', sort: sort === 'NEAREST' ? 'DEFAULT' : sort
    });
    updateFilterBadge();

    const params = new URLSearchParams({
        staffFilter: staffFilter || '',
        keyword,
        statusFilter: statusVal,
        area: area || '',
        cabinetPrefix: cabinetPrefix || '',
        priority: priority || '',
        ticketGroup: currentGroup,
        ticketType: currentType
    });
    // Bộ lọc này được gửi lên server khi "Chọn tất cả theo bộ lọc" (thao tác hàng loạt)
    lastFilterBody = {
        staffFilter: staffFilter || '', keyword, statusFilter: statusVal, area: area || '',
        cabinetPrefix: cabinetPrefix || '', priority: priority || '',
        ticketGroup: currentGroup, ticketType: currentType
    };

    // Sắp xếp "Gần tôi nhất": cần lấy vị trí hiện tại của thiết bị
    if (sort === 'NEAREST') {
        try {
            const pos = await getMyPosition();
            params.set('lat', pos.lat);
            params.set('lng', pos.lng);
        } catch (e) {
            alert(e.message);
            sort = 'DEFAULT';
            sortSelect.value = 'DEFAULT';
        }
    }
    params.set('sort', sort);
    params.set('page', currentPage);
    params.set('size', PAGE_SIZE);

    const container = document.getElementById('ticketListContainer');
    try {
        const data = await apiFetch(`${CTX}/api/tickets?${params.toString()}`);
        currentPage = data.page; // server tự đưa về trang hợp lệ nếu trang cũ không còn dữ liệu
        document.getElementById('ticketCountText').textContent = data.totalItem;
        lastTickets = data.resultList || [];
        // Đang "chọn tất cả theo bộ lọc" mà tổng số ca đổi (vd. vừa xử lý một ca) thì bỏ chọn để khỏi thao tác nhầm
        if (selectAllMatching && data.totalItem !== totalMatching) clearSelection(true);
        totalMatching = data.totalItem;
        renderTicketCards(data.resultList);
        updateBulkBar();
        renderPagination(data);
        loadTypeTabs(false); // cập nhật số ca trên tab sau mỗi lần tải / thao tác
    } catch (e) {
        container.innerHTML = `<div class="alert alert-danger p-2 small">${escapeHtml(e.message)}</div>`;
        document.getElementById('paginationBar').innerHTML = '';
    }
}

function debounceLoadTickets() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(function () { loadTickets(); }, 300);
}

// ===================== NHỚ BỘ LỌC (localStorage, riêng từng tài khoản) =====================
function filterStorageKey() {
    const who = currentUser ? (currentUser.id || currentUser.username || currentUser.fullName) : 'anon';
    return 'dash.filters.v1.' + who;
}
function readSavedFilters() {
    try { return JSON.parse(localStorage.getItem(filterStorageKey())) || {}; } catch (e) { return {}; }
}
function saveFilters(f) {
    try { localStorage.setItem(filterStorageKey(), JSON.stringify(f)); } catch (e) { /* trình duyệt chặn lưu: bỏ qua */ }
}
function clearSavedFilters() {
    try { localStorage.removeItem(filterStorageKey()); } catch (e) { /* bỏ qua */ }
}
// Đặt giá trị cho ô chọn nếu giá trị đó còn nằm trong danh sách (bộ lọc cũ có thể không còn hợp lệ)
function setSelectIfExists(id, value) {
    if (value === undefined || value === null) return false;
    const sel = document.getElementById(id);
    const ok = Array.from(sel.options).some(o => o.value === value);
    if (ok) sel.value = value;
    return ok;
}
// Nạp lại ô tìm kiếm, trạng thái, ưu tiên, sắp xếp, tab nhóm / loại; trả về true nếu có nhóm đã nhớ
function restoreSimpleFilters() {
    const f = savedFilters || {};
    if (typeof f.keyword === 'string') document.getElementById('keywordInput').value = f.keyword;
    setSelectIfExists('statusFilterSelect', f.status);
    setSelectIfExists('priorityFilterSelect', f.priority);
    setSelectIfExists('sortSelect', f.sort);
    if (f.group) {
        currentGroup = f.group;
        currentType = f.type || '';
        return true;
    }
    return false;
}

// ===================== THANH LỌC GỌN + TẤM BỘ LỌC =====================
const SORT_LABELS = { DEFAULT: 'Mặc định', APPOINTMENT: 'Giờ hẹn', NEAREST: 'Gần tôi' };

function selVal(id) { return document.getElementById(id).value; }

// Số bộ lọc đang bật khác mặc định (ô tìm kiếm và chip loại đã hiện sẵn nên không tính)
function activeFilterCount() {
    let n = 0;
    const staffSel = document.getElementById('staffFilterSelect');
    if (!staffSel.disabled && defaultStaff && staffSel.value !== defaultStaff) n++;
    if (selVal('areaFilterSelect')) n++;
    if (selVal('cabinetPrefixInput')) n++;
    if (selVal('statusFilterSelect') && selVal('statusFilterSelect') !== 'ALL') n++;
    if (selVal('priorityFilterSelect')) n++;
    if (selVal('sortSelect') && selVal('sortSelect') !== 'DEFAULT') n++;
    return n;
}

// Cập nhật số trên nút lọc, nút "Áp dụng (N)" và dòng tóm tắt bên dưới ô tìm kiếm
function updateFilterBadge() {
    const n = activeFilterCount();
    const badge = document.getElementById('filterBadge');
    badge.textContent = n;
    badge.classList.toggle('d-none', n === 0);
    document.getElementById('filterOpenBtn').classList.toggle('has-active', n > 0);
    document.getElementById('fsApplyCount').textContent = n > 0 ? ' (' + n + ')' : '';

    const staffVal = selVal('staffFilterSelect');
    let left = 'Địa bàn: ' + (staffVal === 'ALL' ? 'Tất cả nhân viên' : staffVal);
    if (selVal('areaFilterSelect')) left += ' · ' + selVal('areaFilterSelect');
    if (selVal('cabinetPrefixInput')) left += ' · Tủ ' + selVal('cabinetPrefixInput');
    const statusSel = document.getElementById('statusFilterSelect');
    const statusText = statusSel.value === 'ALL' || !statusSel.selectedOptions.length ? 'Tất cả' : statusSel.selectedOptions[0].textContent.trim();
    document.getElementById('fsumLeft').textContent = left;
    document.getElementById('fsumRight').textContent = 'Trạng thái: ' + statusText;
    const anyFilter = n > 0 || document.getElementById('keywordInput').value.trim() !== '' || currentType !== '';
    document.getElementById('fsumClear').classList.toggle('d-none', !anyFilter);
}

function chipLabel(selectId, opt) {
    if (selectId === 'sortSelect') return SORT_LABELS[opt.value] || opt.textContent.trim();
    if (opt.value === '' || opt.value === 'ALL') return 'Tất cả';
    return opt.textContent.trim();
}

// Chip của trạng thái / ưu tiên / sắp xếp dựng từ chính các ô chọn ẩn, nên các lựa chọn của server (enum) tự có mặt ở đây
function buildFilterChips() {
    document.querySelectorAll('#filterSheet [data-chips-for]').forEach(box => {
        const sel = document.getElementById(box.getAttribute('data-chips-for'));
        box.innerHTML = Array.from(sel.options).map(o =>
            `<button type="button" class="fs-chip ${o.value === sel.value ? 'active' : ''}" data-value="${escapeHtml(o.value)}">${escapeHtml(chipLabel(sel.id, o))}</button>`
        ).join('');
    });
}

function snapshotFilters() {
    return {
        staff: selVal('staffFilterSelect'), area: selVal('areaFilterSelect'), cabinet: selVal('cabinetPrefixInput'),
        status: selVal('statusFilterSelect'), priority: selVal('priorityFilterSelect'), sort: selVal('sortSelect')
    };
}

function openFilterSheet() {
    sheetSnapshot = snapshotFilters();
    buildFilterChips();
    updateFilterBadge();
    document.getElementById('filterBackdrop').classList.add('open');
    const sheet = document.getElementById('filterSheet');
    sheet.classList.add('open');
    sheet.setAttribute('aria-hidden', 'false');
    document.body.classList.add('fs-open');
}

function hideFilterSheet() {
    document.getElementById('filterBackdrop').classList.remove('open');
    const sheet = document.getElementById('filterSheet');
    sheet.classList.remove('open');
    sheet.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('fs-open');
}

// Đóng mà không "Áp dụng": trả các ô lọc về giá trị lúc mở để danh sách và bộ lọc không lệch nhau
async function closeFilterSheet() {
    if (!document.getElementById('filterSheet').classList.contains('open')) return;
    hideFilterSheet();
    const s = sheetSnapshot;
    sheetSnapshot = null;
    if (!s) return;
    const now = snapshotFilters();
    if (now.staff !== s.staff || now.area !== s.area || now.cabinet !== s.cabinet) {
        document.getElementById('staffFilterSelect').value = s.staff;
        await loadAreaDropdownForStaff(s.staff);
        setSelectIfExists('areaFilterSelect', s.area);
        await loadCabinetDropdownForStaff(s.staff, s.area);
        setSelectIfExists('cabinetPrefixInput', s.cabinet);
    }
    setSelectIfExists('statusFilterSelect', s.status);
    setSelectIfExists('priorityFilterSelect', s.priority);
    setSelectIfExists('sortSelect', s.sort);
    updateFilterBadge();
}

async function applyFilters() {
    sheetSnapshot = null;
    hideFilterSheet();
    await loadTickets();
}

// Xóa tất cả: về mặc định (nhân viên của chính mình, mọi khu vực / tủ / trạng thái, sắp xếp mặc định) và quên bộ lọc đã nhớ
async function resetFilters() {
    clearSavedFilters();
    sheetSnapshot = null;
    currentType = '';
    document.getElementById('keywordInput').value = '';
    const staffSel = document.getElementById('staffFilterSelect');
    if (!staffSel.disabled && defaultStaff) staffSel.value = defaultStaff;
    await loadAreaDropdownForStaff(staffSel.value);
    await loadCabinetDropdownForStaff(staffSel.value, '');
    document.getElementById('areaFilterSelect').value = '';
    document.getElementById('cabinetPrefixInput').value = '';
    document.getElementById('priorityFilterSelect').value = '';
    document.getElementById('statusFilterSelect').value = 'ALL';
    document.getElementById('sortSelect').value = 'DEFAULT';
    hideFilterSheet();
    renderTypeChips();
    buildFilterChips();
    loadTickets();
}

function renderTicketCards(tickets) {
    const container = document.getElementById('ticketListContainer');
    ticketCache = {};
    (tickets || []).forEach(t => { ticketCache[t.id] = t; });
    if (!tickets || tickets.length === 0) {
        container.innerHTML = `<div class="card border-0 shadow-sm rounded-4 p-4 text-center bg-white text-muted small">Không tìm thấy ca phản ánh phù hợp với bộ lọc</div>`;
        return;
    }

    // Phần thông tin khách vẽ bởi TicketCard (dùng chung với trang Khách đã hẹn); ở đây chỉ khai báo phần riêng của dashboard
    container.innerHTML = tickets.map(item => {
        const isPinned = item.isPinned === true;
        const appTime = item.appointmentTime
            ? `<div class="text-success small fw-bold mb-1"><i class="fa-regular fa-clock me-1"></i>${escapeHtml(item.appointmentTime)}</div>` : '';
        return TicketCard.build(item, {
            cardClass: selectMode && isSelected(Number(item.id)) ? 'is-selected' : '',
            topHtml: selectMode
                ? `<label class="select-row"><input type="checkbox" class="form-check-input m-0" ${isSelected(Number(item.id)) ? 'checked' : ''} onchange="toggleSelect(${Number(item.id)}, this)"><span>Chọn ca này</span></label>` : '',
            statusBadge: renderStatusBadge(item.appStatus),
            apptHtml: appTime,
            apptFirst: false,
            processCall: it => `openModal(${Number(it.id)}, '${safeStatus(it.appStatus)}')`,
            toolbarHtml: `<div class="quick-toolbar">
            <button type="button" class="btn-quick-item ${item.appStatus==='HEN_GIO'?'active-hengio':''}" onclick="openModal(${Number(item.id)}, 'HEN_GIO')">
                <i class="fa-regular fa-clock"></i>Hẹn giờ
            </button>
            <button type="button" class="btn-quick-item ${item.appStatus==='KLL'?'active-kll':''}" onclick="quickKLL(${Number(item.id)})">
                <i class="fa-solid fa-phone-slash"></i>KLL
            </button>
            <button type="button" class="btn-quick-item" onclick="openModal(${Number(item.id)}, 'HEN_KHAC')">
                <i class="fa-regular fa-calendar"></i>Khác
            </button>
            <button type="button" class="btn-quick-item ${isPinned?'pinned':''}" onclick="togglePin(${Number(item.id)}, ${!isPinned})">
                <i class="fa-solid fa-star text-warning"></i>${isPinned?'Đã ghim':'Ghim'}
            </button>
        </div>`
        });
    }).join('');
}

function renderPagination(data) {
    const bar = document.getElementById('paginationBar');
    if (!data || data.totalPage <= 1) {
        bar.innerHTML = '';
        return;
    }
    const prevDisabled = data.page <= 1 ? ' disabled' : '';
    const nextDisabled = data.page >= data.totalPage ? ' disabled' : '';
    bar.innerHTML =
        '<button type="button" class="btn btn-sm btn-outline-primary rounded-pill px-3"' + prevDisabled
        + ' onclick="goToPage(' + (data.page - 1) + ')"><i class="fa-solid fa-chevron-left me-1"></i>Trước</button>'
        + '<span class="small text-muted">Trang <b class="text-dark">' + data.page + '</b> / ' + data.totalPage + '</span>'
        + '<button type="button" class="btn btn-sm btn-outline-primary rounded-pill px-3"' + nextDisabled
        + ' onclick="goToPage(' + (data.page + 1) + ')">Sau<i class="fa-solid fa-chevron-right ms-1"></i></button>';
}

function goToPage(page) {
    currentPage = page;
    loadTickets(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Màu nền theo mức ưu tiên: Hot = đỏ, Nguy cơ HOT = vàng, Bình thường = xanh
function priorityStyle(level) {
    const v = String(level || '').trim().toUpperCase();
    if (v.includes('NGUY CƠ')) return 'background:#facc15;color:#422006;';
    if (v.includes('HOT'))     return 'background:#dc2626;color:#fff;';
    if (v.includes('BÌNH THƯỜNG')) return 'background:#16a34a;color:#fff;';
    return 'background:#f1f5f9;color:#475569;border:1px solid #e2e8f0;';
}

// Trạng thái xử lý (nhãn, biểu tượng, màu, các lựa chọn trong hộp "Cập nhật xử lý ca") do server khai báo
// bằng enum TicketStatus + StatusContext; giao diện chỉ việc đọc từ /api/tickets/meta
async function loadStatusMeta() {
    try {
        statusMeta = await apiFetch(CTX + '/api/tickets/meta');
        // Ô lọc "Trạng thái": các lựa chọn lấy từ enum TicketListFilter của server
        const filterSel = document.getElementById('statusFilterSelect');
        if (filterSel && (statusMeta.listFilters || []).length > 0) {
            const current = filterSel.value;
            filterSel.innerHTML = '';
            statusMeta.listFilters.forEach(function (f) {
                const opt = document.createElement('option');
                opt.value = f.code;
                opt.textContent = f.label;
                filterSel.appendChild(opt);
            });
            filterSel.value = current;
        }
        const sel = document.getElementById('modalStatusSelect');
        sel.innerHTML = '';
        (statusMeta.statusOptions.MAIN || []).forEach(function (o) {
            const opt = document.createElement('option');
            opt.value = o.code;
            opt.textContent = o.label;
            sel.appendChild(opt);
        });
    } catch (e) { console.error(e); }
}

function findStatus(code) {
    return statusMeta.statuses.find(function (s) { return s.code === code; });
}

// Chỉ chấp nhận các trạng thái đã biết khi đưa vào thuộc tính onclick
function safeStatus(s) {
    return findStatus(s) ? s : 'DANG_XU_LY';
}

function renderStatusBadge(status) {
    const info = findStatus(status) || findStatus('DANG_XU_LY');
    if (!info) return '';
    return '<span class="status-badge ' + escapeHtml(info.badgeClass) + '"><i class="' + escapeHtml(info.icon) + '"></i>'
        + escapeHtml(info.label) + '</span>';
}

// "2026-10-18T14:00:00" -> "2026-10-18T14:00" cho ô datetime-local
function toInputValue(v) {
    return v ? String(v).substring(0, 16) : '';
}

function openModal(id, status) {
    const item = ticketCache[id] || {};
    document.getElementById('modalTicketId').value = id;
    document.getElementById('modalStatusSelect').value = status || 'DANG_XU_LY';
    document.getElementById('modalApptStart').value = toInputValue(item.appointmentStart);
    document.getElementById('modalApptEnd').value = toInputValue(item.appointmentEnd);
    document.getElementById('modalCallNote').value = item.callNote || '';
    actionModalInstance.show();
}

async function submitModalAction() {
    const id = document.getElementById('modalTicketId').value;
    const startVal = document.getElementById('modalApptStart').value;
    const endVal = document.getElementById('modalApptEnd').value;
    if (endVal && !startVal) {
        return alert('Hãy chọn giờ bắt đầu hẹn trước.');
    }
    const payload = {
        appStatus: document.getElementById('modalStatusSelect').value,
        callNote: document.getElementById('modalCallNote').value
    };
    if (startVal) {
        payload.appointmentStart = startVal;
        payload.appointmentEnd = endVal || null;
    } else if (ticketCache[id] && ticketCache[id].appointmentStart) {
        // Trước đó có giờ hẹn mà giờ để trống -> xóa giờ hẹn
        payload.clearAppointment = true;
    }
    if (await sendTicketAction(id, payload)) {
        actionModalInstance.hide();
        // Ca đã hẹn không còn nằm ở danh sách này mà chuyển sang trang "Đã hẹn"
        if (payload.appStatus === 'HEN_GIO' || payload.appStatus === 'HEN_KHAC') {
            flashNotice('Đã chuyển khách sang mục "Đã hẹn"');
        }
    }
    loadTickets(true);
}

async function quickKLL(id) {
    if (!confirm('Không liên lạc được khách hàng này? Ca sẽ được chuyển vào Lịch sử.')) return;
    await sendTicketAction(id, { appStatus: 'KLL', callNote: 'Không liên lạc được' });
    loadTickets(true);
}

async function togglePin(id, isPinned) {
    await sendTicketAction(id, { isPinned });
    loadTickets(true);
}

async function apiFetch(url, options) {
    const res = await fetch(url, options);
    // Hết phiên đăng nhập: server chuyển hướng sang trang login
    if (res.redirected && res.url.includes('/login')) {
        window.location.href = CTX + '/login';
        throw new Error('Phiên đăng nhập đã hết hạn');
    }
    if (!res.ok) {
        let msg = 'Có lỗi xảy ra (mã ' + res.status + ')';
        let data = null;
        if (res.status === 413) {
            msg = 'File quá lớn, vượt giới hạn cho phép';
        } else {
            try {
                data = await res.json();
                msg = data.message || data.detail || msg;
            } catch (_) { /* không có JSON */ }
        }
        const error = new Error(msg);
        error.status = res.status;
        error.data = data;
        throw error;
    }
    return res.json();
}

async function sendTicketAction(id, payload) {
    try {
        await apiFetch(`${CTX}/api/tickets/${id}/action`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (window.BottomNav) BottomNav.refreshAppointments();   // số đếm trên tab "Đã hẹn" (thanh dưới)
        return true;
    } catch (e) {
        alert(e.message);
        return false;
    }
}

// Lấy vị trí hiện tại của thiết bị (chỉ hoạt động trên HTTPS hoặc localhost)
function getMyPosition() {
    return new Promise((resolve, reject) => {
        if (!window.isSecureContext) {
            return reject(new Error('Trình duyệt chỉ cho lấy vị trí khi mở trang bằng HTTPS hoặc localhost.'));
        }
        if (!navigator.geolocation) {
            return reject(new Error('Thiết bị không hỗ trợ định vị.'));
        }
        navigator.geolocation.getCurrentPosition(
            pos => {
                myPosition = { lat: pos.coords.latitude, lng: pos.coords.longitude };
                resolve(myPosition);
            },
            err => reject(new Error(err.code === 1
                ? 'Bạn chưa cho phép truy cập vị trí. Hãy bật quyền vị trí cho trang này rồi thử lại.'
                : 'Không lấy được vị trí hiện tại, vui lòng thử lại sau.')),
            { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
        );
    });
}

// Thông báo nhỏ nổi phía trên thanh điều hướng, tự tắt
function flashNotice(message) {
    const old = document.getElementById('flashNotice');
    if (old) old.remove();
    const el = document.createElement('div');
    el.id = 'flashNotice';
    el.textContent = message;
    el.style.cssText = 'position:fixed;left:50%;bottom:calc(96px + env(safe-area-inset-bottom,0px));transform:translateX(-50%);'
        + 'z-index:1060;max-width:90%;padding:9px 16px;border-radius:10px;font-size:.86rem;text-align:center;'
        + 'background:rgba(15,23,42,.93);color:#fff;box-shadow:0 6px 18px rgba(0,0,0,.25);';
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 3000);
}

function escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
