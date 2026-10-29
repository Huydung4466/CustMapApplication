<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Nộp báo cáo - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=2" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/report-form.css?v=2" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Nộp báo cáo"/>
    <jsp:param name="navIcon" value="fa-solid fa-file-signature"/>
</jsp:include>

<div class="container-app pb-5">

    <div id="rfLoading" class="text-center py-5 text-muted">
        <div class="spinner-border text-primary spinner-border-sm mb-2" role="status"></div>
        <div class="small">Đang tải dữ liệu...</div>
    </div>
    <div id="rfError" class="alert alert-danger small d-none"></div>

    <div id="rfMain" class="d-none">

        <div class="d-flex justify-content-between align-items-center mb-2 small">
            <a href="javascript:history.back()" class="text-decoration-none"><i class="fa-solid fa-arrow-left me-1"></i>Quay lại</a>
            <a href="${pageContext.request.contextPath}/report-snippets" id="adminLink" class="text-decoration-none d-none">
                <i class="fa-solid fa-clipboard-check me-1"></i>Duyệt / quản lý mẫu
            </a>
        </div>

        <!-- Trạng thái nộp báo cáo của ca (chưa nộp / đã mở form / đã nộp), do report-form.js vẽ -->
        <div id="rfStatus" class="rf-card rf-status"></div>

        <!-- Bước 1: thông tin của ca, đã điền sẵn -->
        <div class="rf-card">
            <div class="rf-title"><span class="rf-step">1</span>Thông tin ca <span class="rf-sub">(điền sẵn từ ca, sửa nhanh nếu cần)</span></div>
            <div class="row g-2">
                <div class="col-6">
                    <label class="rf-label" for="infoDate">Ngày thực hiện</label>
                    <input type="date" id="infoDate" class="form-control form-control-sm">
                </div>
                <div class="col-6">
                    <label class="rf-label" for="infoStaff">Nhân sự</label>
                    <input type="text" id="infoStaff" class="form-control form-control-sm">
                </div>
                <div class="col-12 col-md-6">
                    <label class="rf-label" for="infoArea">Địa bàn</label>
                    <select id="infoArea" class="form-select form-select-sm"></select>
                </div>
                <div class="col-12 col-md-6">
                    <label class="rf-label" for="infoService">Loại dịch vụ</label>
                    <select id="infoService" class="form-select form-select-sm"></select>
                </div>
                <div class="col-12 col-md-6">
                    <label class="rf-label" for="infoAccount">Account khách hàng</label>
                    <input type="text" id="infoAccount" class="form-control form-control-sm">
                </div>
                <div class="col-12 col-md-6">
                    <label class="rf-label" for="infoName">Tên khách hàng</label>
                    <input type="text" id="infoName" class="form-control form-control-sm">
                </div>
                <div class="col-12 col-md-6">
                    <label class="rf-label" for="infoPhone">Số điện thoại</label>
                    <input type="text" id="infoPhone" class="form-control form-control-sm">
                </div>
                <div class="col-12 col-md-6">
                    <label class="rf-label" for="infoCabinet">Mã tủ / hộp cáp</label>
                    <input type="text" id="infoCabinet" class="form-control form-control-sm">
                </div>
                <div class="col-12">
                    <label class="rf-label" for="infoAddress">Địa chỉ khách hàng</label>
                    <input type="text" id="infoAddress" class="form-control form-control-sm">
                </div>
                <div class="col-12">
                    <label class="rf-label" for="infoFt">FT / nhân viên Công ty Công trình liên quan</label>
                    <input type="text" id="infoFt" class="form-control form-control-sm">
                </div>
            </div>
        </div>

        <!-- Bước 2: nhóm nhiệm vụ chính (như "thư mục" chứa các mẫu) -->
        <div class="rf-card">
            <div class="rf-title"><span class="rf-step">2</span>Nhóm nhiệm vụ chính</div>
            <select id="rfGroup" class="form-select form-select-sm" onchange="onGroupChange()"></select>
            <div class="form-check mt-2 small">
                <input class="form-check-input" type="checkbox" id="rfSaveToGroup" checked>
                <label class="form-check-label" for="rfSaveToGroup">
                    Mẫu mới lưu <b>riêng cho nhóm này</b> (bỏ chọn = dùng chung cho mọi nhóm)
                </label>
            </div>
        </div>

        <!-- Bước 3: 4 mục viết, mỗi mục chọn mẫu đã lưu hoặc tự viết rồi lưu lại -->
        <div class="rf-title px-1 mb-2"><span class="rf-step">3</span>Nội dung báo cáo
            <span class="rf-sub">(để trống mục nào thì form để trống mục đó)</span></div>
        <div id="rfFields"></div>

        <div class="rf-submit-bar">
            <button type="button" class="btn btn-primary fw-bold w-100" onclick="openGoogleForm()">
                <i class="fa-solid fa-arrow-up-right-from-square me-1"></i>Mở form báo cáo (đã điền sẵn)
            </button>
            <div class="rf-sub text-center mt-1">Ảnh hiện trường và bấm Gửi vẫn thực hiện trong Google Form.</div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const CTX = '${pageContext.request.contextPath}';
    const TICKET_ID = '${ticketId}';
</script>
<script src="${pageContext.request.contextPath}/js/app-common.js?v=1"></script>
<script src="${pageContext.request.contextPath}/js/report-form.js?v=3"></script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp"/>
</body>
</html>
