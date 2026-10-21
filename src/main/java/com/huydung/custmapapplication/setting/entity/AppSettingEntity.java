package com.huydung.custmapapplication.setting.entity;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Cấu hình hệ thống dạng khóa - giá trị (VD: camera.showTime = true). */
@Entity
@Table(name = "app_settings")
public class AppSettingEntity extends BaseEntity {

    @Column(name = "setting_key", unique = true, nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 500)
    private String settingValue;

    public AppSettingEntity() {
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
}
