package com.huydung.custmapapplication.ticket.dto;

import com.huydung.custmapapplication.common.dto.BaseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDTO extends BaseDTO {
    private String ticketCode;

    // Loại ca và thuộc tính riêng đã định dạng sẵn để hiển thị (xem TicketType)
    private String ticketType;       // PHAN_ANH | PORT_KEM | ...
    private String ticketTypeLabel;
    private String ticketGroup;      // PHAN_ANH | HA_TANG
    private String ticketTypeColor;  // màu badge Bootstrap
    private List<TicketAttributeView> attributeItems;
    private String assignedStaff;
    private String area;
    private String communeCluster;
    private String commune;
    private String localStaffCode;

    private String customerName;
    private String subscriberAccount;
    private int duplicateCount; // Nếu >= 2 sẽ hiển thị "Trùng x2", "Trùng x3"... trên giao diện

    private String contactPhone; // SĐT 1: Số thuê bao khách
    private String reportPhone;  // SĐT 2: Số nhập báo cáo

    private String installAddress;
    private String complaintContent;

    private String techStaffName; // Hiển thị mục "Phụ trách: ..."
    private String techStaffPhone;

    private String priorityLevel;
    private String gnocProgress;

    // Thông tin vị trí Tủ kết nối từ bảng KMZ
    private String cabinetCode;
    private Double latitude;
    private Double longitude;
    private String googleMapsUrl; // Link mở trực tiếp Google Maps trên điện thoại

    // Khoảng cách (km) từ vị trí người dùng tới tủ, chỉ có khi sắp xếp theo "Gần tôi"
    private Double distanceKm;

    // Trạng thái các nút bấm trên thẻ
    private String appStatus;
    private String appointmentTime;
    private LocalDateTime appointmentStart;
    private LocalDateTime appointmentEnd;
    private String callNote;
    private Boolean isPinned;

    // Số ảnh hiện trường đã chụp của ca (hiện trên nút camera)
    private Integer photoCount;

    // Lưu trữ / lịch sử
    private LocalDateTime archivedAt;
    private String archiveReason; // DONE (bấm Đã xử lý) | KLL (không liên lạc được) | NOT_IN_IMPORT (không còn trong file import mới)
}
