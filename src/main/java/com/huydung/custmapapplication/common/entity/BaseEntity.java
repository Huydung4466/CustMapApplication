package com.huydung.custmapapplication.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lớp cha chung cho mọi entity: khóa chính + thời điểm cập nhật gần nhất.
 * updated_at tự được gán mỗi khi thêm mới hoặc cập nhật qua JPA.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
