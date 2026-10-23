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
%>
<link href="<%= ctx %>/css/top-navbar.css?v=1" rel="stylesheet">
<div class="top-navbar py-2 mb-3" id="appTopNav" data-ctx="<%= HtmlUtils.htmlEscape(ctx) %>" data-kmz="<%= navKmz %>">
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
<script src="<%= ctx %>/js/app-nav.js?v=1"></script>
