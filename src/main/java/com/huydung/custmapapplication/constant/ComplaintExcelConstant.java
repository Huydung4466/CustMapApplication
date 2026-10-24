package com.huydung.custmapapplication.constant;

/**
 * File phản ánh khách hàng (loại ca PHAN_ANH).
 * File này được đọc theo VỊ TRÍ cột (không đọc theo tên) nên nếu file nguồn đổi thứ tự cột
 * thì chỉ cần sửa các số bên dưới. Chỉ số bắt đầu từ 0 (cột A = 0).
 */
public final class ComplaintExcelConstant {

    private ComplaintExcelConstant() {
    }

    // ---------------- Tên cột dùng để nhận diện file ----------------

    /** Tiêu đề cột Mã phản ánh: có cột này thì file được nhận là file phản ánh. */
    public static final String HEADER_REPORT_CODE = "Mã phản ánh";

    // ---------------- Vị trí cột ----------------

    public static final int COL_ASSIGNED_STAFF = 0;      // Cột A: Giao nhân viên
    public static final int COL_REPORT_CODE = 1;         // Cột B: Mã phản ánh
    public static final int COL_AREA = 2;                // Cột C: Khu vực
    public static final int COL_COMMUNE_CLUSTER = 3;     // Cột D: Cụm xã
    public static final int COL_COMMUNE = 4;             // Cột E: Xã
    public static final int COL_LOCAL_STAFF_CODE = 5;    // Cột F: Nhân viên địa bàn
    public static final int COL_SERVICE_NAME = 7;        // Cột H: Tên dịch vụ
    public static final int COL_CUSTOMER_NAME = 8;       // Cột I: Tên KH
    public static final int COL_CONTACT_PHONE = 9;       // Cột J: SĐT liên hệ KH
    public static final int COL_INSTALL_ADDRESS = 10;    // Cột K: Địa chỉ lắp đặt
    public static final int COL_REPORT_PHONE = 11;       // Cột L: Số liên hệ nhập phản ánh
    public static final int COL_CATEGORY = 13;           // Cột N: Thể loại
    public static final int COL_PRIORITY_LEVEL = 18;     // Cột S: Mức ưu tiên SCKN
    public static final int COL_TECH_STAFF_NAME = 21;    // Cột V: NV kỹ thuật được giao WO
    public static final int COL_TECH_STAFF_PHONE = 22;   // Cột W: SĐT của NV kỹ thuật
    public static final int COL_COMPLAINT_CONTENT = 23;  // Cột X: Nội dung khiếu nại
    public static final int COL_GNOC_PROGRESS = 25;      // Cột Z: Tiến độ của GNOC
    public static final int COL_SUBSCRIBER_ACCOUNT = 28; // Cột AC: Thuê bao phản ánh
    public static final int COL_CABINET_CODE = 30;       // Cột AE: Mã tủ
}
