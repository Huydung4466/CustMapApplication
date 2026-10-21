package com.huydung.custmapapplication.constant;

/**
 * Loại ca PORT_KEM (Port kém GPON) trong file kiểm tra hạ tầng.
 * Các cột dùng chung (Account, Mã trạm...) xem {@link InfraExcelConstant}.
 */
public final class PortKemExcelConstant {

    private PortKemExcelConstant() {
    }

    /** Giá trị trong cột "Loại dữ liệu" của các dòng thuộc loại này. */
    public static final String SOURCE_LABEL = "Port kém GPON";

    public static final String HEADER_DEVICE_CODE = "Mã thiết bị";
    public static final String HEADER_PORT = "Port";
    public static final String HEADER_RX_POWER = "Công suất thu (Port kém)";
    public static final String HEADER_HOLD_TIME = "Thời gian tồn(h)";
    public static final String HEADER_DEVICE_TYPE = "Loại thiết bị";
    public static final String HEADER_PERIPHERAL = "Thiết bị ngoại vi";
    public static final String HEADER_START_TIME = "Thời gian bắt đầu";
    public static final String HEADER_CUSTOMER_RANK = "Hạng KH";
    public static final String HEADER_CUSTOMER_TYPE = "Loại KH";
    public static final String HEADER_BILLING_BLOCK_STATUS = "Trạng thái chặn cắt cước";
    public static final String HEADER_DATA_SOURCE = "Nguồn dữ liệu";
}
