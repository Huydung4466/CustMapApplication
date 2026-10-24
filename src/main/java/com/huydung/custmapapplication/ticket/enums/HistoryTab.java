package com.huydung.custmapapplication.ticket.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Hai trang của mục Lịch sử. Mỗi trang gom một nhóm lý do lưu trữ (xem {@link ArchiveReason#getTab()}).
 * Thêm trang mới: thêm giá trị ở đây, gán cho lý do lưu trữ tương ứng, rồi thêm @GetMapping cho đường dẫn mới
 * trong TicketPageController; giao diện tự có thêm nút chuyển trang.
 */
public enum HistoryTab {

    /** Công việc đã hoàn thành (bấm "Đã xử lý"). Ca cũ chưa ghi lý do lưu trữ cũng tính vào đây. */
    COMPLETED("Đã hoàn thành", "fa-solid fa-circle-check", "/history", true,
            "Chưa có ca nào hoàn thành"),

    /** Ca không liên lạc được hoặc các vấn đề khác (không còn trong file GNOC...). */
    ISSUES("Không liên lạc được & khác", "fa-solid fa-phone-slash", "/history/issues", false,
            "Chưa có ca nào không liên lạc được hoặc có vấn đề khác");

    private final String label;
    private final String icon;        // class Font Awesome
    private final String path;        // đường dẫn trang (không gồm context path)
    private final boolean includesUnknownReason; // có lấy cả ca lưu trữ chưa ghi lý do (dữ liệu cũ) hay không
    private final String emptyText;

    HistoryTab(String label, String icon, String path, boolean includesUnknownReason, String emptyText) {
        this.label = label;
        this.icon = icon;
        this.path = path;
        this.includesUnknownReason = includesUnknownReason;
        this.emptyText = emptyText;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getPath() {
        return path;
    }

    public boolean isIncludesUnknownReason() {
        return includesUnknownReason;
    }

    public String getEmptyText() {
        return emptyText;
    }

    /** Các lý do lưu trữ thuộc trang này. */
    public List<ArchiveReason> reasons() {
        return Arrays.stream(ArchiveReason.values()).filter(r -> r.getTab() == this).toList();
    }

    /** Mã (tên) các lý do lưu trữ thuộc trang này, dùng làm tham số truy vấn. */
    public List<String> reasonCodes() {
        return reasons().stream().map(Enum::name).toList();
    }

    /** Trả về null nếu chuỗi rỗng hoặc không khớp trang nào. */
    public static HistoryTab fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (HistoryTab t : values()) {
            if (t.name().equalsIgnoreCase(code.trim())) return t;
        }
        return null;
    }
}
