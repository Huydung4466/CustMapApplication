package com.huydung.custmapapplication.common.exception;

// Ném ra khi import sắp lưu trữ quá nhiều ca và người dùng chưa xác nhận
public class ImportConfirmationRequiredException extends RuntimeException {

    private final int archiveCount;
    private final int activeCount;

    public ImportConfirmationRequiredException(int archiveCount, int activeCount) {
        super("File này sẽ chuyển " + archiveCount + "/" + activeCount
                + " ca đang mở sang Lịch sử vì không còn xuất hiện trong file. Bạn có chắc đây là đúng file không?");
        this.archiveCount = archiveCount;
        this.activeCount = activeCount;
    }

    public int getArchiveCount() { return archiveCount; }

    public int getActiveCount() { return activeCount; }
}
