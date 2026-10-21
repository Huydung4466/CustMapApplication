package com.huydung.custmapapplication.ticket.type;

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

    PORT_KEM("Port kém GPON", TicketGroup.HA_TANG, "warning",
            "Port kém GPON",
            List.of("Account / Thuê bao dịch vụ", "Mã thiết bị", "Port"),
            null,
            List.of(
                    of("ma_tram", "Mã trạm", "Mã trạm (Port kém GPON)"),
                    of("cong_suat", "Công suất thu", "Công suất thu (Port kém)", "dBm"),
                    of("thoi_gian_ton", "Thời gian tồn", "Thời gian tồn(h)", "giờ"),
                    of("thiet_bi", "Thiết bị", "Mã thiết bị"),
                    of("port", "Port", "Port"),
                    of("loai_thiet_bi", "Loại thiết bị", "Loại thiết bị"),
                    of("ngoai_vi", "Thiết bị ngoại vi", "Thiết bị ngoại vi"),
                    of("bat_dau", "Bắt đầu từ", "Thời gian bắt đầu"),
                    of("hang_kh", "Hạng KH", "Hạng KH"),
                    of("loai_kh", "Loại KH", "Loại KH"),
                    of("chan_cuoc", "Chặn cắt cước", "Trạng thái chặn cắt cước"),
                    of("nguon", "Nguồn dữ liệu", "Nguồn dữ liệu"))),

    LAN_HOMEWIFI_KEM("LAN / HomeWifi kém", TicketGroup.HA_TANG, "info",
            "Lan +HWifi kém",
            List.of("Account / Thuê bao dịch vụ", "SERIAL_GPON_HOMEWIFI"),
            "TEN_NHAN_VIEN",
            List.of(
                    of("ma_tram", "Mã trạm", "Mã trạm (Port kém GPON)"),
                    of("loi", "Lỗi", "Thông tin lỗi (Lan +HWifi kém, cột 22)"),
                    of("nguyen_nhan", "Nguyên nhân", "Nguyên nhân (Lan +HWifi kém)"),
                    of("trang_thai", "Trạng thái xử lý", "Trạng thái xử lý"),
                    of("so_ngay", "Số ngày liên tiếp không đạt", "SO_NGAY_LIEN_TIEP_KHONG_DAT", "ngày"),
                    of("muc_thu", "Mức thu không đạt", "MUC_THU_KHONG_DAT"),
                    of("ket_noi_lan", "Kết nối LAN không đạt", "KET_NOI_LAN_KHONG_DAT"),
                    of("online", "Trạng thái online", "TRANG_THAI_ONLINE"),
                    of("hang_sx", "Hãng sản xuất", "HANG_SAN_XUAT"),
                    of("serial_ont", "Serial ONT", "SERIAL_GPON_ONT"),
                    of("serial_hw", "Serial HomeWifi", "SERIAL_GPON_HOMEWIFI"),
                    of("sl_bccs", "Số HomeWifi (BCCS)", "SL_THIET_BI_HOMEWIFI_BCCS"),
                    of("thoi_diem_chot", "Thời điểm chốt", "THOI_DIEM_CHOT"),
                    of("nghiem_thu", "Ngày nghiệm thu", "NGAY_NGHIEM_THU"))),

    LAP_THIEU_HOMEWIFI("Lắp thiếu HomeWifi", TicketGroup.HA_TANG, "secondary",
            "Lắp thiếu HomeWifi",
            List.of("Account / Thuê bao dịch vụ"),
            "User nhân viên triển khai",
            List.of(
                    of("ma_tram", "Mã trạm", "Mã trạm (Port kém GPON)"),
                    of("thieu_hien_tai", "Hiện tại thiếu so với gói", "Hiện tại thiếu so với gói cước"),
                    of("goi_cuoc", "HomeWifi theo gói cước", "Số lượng Homewifi theo gói cước"),
                    of("dang_dung", "HomeWifi đang sử dụng", "Số lượng Homewifi thực tế khách hàng đang sử dụng"),
                    of("lap_luc_tk", "Lắp lúc triển khai", "Số lượng Homewifi lắp đặt tại thời điểm triển khai"),
                    of("thieu_luc_tk", "Thiếu lúc triển khai", "Số lượng Homewifi lắp thiếu so với gói cước tại thời điểm triển khai"),
                    of("serial_thieu", "Serial HomeWifi thiếu", "Serial home wifi thiếu tại thời điểm triển khai"),
                    of("ngay_tk", "Ngày triển khai", "Ngày thuê bao Triển khai mới, lắp đặt thêm thiết bị"),
                    of("ont", "Mã ONT", "Mã hàng hóa ONT"),
                    of("serial_ont", "Serial ONT", "Serial ONT")));

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
