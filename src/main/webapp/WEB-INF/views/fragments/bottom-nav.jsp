<%--
  Thanh điều hướng dưới cùng, dùng chung cho các trang sau khi đăng nhập.
  Cách dùng (đặt ngay trước </body> của trang):
      <jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp">
          <jsp:param name="active" value="appointments"/>   <!-- appointments | map | report | profile | (bỏ trống) -->
      </jsp:include>
  Nút 1: Khách đã hẹn (kèm số khách đã hẹn, do js/bottom-nav.js nạp) | Nút 2: Bản đồ | Nút "+" ở giữa: chụp ảnh nhanh (không gắn ca) | Nút 3: Báo cáo | Nút 4: Cá nhân (có chấm đỏ khi còn thông báo chưa đọc, do js/notifications.js bật tắt)
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<link href="${pageContext.request.contextPath}/css/bottom-nav.css?v=1" rel="stylesheet">
<nav class="vt-bnav" aria-label="Điều hướng chính" id="appBottomNav" data-ctx="${pageContext.request.contextPath}">
    <div class="vt-bnav-bar">
        <div class="vt-bnav-side left">
            <a class="vt-bnav-item ${param.active == 'appointments' ? 'active' : ''}" href="${pageContext.request.contextPath}/appointments">
                <i class="fa-solid fa-calendar-check"></i><b class="vt-bnav-badge d-none" id="bnApptBadge" title="Số khách đã hẹn"></b><span>Đã hẹn</span>
            </a>
            <a class="vt-bnav-item ${param.active == 'map' ? 'active' : ''}" href="${pageContext.request.contextPath}/map">
                <i class="fa-solid fa-map-location-dot"></i><span>Bản đồ</span>
            </a>
        </div>
        <div class="vt-bnav-mid">
            <svg class="vt-bnav-curve" viewBox="0 0 112 84" aria-hidden="true" focusable="false">
                <path class="fill" d="M0,20.5 H12 Q24,20.5 30,7.4 A36,36 0 1 0 82,7.4 Q88,20.5 100,20.5 H112 V84 H0 Z"/>
                <path class="stroke" d="M0,20.5 H12 Q24,20.5 30,7.4 A36,36 0 1 0 82,7.4 Q88,20.5 100,20.5 H112"/>
            </svg>
            <button type="button" class="vt-bnav-center" aria-label="Chụp ảnh nhanh" title="Chụp ảnh nhanh"
                    onclick="if (window.CameraCapture) { CameraCapture.openFree(); }">
                <i class="fa-solid fa-camera"></i>
            </button>
        </div>
        <div class="vt-bnav-side right">
            <a class="vt-bnav-item ${param.active == 'report' ? 'active' : ''}" href="${pageContext.request.contextPath}/report">
                <i class="fa-solid fa-chart-column"></i><span>Báo cáo</span>
            </a>
            <a class="vt-bnav-item ${param.active == 'profile' ? 'active' : ''}" href="${pageContext.request.contextPath}/profile">
                <i class="fa-solid fa-user"></i><b class="vt-bnav-dot d-none" id="bnProfileDot" title="Có thông báo chưa đọc"></b><span>Cá nhân</span>
            </a>
        </div>
    </div>
</nav>
<script>document.body.classList.add('has-bottom-nav');</script>
<script src="${pageContext.request.contextPath}/js/camera-capture.js?v=3"></script>
<script src="${pageContext.request.contextPath}/js/bottom-nav.js?v=1"></script>
