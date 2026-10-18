package com.huydung.custmapapplication.kmz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KmzImportResultDTO {
    private String fileName;
    private int totalParsed;     // Tổng số điểm đọc được từ file KMZ
    private int totalSaved;      // Tổng số điểm đã lưu/cập nhật vào DB
    private String message;
}
