package com.huydung.custmapapplication.ticket.duplicate;

import com.huydung.custmapapplication.ticket.enums.TicketType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Quy tắc "trùng account" dùng chung cho Phản ánh và Hạ tầng (một nơi duy nhất).
 * <ul>
 *   <li>Hai ca trùng nhau khi CÙNG loại ca và CÙNG account (thuê bao), không phân biệt hoa thường / khoảng trắng thừa.</li>
 *   <li>Nhóm nào được gộp thành một thẻ do {@code TicketGroup#isCollapseDuplicateAccounts()} khai báo;
 *       nhóm không gộp (Phản ánh) vẫn hiện đủ từng ca, chỉ gắn nhãn "Trùng xN".</li>
 * </ul>
 */
public final class DuplicateRule {

    private DuplicateRule() {
    }

    /** Các loại ca bị gộp khi trùng account. */
    public static List<TicketType> collapsingTypes() {
        return Arrays.stream(TicketType.values())
                .filter(t -> t.getGroup().isCollapseDuplicateAccounts())
                .toList();
    }

    /** Khóa nhóm trùng: "LOẠI|account". Ca cũ chưa có loại coi như PHAN_ANH. */
    public static String key(TicketType type, String account) {
        TicketType t = type != null ? type : TicketType.PHAN_ANH;
        return t.name() + "|" + (account == null ? "" : account.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean hasAccount(String account) {
        return account != null && !account.trim().isEmpty();
    }
}
