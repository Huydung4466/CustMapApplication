/*
 * profile.js - Trang Cá nhân.
 *  - Mọi vai trò: xem thông tin tài khoản, sửa số điện thoại, đổi mật khẩu, đăng xuất.
 *  - ROLE_ADMIN: thêm khu "Quản lý tài khoản" (xem tài khoản khác, tạo tài khoản nhân viên, đặt lại mật khẩu nhân viên).
 * Cần: CTX, app-common.js, Bootstrap.
 */
let me = null;
let isAdmin = false;
let searchTimer = null;
let createModal = null;
let resetModal = null;
let resetTarget = null;

const ROLE_LABELS = { ROLE_ADMIN: 'Quản trị viên', ROLE_STAFF: 'Nhân viên' };

document.addEventListener('DOMContentLoaded', async function () {
    createModal = new bootstrap.Modal(document.getElementById('createModal'));
    resetModal = new bootstrap.Modal(document.getElementById('resetModal'));
    await loadProfile();
    if (isAdmin) {
        document.getElementById('adminSection').classList.remove('d-none');
        document.getElementById('cameraSection').classList.remove('d-none');
        loadUsers();
        loadCameraSettings();
    }
});

function roleBadges(roles) {
    return (roles || []).map(function (r) {
        const cls = r === 'ROLE_ADMIN' ? 'role-admin' : 'role-staff';
        return '<span class="role-badge ' + cls + '">' + escapeHtml(ROLE_LABELS[r] || r) + '</span>';
    }).join(' ');
}

// ===================== THÔNG TIN CỦA TÔI =====================
async function loadProfile() {
    try {
        const auth = await apiFetch(CTX + '/api/auth/me');
        isAdmin = !!auth.admin;
        me = await apiFetch(CTX + '/api/profile');
        document.getElementById('currentUserFullName').textContent = me.fullName;
        document.getElementById('avatar').textContent = initialsOf(me.fullName);
        document.getElementById('pFullName').textContent = me.fullName;
        document.getElementById('pUsername').textContent = '@' + me.username;
        document.getElementById('pRoles').innerHTML = roleBadges(me.roles);
        document.getElementById('pStatus').innerHTML = me.status === 1
            ? '<span class="text-success fw-semibold"><i class="fa-solid fa-circle-check me-1"></i>Đang hoạt động</span>'
            : '<span class="text-danger fw-semibold"><i class="fa-solid fa-lock me-1"></i>Đã khóa</span>';
        document.getElementById('phoneInput').value = me.phone || '';
    } catch (e) {
        showToast(e.message, true);
    }
}

async function savePhone() {
    const btn = document.getElementById('btnSavePhone');
    btn.disabled = true;
    try {
        me = await apiFetch(CTX + '/api/profile', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ phone: document.getElementById('phoneInput').value })
        });
        document.getElementById('phoneInput').value = me.phone || '';
        showToast('Đã lưu số điện thoại');
    } catch (e) {
        showToast(e.message, true);
    } finally {
        btn.disabled = false;
    }
}

// ===================== QUẢN LÝ TÀI KHOẢN (ADMIN) =====================
// Chỉ đặt lại mật khẩu được cho tài khoản nhân viên (ROLE_STAFF) không phải quản trị viên
function canReset(u) {
    const roles = u.roles || [];
    return roles.indexOf('ROLE_STAFF') !== -1 && roles.indexOf('ROLE_ADMIN') === -1;
}

function debounceLoadUsers() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(loadUsers, 300);
}

async function loadUsers() {
    const box = document.getElementById('userList');
    const kw = document.getElementById('userKeyword').value.trim();
    try {
        const users = await apiFetch(CTX + '/api/users' + (kw ? '?keyword=' + encodeURIComponent(kw) : ''));
        document.getElementById('userCount').textContent = users.length;
        if (!users.length) {
            box.innerHTML = '<div class="text-center text-muted small py-3">Không có tài khoản phù hợp</div>';
            return;
        }
        box.innerHTML = users.map(userRowHtml).join('');
    } catch (e) {
        box.innerHTML = '<div class="alert alert-danger p-2 small mb-0">' + escapeHtml(e.message) + '</div>';
    }
}

function userRowHtml(u) {
    const id = Number(u.id);
    const phone = u.phone ? '<a href="tel:' + escapeHtml(u.phone) + '" class="text-decoration-none">' + escapeHtml(u.phone) + '</a>' : 'Chưa có SĐT';
    const locked = u.status !== 1 ? ' <span class="badge bg-secondary">Khóa</span>' : '';
    const reset = canReset(u)
        ? '<button type="button" class="btn btn-sm btn-outline-secondary rounded-pill px-2" onclick="openReset(' + id + ')" title="Đặt lại mật khẩu">'
          + '<i class="fa-solid fa-key me-1"></i>Đặt lại MK</button>'
        : '';
    return '<div class="user-row">'
        + '<div class="avatar-circle avatar-sm">' + escapeHtml(initialsOf(u.fullName)) + '</div>'
        + '<div class="info"><div class="name">' + escapeHtml(u.fullName) + locked + '</div>'
        +   '<div class="sub">@' + escapeHtml(u.username) + ' • ' + phone + '</div>'
        +   '<div class="mt-1">' + roleBadges(u.roles) + '</div></div>'
        + reset
        + '</div>';
}

// Mật khẩu ngẫu nhiên 10 ký tự, có cả chữ và số (bỏ các ký tự dễ nhầm như 0/O, 1/l/I)
function generatePassword() {
    const letters = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';
    const digits = '23456789';
    const all = letters + digits;
    const buf = new Uint32Array(10);
    crypto.getRandomValues(buf);
    const chars = Array.prototype.map.call(buf, function (n) { return all.charAt(n % all.length); });
    chars[0] = letters.charAt(buf[0] % letters.length);
    chars[1] = digits.charAt(buf[1] % digits.length);
    // Xáo trộn để chữ/số không luôn nằm ở đầu
    for (let i = chars.length - 1; i > 0; i--) {
        const j = crypto.getRandomValues(new Uint32Array(1))[0] % (i + 1);
        const t = chars[i]; chars[i] = chars[j]; chars[j] = t;
    }
    return chars.join('');
}

function passwordProblem(pw, username) {
    if (!pw || pw.length < 8) return 'Mật khẩu phải có ít nhất 8 ký tự';
    if (!/[A-Za-z]/.test(pw) || !/[0-9]/.test(pw)) return 'Mật khẩu phải gồm cả chữ và số';
    if (username && pw.toLowerCase() === username.trim().toLowerCase()) return 'Mật khẩu không được trùng với tên đăng nhập';
    return null;
}

// ----- Tạo tài khoản nhân viên -----
function openCreate() {
    ['cUsername', 'cFullName', 'cPhone'].forEach(function (id) { document.getElementById(id).value = ''; });
    document.getElementById('cPassword').value = generatePassword();
    document.getElementById('createError').textContent = '';
    createModal.show();
}

function fillCreatePassword() {
    document.getElementById('cPassword').value = generatePassword();
}

async function submitCreate() {
    const err = document.getElementById('createError');
    err.textContent = '';
    const username = document.getElementById('cUsername').value.trim();
    const fullName = document.getElementById('cFullName').value.trim();
    const phone = document.getElementById('cPhone').value.trim();
    const password = document.getElementById('cPassword').value;
    if (!username) { err.textContent = 'Nhập tên đăng nhập'; return; }
    if (!fullName) { err.textContent = 'Nhập họ tên (phải trùng tên ở cột "Giao nhân viên" trong file Excel)'; return; }
    const problem = passwordProblem(password, username);
    if (problem) { err.textContent = problem; return; }

    const btn = document.getElementById('btnCreate');
    btn.disabled = true;
    try {
        await apiFetch(CTX + '/api/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: username, fullName: fullName, phone: phone, password: password, roleCodes: ['ROLE_STAFF'] })
        });
        createModal.hide();
        showToast('Đã tạo tài khoản ' + username + '. Hãy gửi mật khẩu cho nhân viên.');
        loadUsers();
    } catch (e) {
        err.textContent = e.message;
    } finally {
        btn.disabled = false;
    }
}

// ----- Đặt lại mật khẩu nhân viên -----
async function openReset(id) {
    // Lấy lại thông tin từ danh sách đang hiển thị
    try {
        const users = await apiFetch(CTX + '/api/users');
        resetTarget = users.filter(function (u) { return Number(u.id) === id; })[0] || null;
    } catch (e) { showToast(e.message, true); return; }
    if (!resetTarget || !canReset(resetTarget)) { showToast('Chỉ đặt lại được mật khẩu của tài khoản nhân viên', true); return; }
    document.getElementById('resetWho').textContent = resetTarget.fullName + ' (@' + resetTarget.username + ')';
    document.getElementById('rPassword').value = generatePassword();
    document.getElementById('resetError').textContent = '';
    resetModal.show();
}

function fillResetPassword() {
    document.getElementById('rPassword').value = generatePassword();
}

async function submitReset() {
    const err = document.getElementById('resetError');
    err.textContent = '';
    if (!resetTarget) return;
    const password = document.getElementById('rPassword').value;
    const problem = passwordProblem(password, resetTarget.username);
    if (problem) { err.textContent = problem; return; }

    const btn = document.getElementById('btnReset');
    btn.disabled = true;
    try {
        await apiFetch(CTX + '/api/users/' + resetTarget.id + '/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ newPassword: password })
        });
        resetModal.hide();
        showToast('Đã đặt lại mật khẩu. Nhân viên sẽ phải đổi mật khẩu khi đăng nhập.');
    } catch (e) {
        err.textContent = e.message;
    } finally {
        btn.disabled = false;
    }
}

// ===================== CẤU HÌNH CAMERA (ADMIN) =====================
// Thứ tự khớp với thứ tự in trên ảnh (camera-capture.js > buildLines)
const CAMERA_OPTIONS = [
    { key: 'showTime',         label: 'Ngày giờ chụp',          sample: '19/10/26 09:43' },
    { key: 'showCoordinates',  label: 'Tọa độ GPS',             sample: "N 12\u00B0 40' 0\", E 108\u00B0 2' 18\"" },
    { key: 'showAddress',      label: 'Địa chỉ',                sample: '12 Lê Duẩn, Tân Lợi, Buôn Ma Thuột, Đắk Lắk' },
    { key: 'showCustomerName', label: 'Tên khách hàng',         sample: 'Nguyễn Văn A' },
    { key: 'showAccount',      label: 'Mã thuê bao (account)',  sample: 'd500_gftth_a1' },
    { key: 'showCabinetCode',  label: 'Mã tủ',                  sample: 'Tủ: DLK0412-A' },
    { key: 'showStaff',        label: 'Tên nhân viên chụp',     sample: 'NV: ' + (me ? me.fullName : 'Họ tên') }
];
let cameraCfg = null;

async function loadCameraSettings() {
    try {
        cameraCfg = await apiFetch(CTX + '/api/settings/camera');
        renderCameraSwitches();
    } catch (e) {
        document.getElementById('cameraSwitches').innerHTML =
            '<div class="alert alert-danger p-2 small mb-0">' + escapeHtml(e.message) + '</div>';
    }
}

function renderCameraSwitches() {
    document.getElementById('cameraSwitches').innerHTML = CAMERA_OPTIONS.map(function (o) {
        return '<div class="form-check form-switch d-flex align-items-center justify-content-between ps-0 py-2 border-bottom">'
            + '<label class="form-check-label flex-grow-1" for="sw-' + o.key + '">' + escapeHtml(o.label) + '</label>'
            + '<input class="form-check-input ms-2 mt-0" type="checkbox" role="switch" id="sw-' + o.key + '"'
            + (cameraCfg[o.key] ? ' checked' : '') + ' onchange="toggleCamera(\'' + o.key + '\', this)" style="width:2.6em;height:1.4em;">'
            + '</div>';
    }).join('');
    renderCameraPreview();
}

function renderCameraPreview() {
    const lines = CAMERA_OPTIONS.filter(function (o) { return cameraCfg[o.key]; });
    const box = document.getElementById('cameraPreview');
    box.innerHTML = lines.length
        ? lines.map(function (o) { return '<div>' + escapeHtml(o.sample) + '</div>'; }).join('')
        : '<div class="text-white-50">Đã tắt hết: ảnh sẽ không có chữ.</div>';
}

async function toggleCamera(key, checkbox) {
    const body = {};
    body[key] = checkbox.checked;
    checkbox.disabled = true;
    try {
        cameraCfg = await apiFetch(CTX + '/api/settings/camera', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        renderCameraPreview();
        showToast('Đã lưu cấu hình camera');
    } catch (e) {
        checkbox.checked = !checkbox.checked;   // lưu lỗi thì trả công tắc về như cũ
        showToast(e.message, true);
    } finally {
        checkbox.disabled = false;
    }
}
