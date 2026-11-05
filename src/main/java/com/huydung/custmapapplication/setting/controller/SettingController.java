package com.huydung.custmapapplication.setting.controller;

import com.huydung.custmapapplication.setting.dto.CameraSettingsDTO;
import com.huydung.custmapapplication.setting.service.CameraSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chỉ chứa các API HIỂN THỊ cấu hình (trả về cấu hình riêng của người đang đăng nhập, vì camera cần biết nên in mục nào lên ảnh).
 * API sửa cấu hình nằm ở package setting.api.
 */
@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final CameraSettingService cameraSettingService;

    public SettingController(CameraSettingService cameraSettingService) {
        this.cameraSettingService = cameraSettingService;
    }

    @GetMapping("/camera")
    public ResponseEntity<CameraSettingsDTO> getCameraSettings() {
        return ResponseEntity.ok(cameraSettingService.getCameraSettings());
    }
}
