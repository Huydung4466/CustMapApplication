package com.huydung.custmapapplication.ticket.enums;

import com.huydung.custmapapplication.constant.InfraExcelConstant;
import com.huydung.custmapapplication.constant.LanHomeWifiKemExcelConstant;
import com.huydung.custmapapplication.constant.LapThieuHomeWifiExcelConstant;
import com.huydung.custmapapplication.constant.PortKemExcelConstant;
import com.huydung.custmapapplication.ticket.type.TicketAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.huydung.custmapapplication.ticket.type.TicketAttribute.of;

/**
 * Các loại ca. Đây là NƠI DUY NHẤT khai báo một loại dữ liệu:
 * thêm loại mới = thêm một giá trị ở đây, tab / bộ lọc / thẻ / đọc Excel tự theo.
 *
 * LƯU Ý: không đổi tên giá trị đã có (tên được lưu trong database).
 *
 * Tham số: nhãn hiển thị, nhóm (tab), màu badge (Bootstrap), nhãn trong cột "Loại dữ liệu" của file Excel,
 * các cột ghép thành mã ca (khóa), cột tên người phụ trách, danh sách thuộc tính riêng.
 */
public enum TicketType {

    // Ca phản ánh của khách hàng (file phản ánh có cột "Mã phản ánh", đọc bằng ComplaintFileFormat)
    PHAN_ANH("Phản ánh", TicketGroup.PHAN_ANH, "danger",
            null, List.of(), null, List.of()),

    // Tên cột Excel của từng loại hạ tầng nằm trong package constant (một lớp cho mỗi loại)
    PORT_KEM("Port kém GPON", TicketGroup.HA_TANG, "warning",
            PortKemExcelConstant.SOURCE_LABEL,
            List.of(InfraExcelConstant.HEADER_ACCOUNT,
                    PortKemExcelConstant.HEADER_DEVICE_CODE,
                    PortKemExcelConstant.HEADER_PORT),
            null,
            List.of(
                    of("ma_tram", "Mã trạm", InfraExcelConstant.HEADER_STATION),
                    of("cong_suat", "Công suất thu", PortKemExcelConstant.HEADER_RX_POWER, "dBm"),
                    of("thoi_gian_ton", "Thời gian tồn", PortKemExcelConstant.HEADER_HOLD_TIME, "giờ"),
                    of("thiet_bi", "Thiết bị", PortKemExcelConstant.HEADER_DEVICE_CODE),
                    of("port", "Port", PortKemExcelConstant.HEADER_PORT),
                    of("loai_thiet_bi", "Loại thiết bị", PortKemExcelConstant.HEADER_DEVICE_TYPE),
                    of("ngoai_vi", "Thiết bị ngoại vi", PortKemExcelConstant.HEADER_PERIPHERAL),
                    of("bat_dau", "Bắt đầu từ", PortKemExcelConstant.HEADER_START_TIME),
                    of("hang_kh", "Hạng KH", PortKemExcelConstant.HEADER_CUSTOMER_RANK),
                    of("loai_kh", "Loại KH", PortKemExcelConstant.HEADER_CUSTOMER_TYPE),
                    of("chan_cuoc", "Chặn cắt cước", PortKemExcelConstant.HEADER_BILLING_BLOCK_STATUS),
                    of("nguon", "Nguồn dữ liệu", PortKemExcelConstant.HEADER_DATA_SOURCE))),

    LAN_HOMEWIFI_KEM("LAN / HomeWifi kém", TicketGroup.HA_TANG, "info",
            LanHomeWifiKemExcelConstant.SOURCE_LABEL,
            List.of(InfraExcelConstant.HEADER_ACCOUNT,
                    LanHomeWifiKemExcelConstant.HEADER_SERIAL_HOMEWIFI),
            LanHomeWifiKemExcelConstant.HEADER_STAFF_NAME,
            List.of(
                    of("ma_tram", "Mã trạm", InfraExcelConstant.HEADER_STATION),
                    of("loi", "Lỗi", LanHomeWifiKemExcelConstant.HEADER_ERROR_INFO),
                    of("nguyen_nhan", "Nguyên nhân", LanHomeWifiKemExcelConstant.HEADER_CAUSE),
                    of("trang_thai", "Trạng thái xử lý", LanHomeWifiKemExcelConstant.HEADER_HANDLING_STATUS),
                    of("so_ngay", "Số ngày liên tiếp không đạt",
                            LanHomeWifiKemExcelConstant.HEADER_CONSECUTIVE_FAIL_DAYS, "ngày"),
                    of("muc_thu", "Mức thu không đạt", LanHomeWifiKemExcelConstant.HEADER_RECEIVE_LEVEL_FAIL),
                    of("ket_noi_lan", "Kết nối LAN không đạt", LanHomeWifiKemExcelConstant.HEADER_LAN_CONNECTION_FAIL),
                    of("online", "Trạng thái online", LanHomeWifiKemExcelConstant.HEADER_ONLINE_STATUS),
                    of("hang_sx", "Hãng sản xuất", LanHomeWifiKemExcelConstant.HEADER_MANUFACTURER),
                    of("serial_ont", "Serial ONT", LanHomeWifiKemExcelConstant.HEADER_SERIAL_ONT),
                    of("serial_hw", "Serial HomeWifi", LanHomeWifiKemExcelConstant.HEADER_SERIAL_HOMEWIFI),
                    of("sl_bccs", "Số HomeWifi (BCCS)", LanHomeWifiKemExcelConstant.HEADER_HOMEWIFI_COUNT_BCCS),
                    of("thoi_diem_chot", "Thời điểm chốt", LanHomeWifiKemExcelConstant.HEADER_CLOSING_TIME),
                    of("nghiem_thu", "Ngày nghiệm thu", LanHomeWifiKemExcelConstant.HEADER_ACCEPTANCE_DATE))),

    LAP_THIEU_HOMEWIFI("Lắp thiếu HomeWifi", TicketGroup.HA_TANG, "secondary",
            LapThieuHomeWifiExcelConstant.SOURCE_LABEL,
            List.of(InfraExcelConstant.HEADER_ACCOUNT),
            LapThieuHomeWifiExcelConstant.HEADER_STAFF_NAME,
            List.of(
                    of("ma_tram", "Mã trạm", InfraExcelConstant.HEADER_STATION),
                    of("thieu_hien_tai", "Hiện tại thiếu so với gói", LapThieuHomeWifiExcelConstant.HEADER_MISSING_NOW),
                    of("goi_cuoc", "HomeWifi theo gói cước", LapThieuHomeWifiExcelConstant.HEADER_PACKAGE_QTY),
                    of("dang_dung", "HomeWifi đang sử dụng", LapThieuHomeWifiExcelConstant.HEADER_IN_USE_QTY),
                    of("lap_luc_tk", "Lắp lúc triển khai", LapThieuHomeWifiExcelConstant.HEADER_INSTALLED_QTY),
                    of("thieu_luc_tk", "Thiếu lúc triển khai", LapThieuHomeWifiExcelConstant.HEADER_MISSING_AT_DEPLOY_QTY),
                    of("serial_thieu", "Serial HomeWifi thiếu", LapThieuHomeWifiExcelConstant.HEADER_MISSING_SERIAL),
                    of("ngay_tk", "Ngày triển khai", LapThieuHomeWifiExcelConstant.HEADER_DEPLOY_DATE),
                    of("ont", "Mã ONT", LapThieuHomeWifiExcelConstant.HEADER_ONT_CODE),
                    of("serial_ont", "Serial ONT", LapThieuHomeWifiExcelConstant.HEADER_SERIAL_ONT)));

    private final String label;
    private final TicketGroup group;
    private final String color;
    private final String sourceLabel;
    private final List<String> keyHeaders;
    private final String staffHeader;
    private final List<TicketAttribute> attributes;

    TicketType(String label, TicketGroup group, String color, String sourceLabel,
               List<String> keyHeaders, String staffHeader, List<TicketAttribute> attributes) {
        this.label = label;
        this.group = group;
        this.color = color;
        this.sourceLabel = sourceLabel;
        this.keyHeaders = keyHeaders;
        this.staffHeader = staffHeader;
        this.attributes = attributes;
    }

    public String getLabel() {
        return label;
    }

    public TicketGroup getGroup() {
        return group;
    }

    /** Tên màu Bootstrap (danger, warning, info, secondary...) cho badge của loại này. */
    public String getColor() {
        return color;
    }

    /** Nhãn của loại này trong cột "Loại dữ liệu" của file hạ tầng; null nếu loại này không đến từ file đó. */
    public String getSourceLabel() {
        return sourceLabel;
    }

    /** Các cột Excel ghép lại thành mã ca (ticket_code). */
    public List<String> getKeyHeaders() {
        return keyHeaders;
    }

    /** Cột Excel chứa tên người phụ trách / triển khai (có thể null). */
    public String getStaffHeader() {
        return staffHeader;
    }

    public List<TicketAttribute> getAttributes() {
        return attributes;
    }

    /** Trả về null nếu không khớp loại nào. */
    public static TicketType fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (TicketType t : values()) {
            if (t.name().equalsIgnoreCase(code.trim())) return t;
        }
        return null;
    }

    /** Tìm loại theo nhãn trong cột "Loại dữ liệu" của file Excel (không phân biệt hoa thường / khoảng trắng thừa). */
    public static TicketType fromSourceLabel(String text) {
        String needle = normalize(text);
        if (needle.isEmpty()) return null;
        for (TicketType t : values()) {
            if (t.sourceLabel != null && normalize(t.sourceLabel).equals(needle)) return t;
        }
        return null;
    }

    public static List<TicketType> ofGroup(TicketGroup group) {
        List<TicketType> result = new ArrayList<>();
        for (TicketType t : values()) {
            if (t.group == group) result.add(t);
        }
        return result;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
