package com.huydung.custmapapplication.constant;

/**
 * Loại ca LAP_THIEU_HOMEWIFI (Lắp thiếu HomeWifi) trong file kiểm tra hạ tầng.
 * Các cột dùng chung (Account, Mã trạm...) xem {@link InfraExcelConstant}.
 */
public final class LapThieuHomeWifiExcelConstant {

    private LapThieuHomeWifiExcelConstant() {
    }

    /** Giá trị trong cột "Loại dữ liệu" của các dòng thuộc loại này. */
    public static final String SOURCE_LABEL = "Lắp thiếu HomeWifi";

    /** Cột chứa tên nhân viên triển khai (người phụ trách). */
    public static final String HEADER_STAFF_NAME = "User nhân viên triển khai";

    public static final String HEADER_MISSING_NOW = "Hiện tại thiếu so với gói cước";
    public static final String HEADER_PACKAGE_QTY = "Số lượng Homewifi theo gói cước";
    public static final String HEADER_IN_USE_QTY = "Số lượng Homewifi thực tế khách hàng đang sử dụng";
    public static final String HEADER_INSTALLED_QTY = "Số lượng Homewifi lắp đặt tại thời điểm triển khai";
    public static final String HEADER_MISSING_AT_DEPLOY_QTY =
            "Số lượng Homewifi lắp thiếu so với gói cước tại thời điểm triển khai";
    public static final String HEADER_MISSING_SERIAL = "Serial home wifi thiếu tại thời điểm triển khai";
    public static final String HEADER_DEPLOY_DATE = "Ngày thuê bao Triển khai mới, lắp đặt thêm thiết bị";
    public static final String HEADER_ONT_CODE = "Mã hàng hóa ONT";
    public static final String HEADER_SERIAL_ONT = "Serial ONT";
}
