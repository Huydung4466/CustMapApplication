<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Bản đồ - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=1" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=1" rel="stylesheet">
</head>
<body class="vt-app">

<div class="top-navbar py-2 mb-3">
    <div class="container-app nav-inner">
        <div class="nav-left">
            <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-sm btn-outline-primary nav-icon-btn" title="Về danh sách ca" aria-label="Về danh sách ca">
                <i class="fa-solid fa-arrow-left"></i>
            </a>
            <span class="nav-title"><i class="fa-solid fa-map-location-dot me-1"></i>Bản đồ tủ (KMZ)</span>
        </div>
        <% if (!"".equals(request.getAttribute("myMapsId"))) { %>
        <a href="https://www.google.com/maps/d/viewer?mid=${myMapsId}" target="_blank" rel="noopener"
           class="btn btn-sm btn-outline-primary nav-icon-btn" title="Mở trong Google My Maps" aria-label="Mở trong Google My Maps">
            <i class="fa-solid fa-up-right-from-square"></i>
        </a>
        <% } %>
    </div>
</div>

<div class="container-app">
<% if (!"".equals(request.getAttribute("myMapsId"))) { %>
    <iframe class="map-frame" title="Google My Maps - vị trí các tủ"
            src="https://www.google.com/maps/d/embed?mid=${myMapsId}"
            loading="lazy" allow="geolocation" referrerpolicy="no-referrer-when-downgrade"></iframe>
<% } else { %>
    <div class="card border-0 shadow-sm rounded-4 p-4 bg-white">
        <h6 class="fw-bold text-primary mb-2"><i class="fa-solid fa-circle-info me-1"></i>Chưa cấu hình bản đồ</h6>
        <p class="small text-muted mb-2">Trang này hiển thị bản đồ Google My Maps chứa toàn bộ điểm tủ trong file KMZ. Cách thiết lập:</p>
        <ol class="small text-muted ps-3 mb-2">
            <li>Mở <b>mymaps.google.com</b> &rarr; <b>Tạo bản đồ mới</b> &rarr; <b>Nhập</b> file KMZ.</li>
            <li>Chọn <b>Chia sẻ</b> &rarr; bật <b>Bất kỳ ai có đường liên kết đều có thể xem</b>.</li>
            <li>Sao chép giá trị sau <code>mid=</code> trên đường link của bản đồ.</li>
            <li>Điền vào <code>app.map.my-maps-id=</code> trong <code>application.properties</code> (hoặc dán cả đường link) rồi khởi động lại ứng dụng.</li>
        </ol>
        <p class="small text-muted mb-0">Khi KMZ có thêm điểm mới, hãy cập nhật lại lớp dữ liệu trong My Maps (nhập lại file KMZ).</p>
    </div>
<% } %>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp">
    <jsp:param name="active" value="map"/>
</jsp:include>
</body>
</html>
