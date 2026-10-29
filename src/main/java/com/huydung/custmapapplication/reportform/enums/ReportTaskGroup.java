package com.huydung.custmapapplication.reportform.enums;

import com.huydung.custmapapplication.ticket.enums.TicketGroup;
import com.huydung.custmapapplication.ticket.enums.TicketType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "Nhóm nhiệm vụ chính" của form báo cáo. Nhãn PHẢI ghi đúng như lựa chọn trong form thì Google mới nhận.
 * Mẫu đã lưu được xếp vào các nhóm này (như "thư mục"); mẫu không thuộc nhóm nào dùng được cho mọi nhóm.
 */
public enum ReportTaskGroup {

    KT_TRIEN_KHAI_MOI("Kiểm tra chất lượng triển khai mới"),
    KT_XU_LY_SU_CO("Kiểm tra chất lượng xử lý sự cố"),
    KT_PHAN_ANH("Kiểm tra phản ánh khách hàng/PAKH"),
    HT_TRIEN_KHAI_MOI("Hỗ trợ triển khai mới"),
    HT_XU_LY_SU_CO("Hỗ trợ xử lý sự cố"),
    KT_HA_TANG("Kiểm tra hạ tầng"),
    KHAC("Công việc khác");

    private final String label;

    ReportTaskGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Chuỗi rỗng / null = không gắn nhóm (dùng chung); mã lạ thì báo lỗi. */
    public static ReportTaskGroup fromCodeOrNull(String code) {
        if (code == null || code.isBlank()) return null;
        for (ReportTaskGroup g : values()) {
            if (g.name().equalsIgnoreCase(code.trim())) return g;
        }
        throw new IllegalArgumentException("Nhóm nhiệm vụ không hợp lệ");
    }

    /** Nhóm gợi ý theo loại ca: ca phản ánh -> kiểm tra phản ánh, ca hạ tầng -> kiểm tra hạ tầng. */
    public static ReportTaskGroup suggestFor(TicketType type) {
        if (type == null) return KT_PHAN_ANH;
        return type.getGroup() == TicketGroup.HA_TANG ? KT_HA_TANG : KT_PHAN_ANH;
    }

    /** loại ca -> mã nhóm gợi ý (cho giao diện chọn sẵn). */
    public static Map<String, String> suggestionByTicketType() {
        Map<String, String> map = new LinkedHashMap<>();
        for (TicketType t : TicketType.values()) {
            map.put(t.name(), suggestFor(t).name());
        }
        return map;
    }
}
