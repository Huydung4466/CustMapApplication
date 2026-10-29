package com.huydung.custmapapplication.ticket.dto;

import com.huydung.custmapapplication.ticket.enums.ArchiveReason;
import com.huydung.custmapapplication.ticket.enums.HistoryTab;
import com.huydung.custmapapplication.ticket.enums.StatusContext;
import com.huydung.custmapapplication.ticket.enums.TicketListFilter;
import com.huydung.custmapapplication.ticket.enums.TicketStatus;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Danh mục dùng chung cho giao diện, sinh ra từ các enum ({@link TicketStatus}, {@link ArchiveReason},
 * {@link HistoryTab}, {@link StatusContext}) để JSP / JS không phải khai báo cứng trạng thái và lý do lưu trữ.
 */
public record TicketMetaDTO(List<StatusInfo> statuses,
                            Map<String, List<StatusOption>> statusOptions,
                            List<ArchiveReasonInfo> archiveReasons,
                            List<HistoryTabInfo> historyTabs,
                            List<ListFilterInfo> listFilters) {

    /** Một trạng thái: nhãn ngắn, biểu tượng, class CSS, có phải ca đã hẹn không, có chuyển vào Lịch sử không. */
    public record StatusInfo(String code, String label, String icon, String badgeClass,
                             boolean appointed, boolean archives, String archiveReason) {
    }

    /** Một lựa chọn trong ô "Trạng thái" của hộp cập nhật (theo từng màn hình). */
    public record StatusOption(String code, String label) {
    }

    public record ArchiveReasonInfo(String code, String label, String icon, String badgeClass, String tab) {
    }

    /** Một lựa chọn trong ô lọc "Trạng thái" của dashboard (xem TicketListFilter). */
    public record ListFilterInfo(String code, String label) {
    }

    public record HistoryTabInfo(String code, String label, String icon, String path, String emptyText) {
    }

    public static TicketMetaDTO build() {
        List<StatusInfo> statuses = Arrays.stream(TicketStatus.values())
                .map(s -> new StatusInfo(s.name(), s.getLabel(), s.getIcon(), s.getBadgeClass(),
                        s.isAppointed(), s.archives(), s.archives() ? s.getArchiveReason().name() : null))
                .toList();

        Map<String, List<StatusOption>> options = new LinkedHashMap<>();
        for (StatusContext c : StatusContext.values()) {
            options.put(c.name(), c.getStatuses().stream()
                    .map(s -> new StatusOption(s.name(), c.optionLabel(s)))
                    .toList());
        }

        List<ArchiveReasonInfo> reasons = Arrays.stream(ArchiveReason.values())
                .map(r -> new ArchiveReasonInfo(r.name(), r.getLabel(), r.getIcon(), r.getBadgeClass(), r.getTab().name()))
                .toList();

        List<HistoryTabInfo> tabs = Arrays.stream(HistoryTab.values())
                .map(t -> new HistoryTabInfo(t.name(), t.getLabel(), t.getIcon(), t.getPath(), t.getEmptyText()))
                .toList();

        List<ListFilterInfo> listFilters = Arrays.stream(TicketListFilter.values())
                .map(f -> new ListFilterInfo(f.name(), f.getLabel()))
                .toList();

        return new TicketMetaDTO(statuses, options, reasons, tabs, listFilters);
    }
}
