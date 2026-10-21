package com.huydung.custmapapplication.ticket.importer;

import com.huydung.custmapapplication.ticket.converter.TicketConverter;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.type.TicketAttribute;
import com.huydung.custmapapplication.ticket.type.TicketGroup;
import com.huydung.custmapapplication.ticket.type.TicketType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * File kiểm tra hạ tầng (nhận diện bằng cột "Loại dữ liệu" + "Account / Thuê bao dịch vụ").
 * Một file chứa nhiều loại ca (Port kém, LAN/HomeWifi kém, Lắp thiếu HomeWifi). Mọi cột đều được đọc theo TÊN,
 * cách đọc từng loại (mã ca, thuộc tính, người phụ trách) lấy từ khai báo trong {@link TicketType}.
 */
@Component
public class InfraFileFormat implements TicketFileFormat {

    private static final String H_TYPE = "Loại dữ liệu";
    private static final String H_ACCOUNT = "Account / Thuê bao dịch vụ";
    private static final String H_MANAGER = "Nhân viên quản lý";
    private static final String H_AREA = "Khu vực";
    private static final String H_COMMUNE = "Phường/Xã";
    private static final String H_DETAIL = "Chi tiết lỗi";
    private static final String H_NAME = "Tên KH";
    private static final String H_ADDRESS = "Địa chỉ";
    private static final String H_PHONE = "Số ĐT liên hệ";
    private static final String H_CABINET = "Mã tủ";

    // Thiếu một trong các cột này thì file không dùng được -> báo lỗi ngay thay vì import sai
    private static final List<String> REQUIRED = List.of(H_TYPE, H_ACCOUNT, H_MANAGER, H_AREA);

    private static final int MAX_CODE_LENGTH = 100;       // độ dài cột ticket_code
    private static final int SUMMARY_ATTRIBUTES = 4;      // số thuộc tính đầu đưa vào dòng tóm tắt
    private static final int MAX_LISTED_UNKNOWN = 5;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TicketConverter converter;

    public InfraFileFormat(TicketConverter converter) {
        this.converter = converter;
    }

    @Override
    public String name() {
        return "Kiểm tra hạ tầng";
    }

    @Override
    public boolean supports(ExcelHeader header) {
        return header.has(H_TYPE) && header.has(H_ACCOUNT);
    }

    @Override
    public Set<TicketType> coveredTypes() {
        return Set.copyOf(TicketType.ofGroup(TicketGroup.HA_TANG));
    }

    @Override
    public TicketRowReader open(ExcelHeader header, DataFormatter formatter) {
        List<String> missing = REQUIRED.stream().filter(h -> !header.has(h)).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("File hạ tầng thiếu cột bắt buộc: " + String.join(", ", missing));
        }
        return new InfraRowReader(header, formatter);
    }

    private final class InfraRowReader implements TicketRowReader {

        private final ExcelHeader header;
        private final DataFormatter formatter;
        private final Map<String, Integer> unknownTypes = new TreeMap<>();
        private final Map<TicketType, Integer> missingKey = new EnumMap<>(TicketType.class);
        private final List<String> missingColumns = new ArrayList<>();
        private int unknownTypeRows = 0;

        InfraRowReader(ExcelHeader header, DataFormatter formatter) {
            this.header = header;
            this.formatter = formatter;
            // Cột thuộc tính khai báo nhưng file không có -> cảnh báo một lần (vẫn import, thuộc tính đó để trống)
            for (TicketType t : TicketType.ofGroup(TicketGroup.HA_TANG)) {
                for (TicketAttribute a : t.getAttributes()) {
                    if (!header.has(a.header())) {
                        missingColumns.add(t.getLabel() + " → " + a.header());
                    }
                }
                for (String h : t.getKeyHeaders()) {
                    if (!header.has(h)) {
                        missingColumns.add(t.getLabel() + " (cột tạo mã ca) → " + h);
                    }
                }
            }
        }

        @Override
        public RowKey readKey(Row row) {
            String typeText = text(row, H_TYPE);
            if (typeText.isEmpty()) return null; // dòng trống

            TicketType type = TicketType.fromSourceLabel(typeText);
            if (type == null) {
                unknownTypeRows++;
                unknownTypes.merge(typeText, 1, Integer::sum);
                return null;
            }

            List<String> parts = new ArrayList<>();
            for (String h : type.getKeyHeaders()) {
                String v = text(row, h);
                if (v.isEmpty()) {
                    missingKey.merge(type, 1, Integer::sum);
                    return null;
                }
                parts.add(v);
            }
            return new RowKey(type, buildCode(type, parts));
        }

        @Override
        public void fill(Row row, RowKey key, CustomerTicketEntity entity) {
            TicketType type = key.type();
            entity.setTicketType(type);
            entity.setAssignedStaff(text(row, H_MANAGER));
            entity.setArea(text(row, H_AREA));
            entity.setCommune(text(row, H_COMMUNE));
            entity.setCategory(text(row, H_DETAIL));
            entity.setCustomerName(text(row, H_NAME));
            entity.setInstallAddress(text(row, H_ADDRESS));
            entity.setContactPhone(converter.normalizePhone(text(row, H_PHONE)));
            entity.setSubscriberAccount(text(row, H_ACCOUNT));
            entity.setCabinetCode(text(row, H_CABINET).toUpperCase());
            entity.setTechStaffName(type.getStaffHeader() == null ? "" : text(row, type.getStaffHeader()));

            Map<String, String> attrs = new LinkedHashMap<>();
            List<String> summary = new ArrayList<>();
            for (TicketAttribute a : type.getAttributes()) {
                String v = text(row, a.header());
                if (v.isEmpty()) continue;
                attrs.put(a.key(), v);
                if (summary.size() < SUMMARY_ATTRIBUTES) {
                    summary.add(a.label() + ": " + v + (a.unit() != null ? " " + a.unit() : ""));
                }
            }
            entity.setAttributes(attrs);
            // Dòng tóm tắt đặt vào complaintContent để các trang cũ (Đã hẹn, Lịch sử, báo cáo Excel) vẫn hiện được nội dung
            entity.setComplaintContent(type.getLabel() + (summary.isEmpty() ? "" : " — " + String.join(" • ", summary)));
            entity.setUpdatedAt(LocalDateTime.now());
        }

        @Override
        public List<String> warnings() {
            List<String> w = new ArrayList<>();
            if (unknownTypeRows > 0) {
                String sample = unknownTypes.entrySet().stream().limit(MAX_LISTED_UNKNOWN)
                        .map(e -> "\"" + e.getKey() + "\" (" + e.getValue() + ")")
                        .collect(Collectors.joining(", "));
                w.add("Bỏ qua " + unknownTypeRows + " dòng có Loại dữ liệu chưa khai báo trong TicketType: " + sample);
            }
            missingKey.forEach((type, n) ->
                    w.add("Bỏ qua " + n + " dòng " + type.getLabel() + " thiếu dữ liệu tạo mã ca"));
            if (!missingColumns.isEmpty()) {
                w.add("File không có cột: " + String.join("; ", missingColumns));
            }
            return w;
        }

        // Đọc ô theo tên cột. Cột không tồn tại -> chuỗi rỗng.
        private String text(Row row, String headerName) {
            int idx = header.indexOf(headerName);
            if (idx < 0) return "";
            Cell cell = row.getCell(idx);
            if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().format(DATE_TIME);
            }
            return converter.getCellString(row, idx, formatter);
        }
    }

    // Mã ca = "<LOẠI>|phần1|phần2..."; quá dài (cột chỉ 100 ký tự) thì rút gọn bằng SHA-1 để vẫn ổn định giữa các lần import
    private static String buildCode(TicketType type, List<String> parts) {
        String raw = type.name() + "|" + String.join("|", parts);
        if (raw.length() <= MAX_CODE_LENGTH) return raw;
        return type.name() + "#" + sha1(raw);
    }

    private static String sha1(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
