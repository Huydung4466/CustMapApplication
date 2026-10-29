<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>${historyTabLabel} - Lịch sử ca</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/history.css?v=2" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau các khối style của trang để ghi đè màu cũ). Đổi số v khi sửa file để trình duyệt tải lại -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="${historyTabLabel}"/>
    <jsp:param name="navIcon" value="${historyTabIcon}"/>
</jsp:include>

<div class="container-app">

    <!-- Chuyển giữa 2 trang Lịch sử (dựng từ enum HistoryTab qua /api/tickets/meta) -->
    <div class="btn-group w-100 mb-3" id="historyTabs" role="group" aria-label="Chọn trang lịch sử"></div>

    <div class="filter-panel-bg">
        <div class="row g-2">
            <div class="col-12 col-md-6">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light"><i class="fa-solid fa-magnifying-glass text-muted"></i></span>
                    <input type="text" id="keywordInput" class="form-control" placeholder="Tìm tên KH, SĐT, mã phản ánh, địa chỉ..." oninput="debounceLoadHistory()">
                </div>
            </div>
            <div class="col-12 col-md-6 d-none" id="staffCol">
                <select id="staffFilterSelect" class="form-select form-select-sm fw-semibold text-primary" onchange="loadHistory()"></select>
            </div>
            <div class="col-6 col-md-4">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light text-muted vt-fs-75">Từ</span>
                    <input type="date" id="fromDateInput" class="form-control" onchange="loadHistory()">
                </div>
            </div>
            <div class="col-6 col-md-4">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light text-muted vt-fs-75">Đến</span>
                    <input type="date" id="toDateInput" class="form-control" onchange="loadHistory()">
                </div>
            </div>
            <div class="col-12 col-md-4 text-end">
                <button class="btn btn-sm btn-link text-decoration-none text-muted" onclick="resetHistoryFilters()">Xóa lọc</button>
            </div>
        </div>
        <div class="mt-2 pt-2 border-top text-muted vt-fs-78">
            Tổng: <b class="text-primary" id="historyCountText">0</b> ca
        </div>
    </div>

    <div id="historyList">
        <div class="text-center py-5 text-muted">
            <div class="spinner-border text-primary spinner-border-sm mb-2" role="status"></div>
            <div class="small">Đang tải dữ liệu...</div>
        </div>
    </div>

    <div id="paginationBar" class="d-flex justify-content-center align-items-center gap-3 my-3"></div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const CTX = '${pageContext.request.contextPath}';
    const HISTORY_TAB = '${historyTab}';
</script>
<script src="${pageContext.request.contextPath}/js/history.js?v=2"></script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp"/>
</body>
</html>
