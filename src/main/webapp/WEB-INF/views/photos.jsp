<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Ảnh hiện trường - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=2" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/photos.css?v=1" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-app">

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Ảnh hiện trường"/>
    <jsp:param name="navIcon" value="fa-solid fa-images"/>
</jsp:include>

<div class="container-app pb-5">

    <!-- Lọc theo người chụp -->
    <div class="ph-owners" id="owners"></div>

    <!-- Giải thích cơ chế tự xóa (nội dung do photos.js đặt theo cấu hình hiện tại) -->
    <div class="ph-note" id="retentionNote"></div>

    <div class="ph-grid" id="grid"></div>
    <div class="ph-empty d-none" id="emptyState">
        <i class="fa-regular fa-image"></i>
        <div>Chưa có ảnh nào.</div>
    </div>
    <div class="text-center mt-3">
        <button type="button" class="btn btn-outline-secondary d-none" id="btnMore" onclick="loadMore()">Tải thêm</button>
    </div>
</div>

<!-- Xem ảnh đầy đủ -->
<div class="ph-lightbox" id="lightbox" role="dialog" aria-modal="true" aria-label="Xem ảnh" aria-hidden="true">
    <div class="ph-lb-top">
        <button type="button" class="ph-lb-close" onclick="closeLightbox()" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button>
    </div>
    <div class="ph-lb-stage"><img id="lbImg" alt="Ảnh hiện trường"></div>
    <div class="ph-lb-info">
        <div id="lbMeta"></div>
        <div class="ph-count" id="lbCount"></div>
        <!-- Nút Tải về / Xóa chỉ hiện với tài khoản có quyền Camera (photos.js) -->
        <div class="ph-lb-actions d-none" id="lbActions">
            <a class="btn btn-light btn-sm" id="lbDownload"><i class="fa-solid fa-download me-1"></i>Tải về</a>
            <button type="button" class="btn btn-danger btn-sm" id="lbDelete"><i class="fa-regular fa-trash-can me-1"></i>Xóa ảnh</button>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const CTX = '${pageContext.request.contextPath}';
</script>
<script src="${pageContext.request.contextPath}/js/app-common.js?v=1"></script>
<script src="${pageContext.request.contextPath}/js/photos.js?v=1"></script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp"/>
</body>
</html>
