package com.huydung.custmapapplication.setting.service.impl;

import com.huydung.custmapapplication.auth.service.AuthService;
import com.huydung.custmapapplication.setting.dto.CameraSettingsDTO;
import com.huydung.custmapapplication.setting.entity.AppSettingEntity;
import com.huydung.custmapapplication.setting.repository.AppSettingRepository;
import com.huydung.custmapapplication.setting.service.CameraSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class CameraSettingServiceImpl implements CameraSettingService {

    private static final String PREFIX = "camera.";

    private final AppSettingRepository settingRepository;
    private final AuthService authService;

    public CameraSettingServiceImpl(AppSettingRepository settingRepository, AuthService authService) {
        this.settingRepository = settingRepository;
        this.authService = authService;
    }

    // Khóa riêng của từng tài khoản: camera.user.<id>.<tên mục>
    private String userPrefix() {
        return PREFIX + "user." + authService.getCurrentLoggedUser().getId() + ".";
    }

    @Override
    @Transactional(readOnly = true)
    public CameraSettingsDTO getCameraSettings() {
        Map<String, String> values = new HashMap<>();
        for (AppSettingEntity e : settingRepository.findBySettingKeyStartingWith(PREFIX)) {
            values.put(e.getSettingKey(), e.getSettingValue());
        }
        String userPrefix = userPrefix();
        CameraSettingsDTO dto = new CameraSettingsDTO();
        // Thứ tự ưu tiên: cài đặt riêng của tài khoản > giá trị chung đã lưu trước đây > mặc định
        // Mặc định: giữ như trước đây (giờ, tọa độ, địa chỉ, mã thuê bao); các mục mới mặc định tắt
        dto.setShowTime(flag(values, userPrefix, "showTime", true));
        dto.setShowCoordinates(flag(values, userPrefix, "showCoordinates", true));
        dto.setShowAddress(flag(values, userPrefix, "showAddress", true));
        dto.setShowCustomerName(flag(values, userPrefix, "showCustomerName", false));
        dto.setShowAccount(flag(values, userPrefix, "showAccount", true));
        dto.setShowCabinetCode(flag(values, userPrefix, "showCabinetCode", false));
        dto.setShowStaff(flag(values, userPrefix, "showStaff", false));
        return dto;
    }

    @Override
    @Transactional
    public CameraSettingsDTO updateCameraSettings(CameraSettingsDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu cấu hình");
        }
        put("showTime", request.getShowTime());
        put("showCoordinates", request.getShowCoordinates());
        put("showAddress", request.getShowAddress());
        put("showCustomerName", request.getShowCustomerName());
        put("showAccount", request.getShowAccount());
        put("showCabinetCode", request.getShowCabinetCode());
        put("showStaff", request.getShowStaff());
        return getCameraSettings();
    }

    private boolean flag(Map<String, String> values, String userPrefix, String name, boolean defaultValue) {
        String v = values.get(userPrefix + name);
        if (v == null) v = values.get(PREFIX + name);
        return v == null ? defaultValue : Boolean.parseBoolean(v);
    }

    private void put(String name, Boolean value) {
        if (value == null) {
            return;
        }
        String key = userPrefix() + name;
        AppSettingEntity entity = settingRepository.findBySettingKey(key).orElseGet(() -> {
            AppSettingEntity e = new AppSettingEntity();
            e.setSettingKey(key);
            return e;
        });
        entity.setSettingValue(String.valueOf(value));
        settingRepository.save(entity);
    }
}
