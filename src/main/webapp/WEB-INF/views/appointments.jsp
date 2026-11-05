<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Khách đã hẹn - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=2" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/ticket-card.css?v=3" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Khách đã hẹn"/>
    <jsp:param name="navIcon" value="fa-solid fa-calendar-check"/>
</jsp:include>

<div class="container-app">
    <div class="filter-panel-bg">
        <div class="row g-2">
            <div class="col-12 col-md-6">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light"><i class="fa-solid fa-magnifying-glass text-muted"></i></span>
                    <input type="text" id="keywordInput" class="form-control" placeholder="Tìm tên KH, SĐT, mã phản ánh, địa chỉ..." oninput="debounceLoadTickets()">
                </div>
            </div>
            <div class="col-6 col-md-3">
                <select id="statusFilterSelect" class="form-select form-select-sm" onchange="loadTickets()">
                    <option value="ALL">Tất cả lịch hẹn</option>
                    <option value="HEN_GIO">Hẹn giờ</option>
                    <option value="HEN_KHAC">Hẹn khác</option>
                </select>
            </div>
            <div class="col-6 col-md-3 d-none" id="staffCol">
                <select id="staffFilterSelect" class="form-select form-select-sm fw-semibold text-primary" onchange="loadTickets()"></select>
            </div>
        </div>
        <div class="d-flex justify-content-between align-items-center mt-2 pt-2 border-top text-muted vt-fs-78">
            <div>Đã hẹn: <b class="text-primary" id="ticketCountText">0</b> khách <span class="ms-1">(xếp theo giờ hẹn gần nhất)</span></div>
            <button class="btn btn-sm btn-link text-decoration-none p-0 small text-muted" onclick="resetFilters()">Xóa lọc</button>
        </div>
    </div>

    <div id="reminderBanner"></div>

    <div id="ticketListContainer">
        <div class="text-center py-5 text-muted">
            <div class="spinner-border text-primary spinner-border-sm mb-2" role="status"></div>
            <div class="small">Đang tải dữ liệu...</div>
        </div>
    </div>

    <div id="paginationBar" class="d-flex justify-content-center align-items-center gap-3 my-3"></div>
</div>

<!-- MODAL XỬ LÝ CA -->
<div class="modal fade" id="actionModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom-0 pb-0">
                <h6 class="modal-title fw-bold text-primary">Cập nhật lịch hẹn</h6>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body pt-2">
                <input type="hidden" id="modalTicketId">
                <div class="mb-2">
                    <label class="form-label small fw-bold">Trạng thái</label>
                    <!-- Các lựa chọn do server cấp từ enum TicketStatus / StatusContext.APPOINTMENT (xem loadStatusMeta) -->
                    <select id="modalStatusSelect" class="form-select form-select-sm"></select>
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold">Giờ hẹn từ</label>
                    <input type="datetime-local" id="modalApptStart" class="form-control form-control-sm">
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold">Đến (không bắt buộc)</label>
                    <input type="datetime-local" id="modalApptEnd" class="form-control form-control-sm">
                </div>
                <div class="mb-2">
                    <label class="form-label small fw-bold">Ghi chú</label>
                    <textarea id="modalCallNote" rows="2" class="form-control form-control-sm" placeholder="Ghi chú kết quả gọi..."></textarea>
                </div>
            </div>
            <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-sm btn-secondary rounded-pill px-3" data-bs-dismiss="modal">Đóng</button>
                <button type="button" class="btn btn-sm btn-primary rounded-pill px-4 fw-bold" onclick="submitModalAction()">Lưu</button>
            </div>
        </div>
    </div>
</div>

<%-- Hộp thoại Chi tiết ca dùng chung (nút "Chi tiết" trên thẻ gọi openTicketDetail trong ticket-card.js) --%>
<jsp:include page="/WEB-INF/views/fragments/ticket-detail-modal.jsp"/>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>const CTX = '${pageContext.request.contextPath}';</script>
<script src="${pageContext.request.contextPath}/js/app-common.js?v=1"></script>
<script src="${pageContext.request.contextPath}/js/ticket-card.js?v=3"></script>
<script src="${pageContext.request.contextPath}/js/appointments.js?v=4"></script>

<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp">
    <jsp:param name="active" value="appointments"/>
</jsp:include>
</body>
</html>
