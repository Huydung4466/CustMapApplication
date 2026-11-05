<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>${historyTabLabel} - Lịch sử ca</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/history.css?v=3" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau các khối style của trang để ghi đè màu cũ). Đổi số v khi sửa file để trình duyệt tải lại -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="${historyTabLabel}"/>
    <jsp:param name="navIcon" value="${historyTabIcon}"/>
</jsp:include>

<div class="container-app hx-page">

    <header class="hx-intro">
        <div class="hx-eyebrow">Theo dõi nghiệp vụ</div>
        <h1 class="hx-title">${historyTabLabel}</h1>
        <p class="hx-subtitle" id="hxSubtitle"></p>
    </header>

    <!-- Chuyển giữa 2 trang Lịch sử (dựng từ enum HistoryTab qua /api/tickets/meta), kèm số ca -->
    <nav class="hx-tabs" id="historyTabs" aria-label="Chọn trang lịch sử"></nav>

    <section class="hx-filter" aria-label="Tìm kiếm và bộ lọc">
        <div class="hx-filter-head">
            <span class="hx-section-title"><i class="fa-solid fa-filter"></i>Bộ lọc danh sách</span>
            <button type="button" class="hx-link-btn" id="hxClearBtn"><i class="fa-solid fa-rotate-right"></i>Xóa lọc</button>
        </div>
        <div class="hx-filter-grid" id="hxFilterGrid">
            <label class="hx-search">
                <i class="fa-solid fa-magnifying-glass"></i>
                <input type="search" id="keywordInput" class="hx-control" autocomplete="off"
                       placeholder="Tìm tên KH, SĐT, mã phản ánh, địa chỉ..." aria-label="Tìm kiếm ca">
            </label>
            <div class="hx-staff is-hidden" id="staffCol">
                <select id="staffFilterSelect" class="hx-control" aria-label="Nhân viên quản lý"></select>
            </div>
            <div class="hx-dates">
                <label class="hx-field"><span class="hx-label">Từ ngày</span>
                    <input type="date" id="fromDateInput" class="hx-control"></label>
                <label class="hx-field"><span class="hx-label">Đến ngày</span>
                    <input type="date" id="toDateInput" class="hx-control"></label>
            </div>
        </div>
        <div class="hx-filter-bottom">
            <span id="hxResultText">Đang tải...</span>
        </div>
    </section>

    <div class="hx-listhead">
        <h2 id="hxListTitle">Danh sách ca</h2>
        <span class="hx-found" id="historyCountText"></span>
    </div>

    <div id="historyList" class="hx-cards" aria-live="polite">
        <div class="hx-skel"><span></span><span></span><span></span></div>
        <div class="hx-skel"><span></span><span></span><span></span></div>
    </div>

    <div id="paginationBar" class="hx-pager"></div>
</div>

<!-- Hộp thoại chi tiết ca: dùng chung, xem fragments/ticket-detail-modal.jsp (history.js gọi TicketDetail.open) -->
<jsp:include page="/WEB-INF/views/fragments/ticket-detail-modal.jsp"/>

<!-- Hộp thoại xác nhận (thay cho confirm() của trình duyệt) -->
<div class="hx-overlay" id="hxConfirmOverlay" aria-hidden="true">
    <section class="hx-modal" role="alertdialog" aria-modal="true" aria-labelledby="hxConfirmTitle">
        <div class="hx-modal-head">
            <div><h3 id="hxConfirmTitle">Xác nhận</h3><p id="hxConfirmSub"></p></div>
        </div>
        <div class="hx-modal-body"><p class="hx-confirm-text" id="hxConfirmText"></p></div>
        <div class="hx-modal-foot">
            <button type="button" class="hx-btn outline" id="hxConfirmNo">Hủy</button>
            <button type="button" class="hx-btn red" id="hxConfirmYes">Xác nhận</button>
        </div>
    </section>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const CTX = '${pageContext.request.contextPath}';
    const HISTORY_TAB = '${historyTab}';
</script>
<script src="${pageContext.request.contextPath}/js/history.js?v=3"></script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp"/>
</body>
</html>
