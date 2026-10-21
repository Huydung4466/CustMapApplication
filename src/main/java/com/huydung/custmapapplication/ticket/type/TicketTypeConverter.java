package com.huydung.custmapapplication.ticket.type;

import jakarta.persistence.AttributeConverter;

/**
 * Lưu loại ca dưới dạng chuỗi thường (tên enum) trong cột varchar.
 * Cố ý KHÔNG dùng @Enumerated(STRING): Hibernate 6 sẽ tạo check constraint liệt kê các giá trị enum lúc tạo cột,
 * và ddl-auto=update không cập nhật constraint đó, nên khi thêm một loại mới vào enum thì lưu dữ liệu mới sẽ bị lỗi.
 */
public class TicketTypeConverter implements AttributeConverter<TicketType, String> {

    @Override
    public String convertToDatabaseColumn(TicketType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public TicketType convertToEntityAttribute(String dbData) {
        // Dữ liệu cũ chưa có loại (hoặc loại không còn trong enum) coi như ca phản ánh
        TicketType t = TicketType.fromCode(dbData);
        return t != null ? t : TicketType.PHAN_ANH;
    }
}
