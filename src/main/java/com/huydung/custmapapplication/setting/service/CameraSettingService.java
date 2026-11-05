package com.huydung.custmapapplication.setting.service;

import com.huydung.custmapapplication.setting.dto.CameraSettingsDTO;

public interface CameraSettingService {

    // Cấu hình của NGƯỜI ĐANG ĐĂNG NHẬP (mục chưa từng chỉnh dùng giá trị chung cũ, rồi tới mặc định)
    CameraSettingsDTO getCameraSettings();

    // Mọi vai trò bật/tắt các mục in lên ảnh của CHÍNH MÌNH; trường null giữ nguyên. Trả về cấu hình sau khi lưu
    CameraSettingsDTO updateCameraSettings(CameraSettingsDTO request);
}
