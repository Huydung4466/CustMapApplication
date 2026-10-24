package com.huydung.custmapapplication.setting.service.impl;

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

    public CameraSettingServiceImpl(AppSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CameraSettingsDTO getCameraSettings() {
        Map<String, String> values = new HashMap<>();
        for (AppSettingEntity e : settingRepository.findBySettingKeyStartingWith(PREFIX)) {
            values.put(e.getSettingKey(), e.getSettingValue());
        }
        CameraSettingsDTO dto = new CameraSettingsDTO();
        // Mặc định: giữ như trước đây (giờ, tọa độ, địa chỉ, mã thuê bao); các mục mới mặc định tắt
        dto.setShowTime(flag(values, "showTime", true));
        dto.setShowCoordinates(flag(values, "showCoordinates", true));
        dto.setShowAddress(flag(values, "showAddress", true));
        dto.setShowCustomerName(flag(values, "showCustomerName", false));
        dto.setShowAccount(flag(values, "showAccount", true));
        dto.setShowCabinetCode(flag(values, "showCabinetCode", false));
        dto.setShowStaff(flag(values, "showStaff", false));
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

    private boolean flag(Map<String, String> values, String name, boolean defaultValue) {
        String v = values.get(PREFIX + name);
        return v == null ? defaultValue : Boolean.parseBoolean(v);
    }

    private void put(String name, Boolean value) {
        if (value == null) {
            return;
        }
        String key = PREFIX + name;
        AppSettingEntity entity = settingRepository.findBySettingKey(key).orElseGet(() -> {
            AppSettingEntity e = new AppSettingEntity();
            e.setSettingKey(key);
            return e;
        });
        entity.setSettingValue(String.valueOf(value));
        settingRepository.save(entity);
    }
}
