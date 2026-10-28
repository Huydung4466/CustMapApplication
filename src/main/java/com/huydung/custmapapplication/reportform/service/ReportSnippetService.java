package com.huydung.custmapapplication.reportform.service;

import com.huydung.custmapapplication.reportform.dto.*;

import java.util.List;

public interface ReportSnippetService {

    ReportFormMetaDTO getMeta();

    // Mẫu người đang đăng nhập được DÙNG cho một mục: mẫu đã duyệt + mẫu chính mình tạo đang chờ duyệt
    List<ReportSnippetDTO> listUsable(String field);

    // Trang quản lý: admin thấy toàn bộ; nhân viên thấy mẫu đã duyệt + mọi mẫu / yêu cầu của chính mình
    List<ReportSnippetDTO> listForManage();

    // Admin: toàn bộ mẫu, kể cả đang chờ duyệt / bị từ chối
    List<ReportSnippetDTO> listAllForAdmin();

    // Admin: số yêu cầu đang chờ duyệt (hiện ở nút trên trang cá nhân)
    long countAwaitingApproval();

    // Thêm mẫu mới: admin có hiệu lực ngay, nhân viên chờ admin duyệt
    SnippetActionResult create(ReportSnippetRequest request);

    // Sửa nội dung: admin có hiệu lực ngay; nhân viên gửi yêu cầu chờ duyệt (mẫu mới chưa duyệt / bị từ chối: người tạo tự sửa)
    SnippetActionResult requestEdit(Long id, String content);

    // Xóa: admin xóa ngay, nhân viên gửi yêu cầu chờ duyệt (mẫu mới chưa duyệt / bị từ chối: người tạo tự xóa)
    SnippetActionResult requestDelete(Long id);

    // Chỉ admin: duyệt mẫu mới / yêu cầu sửa / yêu cầu xóa; editedContent (không bắt buộc) = nội dung admin chỉnh lại trước khi duyệt
    SnippetActionResult approve(Long id, String editedContent);

    // Chỉ admin: từ chối kèm lý do (mẫu mới chuyển sang "bị từ chối", yêu cầu sửa / xóa bị hủy)
    SnippetActionResult reject(Long id, String reason);

    // Lịch sử của một mẫu: admin xem mọi mẫu, nhân viên chỉ xem mẫu do mình tạo
    List<SnippetHistoryDTO> getHistory(Long snippetId);

    // Chỉ admin: nhật ký thao tác gần đây trên mọi mẫu
    List<SnippetHistoryDTO> listRecentHistory(int limit);
}
