package com.huydung.custmapapplication.export.builder;

import com.huydung.custmapapplication.common.util.ExportMarker;
import com.huydung.custmapapplication.photo.entity.TicketPhotoEntity;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.user.entity.RoleEntity;
import com.huydung.custmapapplication.user.entity.UserEntity;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dựng file Excel báo cáo gồm 3 sheet:
 *  1. "Nhân viên"       - thông tin nhân viên + thống kê số ca theo từng trạng thái + chú giải màu
 *  2. "Phản ánh"        - toàn bộ ca, mỗi dòng tô màu theo trạng thái (Đã xử lý = xanh lá, KLL = đỏ, ...)
 *  3. "Đã xử lý & KLL"  - riêng các ca đã có kết quả, kèm thời điểm xử lý và vị trí ảnh chụp gần nhất
 */
@Component
public class TicketWorkbookBuilder {

    /** Dữ liệu đầu vào đã được lọc sẵn bởi tầng service. */
    public record ExportContext(List<CustomerTicketEntity> tickets,
                                Map<Long, Integer> photoCounts,
                                Map<Long, TicketPhotoEntity> lastPhotos,
                                List<UserEntity> users,
                                String exportedBy,
                                LocalDateTime exportedAt,
                                String staffFilter,     // null = tất cả nhân viên
                                boolean onlyChanged,
                                int ttlMinutes) {
    }

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HM_DMY = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final int MAX_CELL_LENGTH = 32000;
    private static final int RED_BRAND = 0xEE0033;   // đỏ Viettel
    private static final int BLUE_DARK = 0x1F4E78;

    // ---- Sheet "Phản ánh": 6 cột đầu của phần "thông tin làm việc" (C..H) nằm ngay sau Giao NV / Mã PA để dễ thấy thay đổi
    private static final String[] T_TITLES = {
            "Giao nhân viên", "Mã phản ánh", "Trạng thái", "Thời điểm xử lý / lưu trữ", "Giờ hẹn", "Ghi chú gọi", "Số ảnh", "Ghim",
            "Khu vực", "Cụm xã", "Xã", "Nhân viên địa bàn", "Tên dịch vụ", "Tên KH", "SĐT liên hệ KH", "Địa chỉ lắp đặt",
            "Số liên hệ nhập PA", "Thể loại", "Mức ưu tiên", "NV kỹ thuật (WO)", "SĐT NV kỹ thuật", "Nội dung khiếu nại",
            "Tiến độ GNOC", "Thuê bao phản ánh", "Mã tủ", "Cập nhật lần cuối"};
    private static final int[] T_WIDTHS = {
            20, 16, 26, 18, 24, 34, 8, 7,
            16, 16, 16, 18, 22, 24, 15, 40,
            15, 18, 14, 22, 15, 60,
            36, 26, 20, 18};
    private static final int T_WORK_COLS = 8;                            // số cột đầu là "thông tin làm việc" (header màu xanh)
    private static final Set<Integer> T_WRAP = Set.of(5, 15, 21, 22);    // ghi chú, địa chỉ, nội dung khiếu nại, tiến độ
    private static final Set<Integer> T_CENTER = Set.of(2, 3, 4, 6, 7);

    // ---- Sheet "Đã xử lý & KLL"
    private static final String[] P_TITLES = {
            "STT", "Kết quả", "Thời điểm xử lý", "Mã phản ánh", "Nhân viên", "Tên KH", "Thuê bao phản ánh", "SĐT liên hệ KH",
            "Địa chỉ lắp đặt", "Mã tủ", "Ghi chú gọi", "Số ảnh", "Ảnh gần nhất lúc", "Người chụp", "Vị trí chụp (Google Maps)",
            "Địa chỉ khi chụp"};
    private static final int[] P_WIDTHS = {6, 24, 18, 16, 20, 24, 26, 15, 40, 18, 34, 8, 18, 20, 28, 44};
    private static final Set<Integer> P_WRAP = Set.of(8, 10, 15);

    // ===================== ĐIỂM VÀO =====================

    public void write(ExportContext ctx, OutputStream out) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            buildStaffSheet(wb, st, ctx);
            buildTicketSheet(wb, st, ctx);
            buildProcessedSheet(wb, st, ctx);

            wb.getProperties().getCoreProperties().setCreator("CustMap");
            wb.getProperties().getCoreProperties().setTitle("Báo cáo ca phản ánh");
            ExportMarker.mark(wb);   // đánh dấu để chặn import nhầm file này
            wb.write(out);
        }
    }

    // ===================== SHEET 1: NHÂN VIÊN =====================

    private static final class StaffStat {
        final String name;
        UserEntity user;
        int total, done, kll, appointed, overdue, other, processing, notInImport;

        StaffStat(String name) {
            this.name = name;
        }

        void add(ExportStatus s) {
            total++;
            switch (s) {
                case DONE -> done++;
                case KLL -> kll++;
                case OVERDUE -> { appointed++; overdue++; }
                case HEN_GIO -> appointed++;
                case HEN_KHAC -> other++;
                case NOT_IN_IMPORT -> notInImport++;
                default -> processing++;
            }
        }
    }

    private void buildStaffSheet(XSSFWorkbook wb, Styles st, ExportContext ctx) {
        XSSFSheet sh = wb.createSheet("Nhân viên");
        sh.setDisplayGridlines(false);
        final String[] titles = {"STT", "Họ tên nhân viên", "Tên đăng nhập", "Số điện thoại", "Tài khoản", "Tổng ca",
                ExportStatus.DONE.getShortLabel(), ExportStatus.KLL.getShortLabel(), ExportStatus.HEN_GIO.getShortLabel(),
                "Trong đó quá hẹn", ExportStatus.HEN_KHAC.getShortLabel(), ExportStatus.DANG_XU_LY.getShortLabel(),
                ExportStatus.NOT_IN_IMPORT.getShortLabel(), "Tỷ lệ đã xử lý"};
        final int[] widths = {6, 28, 18, 15, 18, 10, 11, 9, 10, 12, 10, 12, 14, 12};
        final ExportStatus[] headerStatus = {null, null, null, null, null, null, ExportStatus.DONE, ExportStatus.KLL,
                ExportStatus.HEN_GIO, ExportStatus.OVERDUE, ExportStatus.HEN_KHAC, ExportStatus.DANG_XU_LY,
                ExportStatus.NOT_IN_IMPORT, null};
        for (int i = 0; i < widths.length; i++) sh.setColumnWidth(i, widths[i] * 256);

        int r = 0;
        Row title = sh.createRow(r);
        title.setHeightInPoints(28);
        text(title, 0, "BÁO CÁO CA PHẢN ÁNH", st.title());
        merge(sh, r++, 0, titles.length - 1);

        String staffLabel = ctx.staffFilter() == null ? "Tất cả nhân viên" : "Nhân viên: " + ctx.staffFilter();
        String scopeLabel = ctx.onlyChanged() ? "Chỉ ca có thay đổi" : "Tất cả ca";
        Row info = sh.createRow(r);
        text(info, 0, "Xuất lúc " + DT.format(ctx.exportedAt()) + "  •  Bởi: " + ctx.exportedBy()
                + "  •  " + staffLabel + "  •  " + scopeLabel, st.subtitle());
        merge(sh, r++, 0, titles.length - 1);

        Row warn = sh.createRow(r);
        text(warn, 0, "⚠ File này chỉ được lưu trên hệ thống " + ctx.ttlMinutes()
                + " phút rồi tự động xóa. Hãy tải về máy ngay.", st.warnLine());
        merge(sh, r++, 0, titles.length - 1);
        r++;

        // ---- gom thống kê theo nhân viên
        Map<String, UserEntity> userByName = new HashMap<>();
        for (UserEntity u : ctx.users()) {
            if (u.getFullName() != null) userByName.put(u.getFullName().trim().toLowerCase(Locale.ROOT), u);
        }
        Map<String, StaffStat> stats = new LinkedHashMap<>();
        for (CustomerTicketEntity t : ctx.tickets()) {
            String name = t.getAssignedStaff() == null ? "" : t.getAssignedStaff().trim();
            String key = name.toLowerCase(Locale.ROOT);
            StaffStat s = stats.computeIfAbsent(key, k -> new StaffStat(name.isEmpty() ? "(Chưa giao nhân viên)" : name));
            s.add(ExportStatus.of(t, ctx.exportedAt()));
        }
        // Xuất toàn bộ ca của mọi nhân viên: liệt kê cả tài khoản nhân viên đang hoạt động chưa có ca nào
        if (ctx.staffFilter() == null && !ctx.onlyChanged()) {
            for (UserEntity u : ctx.users()) {
                if (isActiveStaff(u) && u.getFullName() != null) {
                    stats.computeIfAbsent(u.getFullName().trim().toLowerCase(Locale.ROOT), k -> new StaffStat(u.getFullName().trim()));
                }
            }
        }
        List<StaffStat> list = new ArrayList<>(stats.values());
        Collator collator = Collator.getInstance(Locale.forLanguageTag("vi"));
        list.sort(Comparator.comparing((StaffStat s) -> s.name, collator));

        // ---- dòng tiêu đề
        Row head = sh.createRow(r++);
        head.setHeightInPoints(34);
        for (int i = 0; i < titles.length; i++) {
            ExportStatus hs = headerStatus[i];
            text(head, i, titles[i], hs == null ? st.header(RED_BRAND, 0xFFFFFF) : st.header(hs.getFillRgb(), hs.getFontRgb()));
        }

        // ---- dòng dữ liệu
        int stt = 1;
        int sumTotal = 0, sumDone = 0, sumKll = 0, sumAppointed = 0, sumOverdue = 0, sumOther = 0, sumProcessing = 0, sumNotIn = 0;
        for (StaffStat s : list) {
            s.user = userByName.get(s.name.trim().toLowerCase(Locale.ROOT));
            Row row = sh.createRow(r++);
            number(row, 0, stt++, st.cell(plain(HorizontalAlignment.CENTER)));
            text(row, 1, s.name, st.cell(bold(HorizontalAlignment.LEFT)));
            text(row, 2, s.user != null ? nz(s.user.getUsername()) : "—", st.cell(plain(HorizontalAlignment.LEFT)));
            text(row, 3, s.user != null && s.user.getPhone() != null ? s.user.getPhone() : "", st.cell(plain(HorizontalAlignment.LEFT)));
            String account = s.user == null ? "Chưa có tài khoản"
                    : (Integer.valueOf(1).equals(s.user.getStatus()) ? "Hoạt động" : "Đã khóa");
            text(row, 4, account, st.cell(plain(HorizontalAlignment.CENTER)));
            number(row, 5, s.total, st.cell(bold(HorizontalAlignment.CENTER)));
            count(row, 6, s.done, ExportStatus.DONE, st);
            count(row, 7, s.kll, ExportStatus.KLL, st);
            count(row, 8, s.appointed, ExportStatus.HEN_GIO, st);
            count(row, 9, s.overdue, ExportStatus.OVERDUE, st);
            count(row, 10, s.other, ExportStatus.HEN_KHAC, st);
            count(row, 11, s.processing, ExportStatus.DANG_XU_LY, st);
            count(row, 12, s.notInImport, ExportStatus.NOT_IN_IMPORT, st);
            rate(row, 13, s.done, s.total - s.notInImport, st);

            sumTotal += s.total; sumDone += s.done; sumKll += s.kll; sumAppointed += s.appointed;
            sumOverdue += s.overdue; sumOther += s.other; sumProcessing += s.processing; sumNotIn += s.notInImport;
        }

        // ---- dòng tổng
        Row total = sh.createRow(r++);
        total.setHeightInPoints(22);
        XSSFCellStyle totalText = st.cell(totalSpec(HorizontalAlignment.LEFT));
        XSSFCellStyle totalNum = st.cell(totalSpec(HorizontalAlignment.CENTER));
        text(total, 0, "", totalText);
        text(total, 1, "TỔNG CỘNG", totalText);
        for (int c = 2; c <= 4; c++) text(total, c, "", totalText);
        number(total, 5, sumTotal, totalNum);
        number(total, 6, sumDone, totalNum);
        number(total, 7, sumKll, totalNum);
        number(total, 8, sumAppointed, totalNum);
        number(total, 9, sumOverdue, totalNum);
        number(total, 10, sumOther, totalNum);
        number(total, 11, sumProcessing, totalNum);
        number(total, 12, sumNotIn, totalNum);
        rate(total, 13, sumDone, sumTotal - sumNotIn, st);
        r++;

        Row note = sh.createRow(r++);
        text(note, 0, "Tỷ lệ đã xử lý = Đã xử lý ÷ (Tổng ca − Không còn trong file). \"Hẹn giờ\" gồm cả ca quá hẹn.", st.subtitle());
        merge(sh, r - 1, 0, titles.length - 1);
        r++;

        // ---- chú giải màu
        Row legendTitle = sh.createRow(r++);
        text(legendTitle, 0, "CHÚ GIẢI MÀU (sheet \"Phản ánh\" và \"Đã xử lý & KLL\")", st.sectionTitle());
        merge(sh, r - 1, 0, titles.length - 1);
        for (ExportStatus s : ExportStatus.values()) {
            Row row = sh.createRow(r++);
            row.setHeightInPoints(20);
            text(row, 1, s.getLabel(), st.cell(statusSpec(s, true, false, HorizontalAlignment.LEFT)));
            text(row, 2, s.getDescription(), st.cell(plain(HorizontalAlignment.LEFT)));
            merge(sh, r - 1, 2, 8);
        }
    }

    // ===================== SHEET 2: PHẢN ÁNH =====================

    private void buildTicketSheet(XSSFWorkbook wb, Styles st, ExportContext ctx) {
        XSSFSheet sh = wb.createSheet("Phản ánh");
        for (int i = 0; i < T_WIDTHS.length; i++) sh.setColumnWidth(i, T_WIDTHS[i] * 256);

        Row head = sh.createRow(0);
        head.setHeightInPoints(34);
        for (int i = 0; i < T_TITLES.length; i++) {
            text(head, i, T_TITLES[i], i < T_WORK_COLS ? st.header(BLUE_DARK, 0xFFFFFF) : st.header(RED_BRAND, 0xFFFFFF));
        }

        int r = 1;
        for (CustomerTicketEntity t : ctx.tickets()) {
            ExportStatus status = ExportStatus.of(t, ctx.exportedAt());
            String[] v = new String[T_TITLES.length];
            v[0] = t.getAssignedStaff();
            v[1] = t.getTicketCode();
            v[2] = status.getLabel();
            v[3] = t.getArchivedAt() == null ? "" : DT.format(t.getArchivedAt());
            v[4] = appointmentText(t);
            v[5] = t.getCallNote();
            v[6] = null; // số ảnh: ghi dạng số bên dưới
            v[7] = Boolean.TRUE.equals(t.getIsPinned()) ? "★" : "";
            v[8] = t.getArea();
            v[9] = t.getCommuneCluster();
            v[10] = t.getCommune();
            v[11] = t.getLocalStaffCode();
            v[12] = t.getServiceName();
            v[13] = t.getCustomerName();
            v[14] = t.getContactPhone();
            v[15] = t.getInstallAddress();
            v[16] = t.getReportPhone();
            v[17] = t.getCategory();
            v[18] = t.getPriorityLevel();
            v[19] = t.getTechStaffName();
            v[20] = t.getTechStaffPhone();
            v[21] = t.getComplaintContent();
            v[22] = t.getGnocProgress();
            v[23] = t.getSubscriberAccount();
            v[24] = t.getCabinetCode();
            v[25] = t.getUpdatedAt() == null ? "" : DT.format(t.getUpdatedAt());

            Row row = sh.createRow(r++);
            for (int c = 0; c < T_TITLES.length; c++) {
                HorizontalAlignment h = T_CENTER.contains(c) ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT;
                boolean wrap = T_WRAP.contains(c);
                XSSFCellStyle style = st.cell(statusSpec(status, wrap, c == 2, h));
                if (c == 6) {
                    number(row, c, ctx.photoCounts().getOrDefault(t.getId(), 0), style);
                } else {
                    text(row, c, v[c], style);
                }
            }
        }

        if (ctx.tickets().isEmpty()) {
            Row row = sh.createRow(1);
            text(row, 0, "Không có ca nào trong phạm vi đã chọn.", st.subtitle());
            merge(sh, 1, 0, 5);
        } else {
            sh.setAutoFilter(new CellRangeAddress(0, r - 1, 0, T_TITLES.length - 1));
        }
        sh.createFreezePane(2, 1);   // giữ cố định dòng tiêu đề và 2 cột đầu (Giao NV, Mã PA)
        landscapeFit(sh);
    }

    // ===================== SHEET 3: ĐÃ XỬ LÝ & KLL =====================

    private void buildProcessedSheet(XSSFWorkbook wb, Styles st, ExportContext ctx) {
        XSSFSheet sh = wb.createSheet("Đã xử lý & KLL");
        for (int i = 0; i < P_WIDTHS.length; i++) sh.setColumnWidth(i, P_WIDTHS[i] * 256);

        Row head = sh.createRow(0);
        head.setHeightInPoints(34);
        for (int i = 0; i < P_TITLES.length; i++) {
            text(head, i, P_TITLES[i], st.header(RED_BRAND, 0xFFFFFF));
        }

        List<CustomerTicketEntity> processed = new ArrayList<>();
        for (CustomerTicketEntity t : ctx.tickets()) {
            if (ExportStatus.of(t, ctx.exportedAt()).isProcessed()) processed.add(t);
        }
        // Mới xử lý lên đầu
        processed.sort(Comparator.comparing(CustomerTicketEntity::getArchivedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        CreationHelper helper = wb.getCreationHelper();
        int r = 1;
        int stt = 1;
        for (CustomerTicketEntity t : processed) {
            ExportStatus status = ExportStatus.of(t, ctx.exportedAt());
            TicketPhotoEntity photo = ctx.lastPhotos().get(t.getId());
            int photoCount = ctx.photoCounts().getOrDefault(t.getId(), 0);

            Row row = sh.createRow(r++);
            XSSFCellStyle center = st.cell(statusSpec(status, false, false, HorizontalAlignment.CENTER));
            XSSFCellStyle left = st.cell(statusSpec(status, false, false, HorizontalAlignment.LEFT));
            XSSFCellStyle wrap = st.cell(statusSpec(status, true, false, HorizontalAlignment.LEFT));

            number(row, 0, stt++, center);
            text(row, 1, status.getLabel(), st.cell(statusSpec(status, false, true, HorizontalAlignment.LEFT)));
            text(row, 2, t.getArchivedAt() == null ? "" : DT.format(t.getArchivedAt()), center);
            text(row, 3, t.getTicketCode(), left);
            text(row, 4, t.getAssignedStaff(), left);
            text(row, 5, t.getCustomerName(), left);
            text(row, 6, t.getSubscriberAccount(), left);
            text(row, 7, t.getContactPhone(), left);
            text(row, 8, t.getInstallAddress(), wrap);
            text(row, 9, t.getCabinetCode(), left);
            text(row, 10, t.getCallNote(), wrap);
            // Ca đã xử lý mà chưa có ảnh nào: in đỏ để dễ kiểm tra
            number(row, 11, photoCount, (status == ExportStatus.DONE && photoCount == 0)
                    ? st.cell(warnSpec(status)) : center);
            if (photo != null) {
                text(row, 12, photo.getTakenAt() == null ? "" : DT.format(photo.getTakenAt()), center);
                text(row, 13, photo.getTakenByName(), left);
                if (photo.getLatitude() != null && photo.getLongitude() != null) {
                    String coords = String.format(Locale.ROOT, "%.6f, %.6f", photo.getLatitude(), photo.getLongitude());
                    Cell cell = row.createCell(14);
                    cell.setCellValue(coords);
                    cell.setCellStyle(st.cell(linkSpec(status)));
                    Hyperlink link = helper.createHyperlink(HyperlinkType.URL);
                    link.setAddress("https://www.google.com/maps?q="
                            + String.format(Locale.ROOT, "%.6f,%.6f", photo.getLatitude(), photo.getLongitude()));
                    cell.setHyperlink(link);
                } else {
                    text(row, 14, "", left);
                }
                text(row, 15, photo.getAddress(), wrap);
            } else {
                for (int c = 12; c <= 15; c++) text(row, c, "", left);
            }
        }

        if (processed.isEmpty()) {
            Row row = sh.createRow(1);
            text(row, 0, "Chưa có ca nào đã xử lý hoặc KLL trong phạm vi đã chọn.", st.subtitle());
            merge(sh, 1, 0, 5);
        } else {
            sh.setAutoFilter(new CellRangeAddress(0, r - 1, 0, P_TITLES.length - 1));
        }
        sh.createFreezePane(4, 1);
        landscapeFit(sh);
    }

    // ===================== HÀM HỖ TRỢ GHI Ô =====================

    private static String appointmentText(CustomerTicketEntity t) {
        if (t.getAppointmentStart() == null) {
            return t.getAppointmentTime() == null ? "" : t.getAppointmentTime();
        }
        String s = HM_DMY.format(t.getAppointmentStart());
        return t.getAppointmentEnd() == null ? s : s + " - " + HM_DMY.format(t.getAppointmentEnd());
    }

    private static boolean isActiveStaff(UserEntity u) {
        if (!Integer.valueOf(1).equals(u.getStatus()) || u.getRoles() == null) return false;
        for (RoleEntity role : u.getRoles()) {
            if ("ROLE_STAFF".equals(role.getCode())) return true;
        }
        return false;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static void text(Row row, int col, String value, XSSFCellStyle style) {
        Cell cell = row.createCell(col);
        String v = value == null ? "" : value;
        if (v.length() > MAX_CELL_LENGTH) v = v.substring(0, MAX_CELL_LENGTH) + "…";
        cell.setCellValue(v);
        cell.setCellStyle(style);
    }

    private static void number(Row row, int col, double value, XSSFCellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // Ô thống kê: có số > 0 thì tô màu của trạng thái đó, bằng 0 thì để trắng cho đỡ rối
    private static void count(Row row, int col, int value, ExportStatus status, Styles st) {
        XSSFCellStyle style = value > 0
                ? st.cell(statusSpec(status, false, true, HorizontalAlignment.CENTER))
                : st.cell(plain(HorizontalAlignment.CENTER));
        number(row, col, value, style);
    }

    // Tỷ lệ phần trăm: >= 80% xanh, 50-79% vàng, dưới 50% cam
    private static void rate(Row row, int col, int done, int base, Styles st) {
        if (base <= 0) {
            text(row, col, "—", st.cell(plain(HorizontalAlignment.CENTER)));
            return;
        }
        double ratio = (double) done / base;
        ExportStatus colorOf = ratio >= 0.8 ? ExportStatus.DONE : (ratio >= 0.5 ? ExportStatus.HEN_GIO : ExportStatus.OVERDUE);
        Cell cell = row.createCell(col);
        cell.setCellValue(ratio);
        cell.setCellStyle(st.cell(new Spec(colorOf.getFillRgb(), colorOf.getFontRgb(), true, false, 10, false,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, false, "0.0%")));
    }

    private static void merge(XSSFSheet sh, int row, int fromCol, int toCol) {
        sh.addMergedRegion(new CellRangeAddress(row, row, fromCol, toCol));
    }

    private static void landscapeFit(XSSFSheet sh) {
        sh.setFitToPage(true);
        sh.getPrintSetup().setLandscape(true);
        sh.getPrintSetup().setFitWidth((short) 1);
        sh.getPrintSetup().setFitHeight((short) 0);
    }

    // ===================== KIỂU Ô =====================

    /** Mô tả một kiểu ô; dùng làm khóa để mỗi kiểu chỉ tạo đúng một lần (Excel giới hạn số kiểu ô). */
    private record Spec(int fill, int font, boolean bold, boolean italic, int size, boolean wrap,
                        HorizontalAlignment h, VerticalAlignment v, boolean border, boolean underline, String format) {
    }

    private static Spec plain(HorizontalAlignment h) {
        return new Spec(-1, 0x000000, false, false, 10, false, h, VerticalAlignment.CENTER, true, false, null);
    }

    private static Spec bold(HorizontalAlignment h) {
        return new Spec(-1, 0x000000, true, false, 10, false, h, VerticalAlignment.CENTER, true, false, null);
    }

    private static Spec totalSpec(HorizontalAlignment h) {
        return new Spec(0xD9D9D9, 0x000000, true, false, 10, false, h, VerticalAlignment.CENTER, true, false, null);
    }

    private static Spec statusSpec(ExportStatus s, boolean wrap, boolean bold, HorizontalAlignment h) {
        return new Spec(s.getFillRgb(), s.getFontRgb(), bold, false, 10, wrap, h, VerticalAlignment.TOP, true, false, null);
    }

    private static Spec warnSpec(ExportStatus s) {
        return new Spec(s.getFillRgb(), 0xC00000, true, false, 10, false, HorizontalAlignment.CENTER, VerticalAlignment.TOP, true, false, null);
    }

    private static Spec linkSpec(ExportStatus s) {
        return new Spec(s.getFillRgb(), 0x0563C1, false, false, 10, false, HorizontalAlignment.LEFT, VerticalAlignment.TOP, true, true, null);
    }

    private static XSSFColor rgb(int rgb) {
        return new XSSFColor(new byte[]{(byte) (rgb >> 16), (byte) (rgb >> 8), (byte) rgb}, null);
    }

    private static final class Styles {
        private final XSSFWorkbook wb;
        private final XSSFDataFormat dataFormat;
        private final Map<Spec, XSSFCellStyle> cache = new HashMap<>();

        Styles(XSSFWorkbook wb) {
            this.wb = wb;
            this.dataFormat = wb.createDataFormat();
        }

        XSSFCellStyle cell(Spec spec) {
            XSSFCellStyle existing = cache.get(spec);
            if (existing != null) return existing;
            XSSFCellStyle style = wb.createCellStyle();

            XSSFFont font = wb.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) spec.size());
            font.setBold(spec.bold());
            font.setItalic(spec.italic());
            if (spec.underline()) font.setUnderline(Font.U_SINGLE);
            font.setColor(rgb(spec.font()));
            style.setFont(font);

            if (spec.fill() >= 0) {
                style.setFillForegroundColor(rgb(spec.fill()));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            style.setAlignment(spec.h());
            style.setVerticalAlignment(spec.v());
            style.setWrapText(spec.wrap());
            if (spec.border()) {
                XSSFColor line = rgb(0xBFBFBF);
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
                style.setTopBorderColor(line);
                style.setBottomBorderColor(line);
                style.setLeftBorderColor(line);
                style.setRightBorderColor(line);
            }
            if (spec.format() != null) style.setDataFormat(dataFormat.getFormat(spec.format()));
            cache.put(spec, style);
            return style;
        }

        XSSFCellStyle title() {
            return cell(new Spec(-1, RED_BRAND, true, false, 16, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false, false, null));
        }

        XSSFCellStyle subtitle() {
            return cell(new Spec(-1, 0x595959, false, true, 10, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false, false, null));
        }

        XSSFCellStyle warnLine() {
            return cell(new Spec(0xFFF2CC, 0x7F6000, true, false, 10, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false, false, null));
        }

        XSSFCellStyle sectionTitle() {
            return cell(new Spec(-1, 0x000000, true, false, 11, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false, false, null));
        }

        XSSFCellStyle header(int fill, int fontRgb) {
            return cell(new Spec(fill, fontRgb, true, false, 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, false, null));
        }
    }
}
