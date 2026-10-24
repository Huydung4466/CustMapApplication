package com.huydung.custmapapplication.ticket.enums;

import java.util.List;

import static com.huydung.custmapapplication.ticket.enums.TicketStatus.*;

/**
 * Các lựa chọn trong ô "Trạng thái" của hộp "Cập nhật xử lý ca", theo từng màn hình:
 * thứ tự hiện và nhãn dài của từng lựa chọn (nhãn có thể khác nhau giữa các màn hình).
 */
public enum StatusContext {

    /** Hộp "Cập nhật xử lý ca" ở danh sách ca chính (dashboard). */
    MAIN(List.of(DANG_XU_LY, HEN_GIO, HEN_KHAC, KLL, DA_XU_LY)) {
        @Override
        public String optionLabel(TicketStatus status) {
            return switch (status) {
                case DANG_XU_LY -> "Đang xử lý";
                case HEN_GIO -> "Hẹn giờ";
                case HEN_KHAC -> "Hẹn khác";
                case KLL -> "Không liên lạc được (KLL) - chuyển vào Lịch sử";
                case DA_XU_LY -> "Đã xử lý hoàn tất - chuyển vào Lịch sử";
            };
        }
    },

    /** Hộp "Cập nhật lịch hẹn" ở trang Khách đã hẹn. */
    APPOINTMENT(List.of(HEN_GIO, HEN_KHAC, DANG_XU_LY, KLL, DA_XU_LY)) {
        @Override
        public String optionLabel(TicketStatus status) {
            return switch (status) {
                case HEN_GIO -> "Hẹn giờ";
                case HEN_KHAC -> "Hẹn khác";
                case DANG_XU_LY -> "Bỏ hẹn - đưa về danh sách ca chính";
                case KLL -> "Không liên lạc được (KLL) - chuyển vào Lịch sử";
                case DA_XU_LY -> "Đã xử lý hoàn tất - chuyển vào Lịch sử";
            };
        }
    };

    private final List<TicketStatus> statuses;

    StatusContext(List<TicketStatus> statuses) {
        this.statuses = statuses;
    }

    /** Các trạng thái hiện trong ô chọn, theo đúng thứ tự hiển thị. */
    public List<TicketStatus> getStatuses() {
        return statuses;
    }

    /** Nhãn của lựa chọn trong ô chọn. */
    public abstract String optionLabel(TicketStatus status);
}
