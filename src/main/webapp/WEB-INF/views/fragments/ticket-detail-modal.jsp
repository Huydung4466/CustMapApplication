<%--
  Hộp thoại "Chi tiết ca" dùng chung (popup nổi ngay trong trang, không chuyển trang).
  Cách dùng: đặt <jsp:include page="/WEB-INF/views/fragments/ticket-detail-modal.jsp"/> trong <body> của trang,
  TRƯỚC thẻ <script> của chính trang đó. Sau đó gọi trong JS của trang:
      TicketDetail.open(item, { reasonLabel: 'Đã xử lý', showReport: true, onNotify: function (msg, isError) { ... } });
  item là một phần tử TicketResponseDTO (đã có sẵn trong danh sách nên mở ngay, không gọi thêm API).
  Tùy chọn (không bắt buộc, bỏ trống thì dòng đó ẩn): statusLabel = nhãn trạng thái xử lý, reasonLabel = nhãn lý do lưu trữ,
            showReport = hiện trạng thái nộp báo cáo, onNotify = hàm hiện thông báo nhỏ (không truyền thì popup tự hiện).
  Đang dùng ở: history.jsp, dashboard.jsp, appointments.jsp (hai trang sau qua nút "Chi tiết" của ticket-card.js).
  Cần Font Awesome trong trang (popup dùng vài biểu tượng).
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<link href="${pageContext.request.contextPath}/css/ticket-detail.css?v=2" rel="stylesheet">
<div class="td-overlay" id="tdOverlay" aria-hidden="true">
    <section class="td-modal" role="dialog" aria-modal="true" aria-labelledby="tdTitle">
        <div class="td-head">
            <div><h3 id="tdTitle">Chi tiết ca</h3><p id="tdSub"></p></div>
            <button type="button" class="td-x" id="tdClose" aria-label="Đóng">&times;</button>
        </div>
        <div class="td-body" id="tdBody"></div>
        <div class="td-foot"><button type="button" class="td-btn" id="tdOk">Đóng</button></div>
    </section>
</div>
<script src="${pageContext.request.contextPath}/js/ticket-detail.js?v=2"></script>
