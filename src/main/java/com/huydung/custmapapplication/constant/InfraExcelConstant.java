package com.huydung.custmapapplication.constant;

/**
 * File kiểm tra hạ tầng: các tên cột DÙNG CHUNG cho mọi loại ca hạ tầng.
 * Tên cột riêng của từng loại nằm ở {@link PortKemExcelConstant},
 * {@link LanHomeWifiKemExcelConstant}, {@link LapThieuHomeWifiExcelConstant}.
 * Tên cột được so khớp không phân biệt hoa thường và bỏ khoảng trắng thừa (xem ExcelHeader).
 */
public final class InfraExcelConstant {

    private InfraExcelConstant() {
    }

    /** Cột phân loại từng dòng thuộc loại ca nào (giá trị trong cột là SOURCE_LABEL của từng loại). */
    public static final String HEADER_TYPE = "Loại dữ liệu";
    public static final String HEADER_ACCOUNT = "Account / Thuê bao dịch vụ";
    public static final String HEADER_MANAGER = "Nhân viên quản lý";
    public static final String HEADER_AREA = "Khu vực";
    public static final String HEADER_COMMUNE = "Phường/Xã";
    public static final String HEADER_DETAIL = "Chi tiết lỗi";
    public static final String HEADER_CUSTOMER_NAME = "Tên KH";
    public static final String HEADER_ADDRESS = "Địa chỉ";
    public static final String HEADER_PHONE = "Số ĐT liên hệ";
    public static final String HEADER_CABINET = "Mã tủ";

    /** Mã trạm: cả 3 loại hạ tầng đều đọc từ cột này (tên cột mang chữ "Port kém GPON" do file nguồn đặt). */
    public static final String HEADER_STATION = "Mã trạm (Port kém GPON)";
}
