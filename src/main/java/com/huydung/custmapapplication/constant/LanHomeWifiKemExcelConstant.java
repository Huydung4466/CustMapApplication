package com.huydung.custmapapplication.constant;

/**
 * Loại ca LAN_HOMEWIFI_KEM (LAN / HomeWifi kém) trong file kiểm tra hạ tầng.
 * Các cột dùng chung (Account, Mã trạm...) xem {@link InfraExcelConstant}.
 */
public final class LanHomeWifiKemExcelConstant {

    private LanHomeWifiKemExcelConstant() {
    }

    /** Giá trị trong cột "Loại dữ liệu" của các dòng thuộc loại này (viết đúng như trong file). */
    public static final String SOURCE_LABEL = "Lan +HWifi kém";

    /** Cột chứa tên người phụ trách. */
    public static final String HEADER_STAFF_NAME = "TEN_NHAN_VIEN";

    public static final String HEADER_ERROR_INFO = "Thông tin lỗi (Lan +HWifi kém, cột 22)";
    public static final String HEADER_CAUSE = "Nguyên nhân (Lan +HWifi kém)";
    public static final String HEADER_HANDLING_STATUS = "Trạng thái xử lý";
    public static final String HEADER_CONSECUTIVE_FAIL_DAYS = "SO_NGAY_LIEN_TIEP_KHONG_DAT";
    public static final String HEADER_RECEIVE_LEVEL_FAIL = "MUC_THU_KHONG_DAT";
    public static final String HEADER_LAN_CONNECTION_FAIL = "KET_NOI_LAN_KHONG_DAT";
    public static final String HEADER_ONLINE_STATUS = "TRANG_THAI_ONLINE";
    public static final String HEADER_MANUFACTURER = "HANG_SAN_XUAT";
    public static final String HEADER_SERIAL_ONT = "SERIAL_GPON_ONT";
    public static final String HEADER_SERIAL_HOMEWIFI = "SERIAL_GPON_HOMEWIFI";
    public static final String HEADER_HOMEWIFI_COUNT_BCCS = "SL_THIET_BI_HOMEWIFI_BCCS";
    public static final String HEADER_CLOSING_TIME = "THOI_DIEM_CHOT";
    public static final String HEADER_ACCEPTANCE_DATE = "NGAY_NGHIEM_THU";
}
