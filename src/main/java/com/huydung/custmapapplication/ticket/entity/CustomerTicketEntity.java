package com.huydung.custmapapplication.ticket.entity;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.huydung.custmapapplication.ticket.enums.TicketStatus;
import com.huydung.custmapapplication.ticket.enums.TicketType;
import com.huydung.custmapapplication.ticket.type.TicketTypeConverter;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "customer_tickets", indexes = {
        @Index(name = "idx_ticket_code", columnList = "ticket_code", unique = true),
        @Index(name = "idx_ticket_assigned_staff", columnList = "assigned_staff"),
        @Index(name = "idx_ticket_subscriber", columnList = "subscriber_account"),
        @Index(name = "idx_ticket_cabinet", columnList = "cabinet_code"),
        @Index(name = "idx_ticket_archived", columnList = "archived_at"),
        @Index(name = "idx_ticket_type", columnList = "ticket_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTicketEntity extends BaseEntity {

    @Column(name = "ticket_code", unique = true, nullable = false, length = 100)
    private String ticketCode;

    // Loại ca (xem TicketType). Lưu dạng chuỗi qua converter, KHÔNG dùng @Enumerated để thêm loại mới không dính check constraint.
    @Convert(converter = TicketTypeConverter.class)
    @Column(name = "ticket_type", length = 30)
    private TicketType ticketType = TicketType.PHAN_ANH;

    // Thuộc tính riêng của từng loại (công suất thu, số HomeWifi thiếu...), key khai báo trong TicketType
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, String> attributes;

    @Column(name = "assigned_staff", columnDefinition = "TEXT")
    private String assignedStaff;

    @Column(name = "area", columnDefinition = "TEXT")
    private String area;

    @Column(name = "commune_cluster", columnDefinition = "TEXT")
    private String communeCluster;

    @Column(name = "commune", columnDefinition = "TEXT")
    private String commune;

    @Column(name = "local_staff_code", columnDefinition = "TEXT")
    private String localStaffCode;

    @Column(name = "service_name", columnDefinition = "TEXT")
    private String serviceName;

    @Column(name = "customer_name", columnDefinition = "TEXT")
    private String customerName;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "install_address", columnDefinition = "TEXT")
    private String installAddress;

    @Column(name = "report_phone", length = 50)
    private String reportPhone;

    @Column(name = "category", columnDefinition = "TEXT")
    private String category;

    @Column(name = "priority_level", columnDefinition = "TEXT")
    private String priorityLevel;

    @Column(name = "tech_staff_name", columnDefinition = "TEXT")
    private String techStaffName;

    @Column(name = "tech_staff_phone", length = 50)
    private String techStaffPhone;

    @Column(name = "complaint_content", columnDefinition = "TEXT")
    private String complaintContent;

    // Đổi từ length = 100 sang TEXT vì có 23 dòng trong Excel dài tới 269 ký tự
    @Column(name = "gnoc_progress", columnDefinition = "TEXT")
    private String gnocProgress;

    @Column(name = "subscriber_account", length = 255)
    private String subscriberAccount;

    @Column(name = "cabinet_code", length = 255)
    private String cabinetCode;

    // Trạng thái xử lý: tên một giá trị của TicketStatus (lưu dạng chuỗi nên thêm trạng thái mới không cần đổi DB)
    @Column(name = "app_status", length = 50)
    private String appStatus = TicketStatus.DANG_XU_LY.name();

    @Column(name = "appointment_time", columnDefinition = "TEXT")
    private String appointmentTime;

    // Giờ hẹn dạng mốc thời gian thật (appointment_time ở trên là chuỗi hiển thị)
    @Column(name = "appointment_start")
    private LocalDateTime appointmentStart;

    @Column(name = "appointment_end")
    private LocalDateTime appointmentEnd;

    @Column(name = "call_note", columnDefinition = "TEXT")
    private String callNote;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    // Lưu trữ: archived_at = null nghĩa là ca đang mở; có giá trị nghĩa là ca nằm trong Lịch sử
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    // Tên một giá trị của ArchiveReason: DONE (bấm Đã xử lý) | KLL (không liên lạc được) | NOT_IN_IMPORT (không còn trong file import mới).
    // Lý do quyết định ca hiện ở trang Lịch sử nào (xem HistoryTab)
    @Column(name = "archive_reason", length = 20)
    private String archiveReason;

    // Nộp báo cáo (Google Form): tên một giá trị của ReportStatus; null = chưa nộp
    @Column(name = "report_status", length = 20)
    private String reportStatus;

    // Thời điểm đổi trạng thái nộp báo cáo gần nhất (null khi chưa nộp)
    @Column(name = "report_updated_at")
    private LocalDateTime reportUpdatedAt;
}
