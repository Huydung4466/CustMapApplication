package com.huydung.custmapapplication.setting.service;

import com.huydung.custmapapplication.setting.dto.CameraSettingsDTO;

public interface CameraSettingService {

    // Cấu hình hiện tại (mục chưa từng cấu hình dùng giá trị mặc định)
    CameraSettingsDTO getCameraSettings();

    // ADMIN bật/tắt các mục in lên ảnh; trường null giữ nguyên. Trả về cấu hình sau khi lưu
    CameraSettingsDTO updateCameraSettings(CameraSettingsDTO request);
}
