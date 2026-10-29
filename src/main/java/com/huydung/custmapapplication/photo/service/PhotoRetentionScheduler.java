package com.huydung.custmapapplication.photo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Định kỳ xóa ảnh đã quá hạn (mặc định 10 phút một lần). Không làm gì khi tự xóa đang bị tắt (xem Cá nhân > Ảnh hiện trường).
 * Cần @EnableScheduling (config/SchedulingConfig). Chỉnh chu kỳ bằng app.photo.sweep-ms nếu muốn.
 */
@Component
public class PhotoRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PhotoRetentionScheduler.class);
    private static final int MAX_ROUNDS = 50;   // mỗi lần quét xóa tối đa 50 lô để không giữ luồng quá lâu

    private final TicketPhotoService photoService;

    public PhotoRetentionScheduler(TicketPhotoService photoService) {
        this.photoService = photoService;
    }

    @Scheduled(fixedDelayString = "${app.photo.sweep-ms:600000}", initialDelayString = "${app.photo.sweep-initial-ms:60000}")
    public void sweep() {
        try {
            int total = 0;
            int rounds = 0;
            int deleted;
            do {
                deleted = photoService.purgeExpired();
                total += deleted;
                rounds++;
            } while (deleted > 0 && rounds < MAX_ROUNDS);
            if (total > 0) {
                log.info("Đã tự xóa {} ảnh quá hạn", total);
            }
        } catch (Exception e) {
            log.error("Lỗi khi tự xóa ảnh quá hạn", e);
        }
    }
}
