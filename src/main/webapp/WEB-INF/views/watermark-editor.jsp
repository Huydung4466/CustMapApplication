<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chỉnh sửa watermark</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/watermark-editor.css" rel="stylesheet">
</head>
<body class="vt-app">
<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Chỉnh sửa watermark"/>
    <jsp:param name="navIcon" value="fa-solid fa-image"/>
</jsp:include>
<main class="container-app py-3">
    <h1 class="h4">Chỉnh sửa watermark</h1>
    <label for="wmFile" class="form-label">Ảnh có watermark (JPG hoặc PNG)</label>
    <input id="wmFile" class="form-control" type="file" accept="image/jpeg,image/png">
    <label for="wmOriginal" class="form-label mt-2">Ảnh gốc chưa đóng watermark (JPG hoặc PNG)</label>
    <input id="wmOriginal" class="form-control" type="file" accept="image/jpeg,image/png">
    <p id="wmStatus" class="mt-2" role="status" aria-live="polite">Chọn hai bản cùng khung hình và cùng kích thước để tự động nhận diện chữ.</p>
    <div class="wm-grid">
        <section class="wm-panel" aria-labelledby="wmPreviewTitle">
            <h2 id="wmPreviewTitle" class="h6">Xem trước</h2>
            <div class="d-flex gap-2 flex-wrap mb-2">
                <button id="wmBefore" class="btn btn-sm btn-outline-dark" type="button" aria-pressed="true" disabled>Ảnh tải lên</button>
                <button id="wmAfter" class="btn btn-sm btn-outline-dark" type="button" aria-pressed="false" disabled>Bản chỉnh sửa</button>
            </div>
            <label for="wmScanMode" class="form-label mt-2">Nội dung cần khoanh</label>
            <select id="wmScanMode" class="form-select">
                <option value="-1">Toàn bộ watermark</option>
                <option value="0">Riêng ngày giờ</option>
                <option value="1">Riêng tọa độ GPS</option>
                <option value="2">Riêng địa chỉ</option>
                <option value="3">Riêng mã hiển thị</option>
            </select>
            <button id="wmZoom" class="btn btn-sm btn-outline-dark mt-2" type="button" aria-controls="wmZoomPane" aria-expanded="false" disabled>Vùng watermark 100%</button>
            <div id="wmZoomPane" class="overflow-auto border mt-2" style="max-height: 20rem" hidden>
                <canvas id="wmZoomCanvas" class="d-block" style="max-width: none" aria-label="Vùng watermark ở kích thước ảnh thật"></canvas>
            </div>
            <canvas id="wmCanvas" aria-label="Ảnh xem trước watermark" hidden></canvas>
            <div class="d-flex gap-2 flex-wrap mt-2">
                <button id="wmSelect" class="btn btn-sm btn-outline-dark" type="button" disabled>Chọn lại vùng chữ</button>
                <button id="wmScan" class="btn btn-sm btn-outline-dark" type="button" disabled>Quét lại</button>
            </div>
        </section>
        <section class="wm-panel" aria-labelledby="wmFieldsTitle">
            <h2 id="wmFieldsTitle" class="h6">Nội dung nhận diện</h2>
            <fieldset id="wmFields" disabled>
                <label for="wmDate" class="form-label">Ngày giờ</label>
                <input id="wmDate" class="form-control mb-2" maxlength="50" aria-describedby="wmDateState">
                <p id="wmDateState" class="small text-muted"></p>
                <label for="wmGps" class="form-label">Tọa độ GPS</label>
                <input id="wmGps" class="form-control mb-2" maxlength="90" aria-describedby="wmGpsState">
                <p id="wmGpsState" class="small text-muted"></p>
                <label for="wmAddress" class="form-label">Địa chỉ</label>
                <input id="wmAddress" class="form-control mb-2" maxlength="200" aria-describedby="wmAddressState">
                <p id="wmAddressState" class="small text-muted"></p>
                <label for="wmCode" class="form-label">Mã hiển thị</label>
                <input id="wmCode" class="form-control mb-2" maxlength="120" aria-describedby="wmCodeState">
                <p id="wmCodeState" class="small text-muted"></p>
                <button id="wmReset" class="btn btn-sm btn-outline-dark" type="button">Khôi phục nội dung nhận diện</button>
            </fieldset>
            <p class="small text-muted mt-3">Nếu thiếu mục, hãy chọn tên mục và khoanh đúng dòng trên ảnh có watermark. Cần đủ bốn dòng để vẽ lại trên ảnh gốc.</p>
            <div class="row g-2 mt-2">
                <div class="col-12">
                    <label for="wmStyle" class="form-label">Kiểu chữ</label>
                    <select id="wmStyle" class="form-select" disabled>
                        <option value="serif">Chữ có chân như ảnh mẫu</option>
                        <option value="camera">Kiểu camera của ứng dụng</option>
                    </select>
                </div>
                <div class="col-12">
                    <label for="wmSize" class="form-label">Cỡ chữ</label>
                    <input id="wmSize" class="form-range" type="range" min="60" max="150" value="100" disabled>
                </div>
                <div class="col-6">
                    <label for="wmOffsetX" class="form-label">Dịch ngang</label>
                    <input id="wmOffsetX" class="form-range" type="range" min="-10" max="10" step="0.25" value="0" disabled>
                </div>
                <div class="col-6">
                    <label for="wmOffsetY" class="form-label">Dịch dọc</label>
                    <input id="wmOffsetY" class="form-range" type="range" min="-10" max="10" step="0.25" value="0" disabled>
                </div>
            </div>
            <button id="wmDownload" class="btn btn-danger" type="button" disabled>Tải bản chỉnh sửa</button>
        </section>
    </div>
</main>
<script defer src="https://cdn.jsdelivr.net/npm/tesseract.js@5.1.1/dist/tesseract.min.js"></script>
<script defer src="${pageContext.request.contextPath}/js/watermark-editor.js?v=5"></script>
</body>
</html>
