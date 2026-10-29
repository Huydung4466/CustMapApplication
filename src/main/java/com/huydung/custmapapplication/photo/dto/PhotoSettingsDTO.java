package com.huydung.custmapapplication.photo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cấu hình ảnh dùng chung cho cả hệ thống (hiện chỉ có bật / tắt tự xóa). */
@Getter
@Setter
@NoArgsConstructor
public class PhotoSettingsDTO {
    private Boolean autoDeleteEnabled;
    private Integer retentionHours;   // chỉ đọc: số giờ ảnh được giữ trước khi tự xóa

    public PhotoSettingsDTO(Boolean autoDeleteEnabled, Integer retentionHours) {
        this.autoDeleteEnabled = autoDeleteEnabled;
        this.retentionHours = retentionHours;
    }
}
