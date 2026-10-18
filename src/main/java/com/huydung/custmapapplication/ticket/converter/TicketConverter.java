package com.huydung.custmapapplication.ticket.converter;

import com.huydung.custmapapplication.kmz.entity.KmzLocationEntity;
import com.huydung.custmapapplication.ticket.dto.TicketResponseDTO;
import com.huydung.custmapapplication.ticket.dto.UpdateTicketActionRequest;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import org.apache.poi.ss.usermodel.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        return modelMapper.map(entity, TicketResponseDTO.class);
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
        return formatter.formatCellValue(cell).trim();
    }

    public String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) return "";
        String cleaned = rawPhone.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) return "";
        if (cleaned.length() == 9 && !cleaned.startsWith("0")) {
            return "0" + cleaned;
        }
        return cleaned;
    }

}
