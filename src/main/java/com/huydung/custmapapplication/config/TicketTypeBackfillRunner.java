package com.huydung.custmapapplication.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Chạy một lần mỗi khi khởi động: các ca tạo trước khi có phân loại (cột ticket_type còn trống) được gán PHAN_ANH.
 * Câu lệnh chỉ động tới dòng còn trống nên chạy lại nhiều lần vẫn an toàn.
 */
@Component
public class TicketTypeBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketTypeBackfillRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketTypeBackfillRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int n = jdbcTemplate.update("UPDATE customer_tickets SET ticket_type = 'PHAN_ANH' WHERE ticket_type IS NULL");
            if (n > 0) {
                log.info("Đã gán loại PHAN_ANH cho {} ca cũ chưa có loại", n);
            }
        } catch (Exception e) {
            log.warn("Không gán được loại cho ca cũ: {}", e.getMessage());
        }
    }
}
