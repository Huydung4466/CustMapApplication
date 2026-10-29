package com.huydung.custmapapplication.ticket.converter;

import com.huydung.custmapapplication.constant.ComplaintExcelConstant;
import com.huydung.custmapapplication.kmz.entity.KmzLocationEntity;
import com.huydung.custmapapplication.ticket.dto.TicketAttributeView;
import com.huydung.custmapapplication.ticket.dto.TicketResponseDTO;
import com.huydung.custmapapplication.ticket.dto.UpdateTicketActionRequest;
import com.huydung.custmapapplication.ticket.entity.CustomerTicketEntity;
import com.huydung.custmapapplication.ticket.type.TicketAttribute;
import com.huydung.custmapapplication.ticket.enums.ReportStatus;
import com.huydung.custmapapplication.ticket.enums.TicketType;
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

        // Trạng thái nộp báo cáo (null trong DB = chưa nộp)
        ReportStatus report = ReportStatus.resolve(entity.getReportStatus());
        dto.setReportStatus(report.name());
        dto.setReportStatusLabel(report.getLabel());
        dto.setReportStatusIcon(report.getIcon());
        dto.setReportStatusBadge(report.getBadgeClass());

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

    // 4. Đọc dòng dữ liệu từ sheet chi tiết của file phản ánh (.xlsx) vào Entity.
    //    Vị trí từng cột khai báo trong ComplaintExcelConstant
    public void mapExcelRowToEntity(Row row, CustomerTicketEntity entity, DataFormatter formatter) {
        entity.setAssignedStaff(getCellString(row, ComplaintExcelConstant.COL_ASSIGNED_STAFF, formatter));
        entity.setArea(getCellString(row, ComplaintExcelConstant.COL_AREA, formatter));
        entity.setCommuneCluster(getCellString(row, ComplaintExcelConstant.COL_COMMUNE_CLUSTER, formatter));
        entity.setCommune(getCellString(row, ComplaintExcelConstant.COL_COMMUNE, formatter));
        entity.setLocalStaffCode(getCellString(row, ComplaintExcelConstant.COL_LOCAL_STAFF_CODE, formatter));
        entity.setServiceName(getCellString(row, ComplaintExcelConstant.COL_SERVICE_NAME, formatter));
        entity.setCustomerName(getCellString(row, ComplaintExcelConstant.COL_CUSTOMER_NAME, formatter));
        entity.setContactPhone(normalizePhone(getCellString(row, ComplaintExcelConstant.COL_CONTACT_PHONE, formatter)));
        entity.setInstallAddress(getCellString(row, ComplaintExcelConstant.COL_INSTALL_ADDRESS, formatter));
        entity.setReportPhone(normalizePhone(getCellString(row, ComplaintExcelConstant.COL_REPORT_PHONE, formatter)));
        entity.setCategory(getCellString(row, ComplaintExcelConstant.COL_CATEGORY, formatter));
        entity.setPriorityLevel(getCellString(row, ComplaintExcelConstant.COL_PRIORITY_LEVEL, formatter));
        entity.setTechStaffName(getCellString(row, ComplaintExcelConstant.COL_TECH_STAFF_NAME, formatter));
        entity.setTechStaffPhone(normalizePhone(getCellString(row, ComplaintExcelConstant.COL_TECH_STAFF_PHONE, formatter)));
        entity.setComplaintContent(getCellString(row, ComplaintExcelConstant.COL_COMPLAINT_CONTENT, formatter));
        entity.setGnocProgress(getCellString(row, ComplaintExcelConstant.COL_GNOC_PROGRESS, formatter));
        entity.setSubscriberAccount(getCellString(row, ComplaintExcelConstant.COL_SUBSCRIBER_ACCOUNT, formatter));
        entity.setCabinetCode(getCellString(row, ComplaintExcelConstant.COL_CABINET_CODE, formatter).toUpperCase());
        entity.setUpdatedAt(LocalDateTime.now());
    }

    public String extractTicketCode(Row row, DataFormatter formatter) {
        return getCellString(row, ComplaintExcelConstant.COL_REPORT_CODE, formatter);
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
