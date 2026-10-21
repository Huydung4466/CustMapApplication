package com.huydung.custmapapplication.ticket.converter;

import com.huydung.custmapapplication.kmz.entity.KmzLocationEntity;
import com.huydung.custmapapplication.ticket.dto.TicketAttributeView;
import com.huydung.custmapapplication.ticket.dto.TicketResponseDTO;
import com.huydung.custmapapplication.ticket.dto.UpdateTicketActionRequest;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.type.TicketAttribute;
import com.huydung.custmapapplication.ticket.type.TicketType;
import org.apache.poi.ss.usermodel.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TicketConverter {

    @Autowired
    private ModelMapper modelMapper;

    public TicketResponseDTO toResponseDTO(CustomerTicketEntity entity,
                                           int duplicateCount,
                                           KmzLocationEntity kmzLocation) {
        if (entity == null) {
            return null;
        }

        // ModelMapper tự động ánh xạ toàn bộ 20 thuộc tính trùng tên từ Entity sang DTO
        TicketResponseDTO dto = modelMapper.map(entity, TicketResponseDTO.class);

        // Gán số lần trùng lặp của Thuê bao phản ánh (x2, x3...)
        dto.setDuplicateCount(duplicateCount);
        dto.setIsPinned(Boolean.TRUE.equals(entity.getIsPinned()));
        applyTypeInfo(entity, dto);

        // Nối tọa độ từ bảng KMZ để tạo link mở Google Maps
        if (kmzLocation != null && kmzLocation.getLatitude() != null && kmzLocation.getLongitude() != null) {
            dto.setLatitude(kmzLocation.getLatitude());
            dto.setLongitude(kmzLocation.getLongitude());
            dto.setGoogleMapsUrl("https://www.google.com/maps?q="
                    + kmzLocation.getLatitude() + "," + kmzLocation.getLongitude());
        }

        return dto;
    }

    // 2. Dùng ModelMapper cập nhật trực tiếp từ UpdateTicketActionRequest vào CustomerTicketEntity
    // (Nhờ setSkipNullEnabled(true) trong ModelMapperConfig, trường nào khác null sẽ tự động ghi đè vào Entity)
    public void updateEntityFromRequest(UpdateTicketActionRequest request, CustomerTicketEntity entity) {
        if (request == null || entity == null) {
            return;
        }
        modelMapper.map(request, entity);
        entity.setUpdatedAt(LocalDateTime.now());
    }

    // 3. Dùng ModelMapper chuyển nhanh Entity -> DTO sau khi bấm nút thao tác trên thẻ
    public TicketResponseDTO toActionResponseDTO(CustomerTicketEntity entity) {
        if (entity == null) {
            return null;
        }
        TicketResponseDTO dto = modelMapper.map(entity, TicketResponseDTO.class);
        applyTypeInfo(entity, dto);
        return dto;
    }

    // Điền loại ca và danh sách thuộc tính (nhãn lấy từ khai báo trong TicketType) cho giao diện
    private void applyTypeInfo(CustomerTicketEntity entity, TicketResponseDTO dto) {
        TicketType type = entity.getTicketType() != null ? entity.getTicketType() : TicketType.PHAN_ANH;
        dto.setTicketType(type.name());
        dto.setTicketTypeLabel(type.getLabel());
        dto.setTicketGroup(type.getGroup().name());
        dto.setTicketTypeColor(type.getColor());

        List<TicketAttributeView> items = new ArrayList<>();
        Map<String, String> attrs = entity.getAttributes();
        if (attrs != null) {
            for (TicketAttribute a : type.getAttributes()) {
                String v = attrs.get(a.key());
                if (v != null && !v.isBlank()) {
                    items.add(new TicketAttributeView(a.label(), v, a.unit()));
                }
            }
        }
        dto.setAttributeItems(items);
    }

    // 4. Đọc dòng dữ liệu từ Sheet "chi tiêt" của file Excel (.xlsx) vào Entity
    public void mapExcelRowToEntity(Row row, CustomerTicketEntity entity, DataFormatter formatter) {
        entity.setAssignedStaff(getCellString(row, 0, formatter));                   // Cột A: Giao nhân viên
        entity.setArea(getCellString(row, 2, formatter));                            // Cột C: Khu vực
        entity.setCommuneCluster(getCellString(row, 3, formatter));                  // Cột D: Cụm xã
        entity.setCommune(getCellString(row, 4, formatter));                         // Cột E: Xã
        entity.setLocalStaffCode(getCellString(row, 5, formatter));                  // Cột F: Nhân viên địa bàn
        entity.setServiceName(getCellString(row, 7, formatter));                     // Cột H: Tên dịch vụ
        entity.setCustomerName(getCellString(row, 8, formatter));                    // Cột I: Tên KH
        entity.setContactPhone(normalizePhone(getCellString(row, 9, formatter)));    // Cột J: SĐT liên hệ KH
        entity.setInstallAddress(getCellString(row, 10, formatter));                 // Cột K: Địa chỉ lắp đặt
        entity.setReportPhone(normalizePhone(getCellString(row, 11, formatter)));    // Cột L: Số liên hệ nhập phản ánh
        entity.setCategory(getCellString(row, 13, formatter));                       // Cột N: Thể loại
        entity.setPriorityLevel(getCellString(row, 18, formatter));                  // Cột S: Mức ưu tiên SCKN
        entity.setTechStaffName(getCellString(row, 21, formatter));                  // Cột V: NV kỹ thuật được giao WO
        entity.setTechStaffPhone(normalizePhone(getCellString(row, 22, formatter))); // Cột W: SĐT của NV kỹ thuật
        entity.setComplaintContent(getCellString(row, 23, formatter));               // Cột X: Nội dung khiếu nại
        entity.setGnocProgress(getCellString(row, 25, formatter));                   // Cột Z: Tiến độ của GNOC
        entity.setSubscriberAccount(getCellString(row, 28, formatter));              // Cột AC: Thuê bao phản ánh
        entity.setCabinetCode(getCellString(row, 30, formatter).toUpperCase());      // Cột AE: Mã tủ
        entity.setUpdatedAt(LocalDateTime.now());
    }

    public String extractTicketCode(Row row, DataFormatter formatter) {
        return getCellString(row, 1, formatter); // Cột B: Mã phản ánh
    }

    public String getCellString(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            double num = cell.getNumericCellValue();
            return BigDecimal.valueOf(num).stripTrailingZeros().toPlainString().trim();
        }
        return cleanPlaceholder(formatter.formatCellValue(cell).trim());
    }

    // File nguồn hay có "#N/A" (tra cứu lỗi) hoặc chữ "NULL" ở ô không có dữ liệu: coi như ô trống
    private String cleanPlaceholder(String value) {
        if (value.equalsIgnoreCase("#N/A") || value.equalsIgnoreCase("NULL")) return "";
        return value;
    }

    public String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) return "";
        String cleaned = rawPhone.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty() || cleaned.matches("0+")) return "";
        if (cleaned.length() == 9 && !cleaned.startsWith("0")) {
            return "0" + cleaned;
        }
        return cleaned;
    }

}
