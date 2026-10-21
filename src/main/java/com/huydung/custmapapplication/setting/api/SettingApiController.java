package com.huydung.custmapapplication.setting.api;

import com.huydung.custmapapplication.setting.dto.CameraSettingsDTO;
import com.huydung.custmapapplication.setting.service.CameraSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API THAY ĐỔI cấu hình hệ thống (chỉ ADMIN - chặn trong SecurityConfig: PUT /api/settings/**). */
@RestController
@RequestMapping("/api/settings")
public class SettingApiController {

    private final CameraSettingService cameraSettingService;

    public SettingApiController(CameraSettingService cameraSettingService) {
        this.cameraSettingService = cameraSettingService;
    }

    // Bật/tắt các mục in lên ảnh khi chụp
    @PutMapping("/camera")
    public ResponseEntity<CameraSettingsDTO> updateCameraSettings(@RequestBody CameraSettingsDTO request) {
        return ResponseEntity.ok(cameraSettingService.updateCameraSettings(request));
    }
}
