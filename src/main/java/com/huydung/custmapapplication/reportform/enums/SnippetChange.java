package com.huydung.custmapapplication.reportform.enums;

/**
 * Loại yêu cầu đang chờ admin duyệt. EDIT / DELETE được lưu ở cột pending_change của mẫu ĐÃ duyệt
 * (mẫu cũ vẫn dùng bình thường trong lúc chờ). ADD KHÔNG lưu ở cột này: nó chỉ dùng để hiển thị loại yêu cầu
 * của mẫu mới đang ở trạng thái PENDING.
 */
public enum SnippetChange {
    ADD("Thêm mới", "pill-add",
            "Mẫu này đang chờ admin duyệt (còn đọng), hiện chỉ bạn và admin thấy."),
    EDIT("Sửa", "pill-edit",
            "Có yêu cầu sửa đang chờ admin duyệt. Trong lúc chờ vẫn dùng nội dung cũ."),
    DELETE("Xóa", "pill-del",
            "Có yêu cầu xóa mẫu này đang chờ admin duyệt. Trong lúc chờ vẫn dùng bình thường.");

    private final String label;
    private final String pillClass;
    private final String waitingNote;

    SnippetChange(String label, String pillClass, String waitingNote) {
        this.label = label;
        this.pillClass = pillClass;
        this.waitingNote = waitingNote;
    }

    public String getLabel() { return label; }

    public String getPillClass() { return pillClass; }

    public String getWaitingNote() { return waitingNote; }

    public boolean is(String code) {
        return name().equals(code);
    }
}
