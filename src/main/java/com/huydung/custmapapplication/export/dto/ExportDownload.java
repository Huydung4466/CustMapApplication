package com.huydung.custmapapplication.export.dto;

import java.nio.file.Path;

/** File sẵn sàng để tải về: đường dẫn trên ổ đĩa, tên khi tải và dung lượng. */
public record ExportDownload(Path path, String fileName, long size) {
}
