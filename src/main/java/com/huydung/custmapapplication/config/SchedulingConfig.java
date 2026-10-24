package com.huydung.custmapapplication.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật tác vụ chạy định kỳ (hiện dùng để tự xóa file Excel xuất ra sau khi hết hạn). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
