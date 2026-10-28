package com.huydung.custmapapplication.reportform.enums;

/** Các thao tác được ghi vào lịch sử mẫu, kèm trạng thái của mẫu SAU thao tác đó. */
public enum SnippetAction {
    CREATE("Gửi mẫu mới", "fa-solid fa-plus", SnippetState.PENDING),
    CREATE_APPROVED("Admin thêm mẫu", "fa-solid fa-plus", SnippetState.APPROVED),
    EDIT_NEW("Sửa mẫu chờ duyệt", "fa-regular fa-pen-to-square", SnippetState.PENDING),
    RESUBMIT("Sửa và gửi lại", "fa-solid fa-rotate-right", SnippetState.PENDING),
    REQUEST_EDIT("Đề nghị sửa", "fa-regular fa-pen-to-square", SnippetState.PENDING),
    REQUEST_DELETE("Đề nghị xóa", "fa-regular fa-trash-can", SnippetState.PENDING_DELETE),
    EDIT_DIRECT("Admin sửa trực tiếp", "fa-solid fa-pen", SnippetState.APPROVED),
    DELETE_DIRECT("Xóa mẫu", "fa-solid fa-trash-can", SnippetState.DELETED),
    APPROVE_ADD("Duyệt mẫu mới", "fa-solid fa-check", SnippetState.APPROVED),
    APPROVE_EDIT("Duyệt yêu cầu sửa", "fa-solid fa-check", SnippetState.APPROVED),
    APPROVE_DELETE("Duyệt yêu cầu xóa", "fa-solid fa-check", SnippetState.DELETED),
    REJECT_ADD("Từ chối mẫu mới", "fa-solid fa-xmark", SnippetState.REJECTED),
    REJECT_EDIT("Từ chối yêu cầu sửa", "fa-solid fa-xmark", SnippetState.APPROVED),
    REJECT_DELETE("Từ chối yêu cầu xóa", "fa-solid fa-xmark", SnippetState.APPROVED);

    private final String label;
    private final String icon;
    private final SnippetState resultState;

    SnippetAction(String label, String icon, SnippetState resultState) {
        this.label = label;
        this.icon = icon;
        this.resultState = resultState;
    }

    public String getLabel() { return label; }

    public String getIcon() { return icon; }

    public SnippetState getResultState() { return resultState; }
}
