<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>Nghe nhạc - Hệ thống Quản lý Phản ánh KH</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/app-pages.css?v=2" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/music.css?v=6" rel="stylesheet">
    <!-- Giao diện tông Viettel (đặt sau CSS của trang để ghi đè màu cũ) -->
    <link href="${pageContext.request.contextPath}/css/viettel-theme.css?v=2" rel="stylesheet">
</head>
<body class="vt-app">

<%-- Bộ biểu tượng của giao diện nhạc (lấy từ bản mẫu) --%>
<svg xmlns="http://www.w3.org/2000/svg" style="position:absolute;width:0;height:0;overflow:hidden" aria-hidden="true">
<symbol id="i-home" viewBox="0 0 24 24"><path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z"/></symbol>
<symbol id="i-edit" viewBox="0 0 24 24"><path d="M12 4H5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h13a2 2 0 0 0 2-2v-7"/><path d="M16 5l3 3-9 9-4 1 1-4 9-9zM18 3l3 3"/></symbol>
<symbol id="i-gallery" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8" cy="9" r="1"/><path d="m4 17 5-5 3 3 3-4 5 6"/></symbol>
<symbol id="i-chart" viewBox="0 0 24 24"><path d="M3 3v18h18M7 15l4-4 3 2 5-7"/></symbol>
<symbol id="i-bell" viewBox="0 0 24 24"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 8-3 9h18c0-1-3-2-3-9M10 21h4"/></symbol>
<symbol id="i-menu" viewBox="0 0 24 24"><path d="M4 6h16M4 12h16M4 18h16"/></symbol>
<symbol id="i-music" viewBox="0 0 24 24"><path d="M9 18V5l12-2v13M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0m12-2a3 3 0 1 1-6 0 3 3 0 0 1 6 0"/></symbol>
<symbol id="i-search" viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="m16.5 16.5 4.5 4.5"/></symbol>
<symbol id="i-filter" viewBox="0 0 24 24"><path d="M4 6h16M4 12h16M4 18h16"/><circle cx="9" cy="6" r="2" fill="#fff"/><circle cx="16" cy="12" r="2" fill="#fff"/><circle cx="11" cy="18" r="2" fill="#fff"/></symbol>
<symbol id="i-headphones" viewBox="0 0 24 24"><path d="M3 15v-3a9 9 0 0 1 18 0v3M3 14h4v7H5a2 2 0 0 1-2-2v-5zm14 0h4v5a2 2 0 0 1-2 2h-2v-7z"/></symbol>
<symbol id="i-play" viewBox="0 0 24 24"><path d="m8 5 11 7-11 7z" stroke-linejoin="round"/></symbol>
<symbol id="i-pause" viewBox="0 0 24 24"><path d="M8 5v14M16 5v14" stroke-width="4"/></symbol>
<symbol id="i-next" viewBox="0 0 24 24"><path d="m5 5 10 7-10 7zM19 5v14"/></symbol>
<symbol id="i-prev" viewBox="0 0 24 24"><path d="m19 5-10 7 10 7zM5 5v14"/></symbol>
<symbol id="i-shuffle" viewBox="0 0 24 24"><path d="m16 3 5 0v5M3 17h4c5 0 5-10 10-10h4M16 21h5v-5M3 7h4c2.1 0 3.4 1.7 4.5 3.4M13 16c1.2 1.7 2.5 1 4 1h4"/></symbol>
<symbol id="i-repeat" viewBox="0 0 24 24"><path d="m17 2 4 4-4 4M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4m14-1v2a3 3 0 0 1-3 3H3"/></symbol>
<symbol id="i-heart" viewBox="0 0 24 24"><path d="M20.8 4.6a5.4 5.4 0 0 0-7.6 0L12 5.8l-1.2-1.2a5.4 5.4 0 1 0-7.6 7.6L12 21l8.8-8.8a5.4 5.4 0 0 0 0-7.6z"/></symbol>
<symbol id="i-back" viewBox="0 0 24 24"><path d="m15 4-8 8 8 8M7 12h14"/></symbol>
<symbol id="i-down" viewBox="0 0 24 24"><path d="m5 9 7 7 7-7"/></symbol>
<symbol id="i-more" viewBox="0 0 24 24"><circle cx="12" cy="5" r="1" fill="currentColor"/><circle cx="12" cy="12" r="1" fill="currentColor"/><circle cx="12" cy="19" r="1" fill="currentColor"/></symbol>
<symbol id="i-list" viewBox="0 0 24 24"><path d="M9 6h12M9 12h12M9 18h12M3 6h1M3 12h1M3 18h1"/></symbol>
<symbol id="i-calendar" viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 2v6M17 2v6M3 11h18m-12 5 2 2 4-4"/></symbol>
<symbol id="i-map" viewBox="0 0 24 24"><path d="m3 6 6-2 6 2 6-2v14l-6 2-6-2-6 2zM9 4v14M15 6v14"/><circle cx="17" cy="8" r="2"/></symbol>
<symbol id="i-camera" viewBox="0 0 24 24"><path d="M5 6h4l2-2h4l2 2h2a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z"/><circle cx="12" cy="13" r="3"/></symbol>
<symbol id="i-user" viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></symbol>
<symbol id="i-lock" viewBox="0 0 24 24"><rect x="4" y="10" width="16" height="12" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></symbol>
<symbol id="i-volume" viewBox="0 0 24 24"><path d="m3 9 5 0 5-4v14l-5-4H3zM17 9a4 4 0 0 1 0 6m3-9a8 8 0 0 1 0 12"/></symbol>
<symbol id="i-check" viewBox="0 0 24 24"><path d="m5 12 4 4L19 6"/></symbol>
<symbol id="i-wave" viewBox="0 0 24 24"><path d="M2 10v4M6 6v12M10 9v6M14 3v18M18 7v10M22 10v4"/></symbol>
<symbol id="i-spark" viewBox="0 0 24 24"><path d="m12 2 2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5z"/></symbol>
<symbol id="i-share" viewBox="0 0 24 24"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.5 10.8 7-4.6m-7 8 7 4"/></symbol>
</svg>

<jsp:include page="/WEB-INF/views/fragments/top-navbar.jsp">
    <jsp:param name="navTitle" value="Nghe nhạc"/>
    <jsp:param name="navIcon" value="fa-solid fa-music"/>
</jsp:include>

<div class="container-app pb-5">
    <div class="mz" id="music">
        <% if (Boolean.TRUE.equals(request.getAttribute("canUploadMusic"))) { %>
        <div class="mz-admin" id="muAdmin">
            <button type="button" class="primary-btn" id="musicOpenImport">+ Thêm nhạc</button>
        </div>
        <% } %>

        <%-- js/music.js vẽ ba màn: Trang chủ nhạc, Chi tiết playlist, Đang phát --%>
        <div id="muContent" aria-live="polite"></div>

        <div class="lyrics-modal" id="muSheet"><div class="sheet"><button type="button" class="close-sheet" data-act="closesheet" aria-label="Đóng">Đóng ✕</button><div id="muSheetBody"></div></div></div>
    </div>
</div>

<% if (Boolean.TRUE.equals(request.getAttribute("canUploadMusic"))) { %>
<dialog id="musicImport" class="mu-dialog" aria-labelledby="importTitle">
    <h2 id="importTitle">Thêm vào thư viện chung</h2>
    <label>Chọn file nhạc <input id="musicFiles" type="file" accept=".mp3,.m4a,.mp4,.aac,.ogg,.oga,.opus,.wav,.flac,.webm,.weba,audio/*" multiple></label>
    <label>Chọn thư mục <input id="musicFolder" type="file" webkitdirectory multiple></label>
    <%-- Dán link file nhạc được chia sẻ từ phần mềm khác: dán xong trình duyệt tự tải về rồi đưa lên thư viện (mỗi dòng một link) --%>
    <label for="musicLinks">Hoặc dán link: file nhạc (.mp3, .m4a...), Spotify, YouTube, SoundCloud</label>
    <div class="mu-link-row">
        <textarea id="musicLinks" rows="2" inputmode="url" autocomplete="off" spellcheck="false"
                  placeholder="https://open.spotify.com/playlist/... (mỗi dòng một link)"></textarea>
        <button type="button" class="btn btn-danger" id="musicLinkGo">Tải</button>
    </div>
    <small>Nhận MP3, M4A, AAC, OGG, OPUS, WAV, FLAC, WEBM; mỗi file tối đa 45 MB. Link Spotify / YouTube / SoundCloud được thêm dạng nghe trực tuyến (phát bằng khung phát của dịch vụ đó). Link file nhạc bị trang nguồn chặn tải thì hãy tải file về máy rồi chọn file.</small>
    <p id="musicImportStatus" role="status"></p>
    <ul id="musicImportResults"></ul>
    <form method="dialog"><button class="btn btn-outline-secondary w-100">Đóng</button></form>
</dialog>
<% } %>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const CTX = '${pageContext.request.contextPath}';
</script>
<script src="${pageContext.request.contextPath}/js/app-common.js?v=1"></script>
<%-- music-player.js (nạp trong top-navbar.jsp) và music.js đều defer nên chạy theo đúng thứ tự này --%>
<% if (Boolean.TRUE.equals(request.getAttribute("canUploadMusic"))) { %>
<%-- Tải file nhạc từ link chia sẻ (chỉ người được nhập nhạc mới cần); phải nạp TRƯỚC music.js --%>
<script defer src="${pageContext.request.contextPath}/js/music-link.js?v=3"></script>
<% } %>
<script defer src="${pageContext.request.contextPath}/js/music.js?v=7"></script>
<jsp:include page="/WEB-INF/views/fragments/bottom-nav.jsp"/>
</body>
</html>
