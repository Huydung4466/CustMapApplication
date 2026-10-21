package com.huydung.custmapapplication.export.dto;

import java.time.LocalDateTime;

/** Thông tin một file Excel đã xuất (lưu tạm trên server, tự xóa khi hết hạn). */
public class ExportFileDTO {

    private String id;
    private String fileName;        // Tên file khi tải về
    private long sizeBytes;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private long secondsLeft;       // Số giây còn lại trước khi bị xóa (tính theo đồng hồ server)
    private int ttlMinutes;         // Thời gian lưu tối đa (phút), dùng cho dòng thông báo "tự xóa sau ... phút"

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public long getSecondsLeft() { return secondsLeft; }
    public void setSecondsLeft(long secondsLeft) { this.secondsLeft = secondsLeft; }
    public int getTtlMinutes() { return ttlMinutes; }
    public void setTtlMinutes(int ttlMinutes) { this.ttlMinutes = ttlMinutes; }
}
