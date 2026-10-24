package com.huydung.custmapapplication.export.service;

import com.huydung.custmapapplication.export.dto.ExportDownload;
import com.huydung.custmapapplication.export.dto.ExportFileDTO;
import com.huydung.custmapapplication.export.dto.ExportRequest;

import java.util.List;

public interface ExportService {

    // Tạo file Excel báo cáo (3 sheet, tô màu theo trạng thái) và lưu tạm trên server; tự xóa sau thời gian quy định
    ExportFileDTO exportTickets(ExportRequest request);

    // Các file còn hạn của người dùng hiện tại, mới nhất trước
    List<ExportFileDTO> listMyExports();

    // Mở file để tải về (lỗi nếu không tồn tại hoặc đã hết hạn)
    ExportDownload openDownload(String id);

    // Xóa sớm một file
    void deleteExport(String id);
}
