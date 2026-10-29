<%--
  Thanh trên cùng, dùng chung cho các trang sau khi đăng nhập (thay cho đoạn nav chép riêng ở từng trang).
  Cách dùng (đặt ngay sau <body class="vt-app">):
      <jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
          <jsp:param name="navTitle" value="Khách đã hẹn"/>
          <jsp:param name="navIcon" value="fa-solid fa-calendar-check"/>
      </jsp:include>
  Tham số tùy chọn:
      navBack = false          ẩn nút quay lại danh sách ca (dùng cho trang chủ)
      navKmz = true            hiện số tủ đã có tọa độ KMZ
      navExtraHref / navExtraIcon / navExtraTitle    một nút tròn phụ ở bên phải (href phải bắt đầu bằng https://)
  Dưới thanh đỏ là hàng 6 tab biểu tượng (kiểu Facebook, không dính khi cuộn). Hiện dùng được tab Trang chủ (về dashboard), tab Ảnh (xem ảnh hiện trường) và tab Thông báo (chuông)
  (số chưa đọc + danh sách do js/notifications.js nạp); 3 tab còn lại bấm vào báo "đang phát triển" (js/nav-tabs.js).
  Thêm chức năng mới: đổi tab giữ chỗ thành <a class="nav-tab" href="..."> (bỏ data-soon).
  Tên người đăng nhập và số KMZ do js/app-nav.js tự nạp. Các trang gọi AppNav.getMe() để lấy thông tin tài khoản
  (không gọi lại /api/auth/me).
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" import="org.springframework.web.util.HtmlUtils" %>
<%
    String ctx = request.getContextPath();
    String navTitle = request.getParameter("navTitle");
    String navIcon = request.getParameter("navIcon");
    boolean navBack = !"false".equals(request.getParameter("navBack"));
    boolean navKmz = "true".equals(request.getParameter("navKmz"));
    String extraHref = request.getParameter("navExtraHref");
    String extraIcon = request.getParameter("navExtraIcon");
    String extraTitle = request.getParameter("navExtraTitle");
    // Chỉ nhận đường dẫn https để không ai nhét được javascript: vào nút phụ
    boolean hasExtra = extraHref != null && extraHref.startsWith("https://");
    // Tab Trang chủ sáng lên khi đang ở dashboard (JSP được forward nên địa chỉ gốc nằm ở thuộc tính forward)
    Object forwardUri = request.getAttribute("jakarta.servlet.forward.request_uri");
    String pagePath = forwardUri != null ? forwardUri.toString() : request.getRequestURI();
    boolean onDashboard = pagePath != null && pagePath.endsWith("/dashboard");
    boolean onPhotos = pagePath != null && pagePath.endsWith("/photos");
%>
<link href="<%= ctx %>/css/top-navbar.css?v=1" rel="stylesheet">
<link href="<%= ctx %>/css/nav-tabs.css?v=2" rel="stylesheet">
<link href="<%= ctx %>/css/notifications.css?v=2" rel="stylesheet">
<div class="top-navbar py-2" id="appTopNav" data-ctx="<%= HtmlUtils.htmlEscape(ctx) %>" data-kmz="<%= navKmz %>">
    <div class="nav-inner">
        <div class="nav-left">
            <% if (navBack) { %>
            <a href="<%= ctx %>/dashboard" class="btn btn-sm btn-outline-primary nav-icon-btn" title="Về danh sách ca" aria-label="Về danh sách ca">
                <i class="fa-solid fa-arrow-left"></i>
            </a>
            <% } %>
            <% if (navTitle != null && !navTitle.isBlank()) { %>
            <span class="nav-title"><% if (navIcon != null && !navIcon.isBlank()) { %><i class="<%= HtmlUtils.htmlEscape(navIcon) %> me-1"></i><% } %><%= HtmlUtils.htmlEscape(navTitle) %></span>
            <% } %>
            <% if (navKmz) { %>
            <span class="badge bg-light text-secondary border small nav-kmz" title="Số tủ đã có tọa độ KMZ">
                <i class="fa-solid fa-server text-primary me-1"></i><span class="nav-kmz-label">KMZ: </span><b id="kmzTotalCount">0</b>
            </span>
            <% } %>
        </div>
        <div class="nav-right">
            <% if (hasExtra) { %>
            <a href="<%= HtmlUtils.htmlEscape(extraHref) %>" target="_blank" rel="noopener"
               class="btn btn-sm btn-outline-primary nav-icon-btn"<% if (extraTitle != null) { %> title="<%= HtmlUtils.htmlEscape(extraTitle) %>" aria-label="<%= HtmlUtils.htmlEscape(extraTitle) %>"<% } %>>
                <i class="<%= extraIcon == null ? "fa-solid fa-arrow-up-right-from-square" : HtmlUtils.htmlEscape(extraIcon) %>"></i>
            </a>
            <% } %>
            <span class="badge bg-primary rounded-pill px-3 py-1 nav-user">
                <i class="fa-solid fa-user-shield me-1"></i><span id="currentUserFullName">Đang tải...</span>
            </span>
        </div>
    </div>
</div>
<%-- Hàng tab biểu tượng (kiểu Facebook). Tab giữ chỗ: data-soon = tên hiện trong câu "đang phát triển" --%>
<nav class="nav-tabs-row" id="appNavTabs" aria-label="Chức năng">
    <div class="nav-tabs-inner">
        <a class="nav-tab<%= onDashboard ? " active" : "" %>" href="<%= ctx %>/dashboard" title="Trang chủ" aria-label="Trang chủ"<%= onDashboard ? " aria-current=\"page\"" : "" %>><i class="fa-solid fa-house"></i></a>
        <button type="button" class="nav-tab" data-soon="Công việc" title="Công việc" aria-label="Công việc"><i class="fa-solid fa-list-check"></i></button>
        <a class="nav-tab<%= onPhotos ? " active" : "" %>" href="<%= ctx %>/photos" title="Ảnh" aria-label="Ảnh"<%= onPhotos ? " aria-current=\"page\"" : "" %>><i class="fa-solid fa-images"></i></a>
        <button type="button" class="nav-tab" data-soon="Thống kê" title="Thống kê" aria-label="Thống kê"><i class="fa-solid fa-chart-line"></i></button>
        <%-- Thông báo: số chưa đọc và danh sách do js/notifications.js nạp --%>
        <button type="button" class="nav-tab" id="navBellBtn" title="Thông báo" aria-label="Thông báo" aria-haspopup="true" aria-expanded="false">
            <i class="fa-regular fa-bell"></i><span class="nav-tab-badge d-none" id="navBellBadge">0</span>
        </button>
        <button type="button" class="nav-tab" data-soon="Menu" title="Menu" aria-label="Menu"><i class="fa-solid fa-bars"></i></button>
    </div>
</nav>
<script src="<%= ctx %>/js/csrf.js?v=1"></script>
<script src="<%= ctx %>/js/app-nav.js?v=1"></script>
<script src="<%= ctx %>/js/nav-tabs.js?v=1"></script>
<script src="<%= ctx %>/js/notifications.js?v=2"></script>
