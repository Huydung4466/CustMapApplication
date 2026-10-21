package com.huydung.custmapapplication.ticket.type;

/**
 * Một thuộc tính riêng của từng loại ca (VD: "Công suất thu").
 * Cùng một khai báo này vừa dùng để ĐỌC cột Excel (theo tên tiêu đề, không theo vị trí cột)
 * vừa dùng để HIỂN THỊ trên thẻ, nên thêm thuộc tính chỉ cần thêm một dòng trong {@link TicketType}.
 *
 * @param key    khóa lưu trong cột jsonb (không đổi sau khi đã có dữ liệu)
 * @param label  nhãn hiển thị trên thẻ
 * @param header tên cột trong file Excel
 * @param unit   đơn vị hiển thị sau giá trị (có thể null)
 */
public record TicketAttribute(String key, String label, String header, String unit) {

    public static TicketAttribute of(String key, String label, String header) {
        return new TicketAttribute(key, label, header, null);
    }

    public static TicketAttribute of(String key, String label, String header, String unit) {
        return new TicketAttribute(key, label, header, unit);
    }
}
