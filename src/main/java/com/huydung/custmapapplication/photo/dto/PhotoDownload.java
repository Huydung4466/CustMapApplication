package com.huydung.custmapapplication.photo.dto;

import org.springframework.core.io.Resource;

/** File ảnh để tải về kèm tên file gợi ý. */
public record PhotoDownload(Resource resource, String fileName) {
}
