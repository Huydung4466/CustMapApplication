package com.huydung.custmapapplication.ticket.enums;

/** Thao tác hàng loạt trên ca (chỉ ADMIN). Thêm thao tác mới: thêm giá trị ở đây và một nhánh trong TicketServiceImpl.bulkAction. */
public enum BulkAction {
    /** Chuyển các ca sang Lịch sử với trạng thái Đã xử lý (vẫn xem lại / khôi phục được). */
    DONE,
    /** Xóa vĩnh viễn ca khỏi hệ thống, kèm ảnh hiện trường. Không khôi phục được. */
    DELETE
}
