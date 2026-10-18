package com.huydung.custmapapplication.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Lớp cha chung cho các DTO trả về từng bản ghi (khớp với BaseEntity)
@Getter
@Setter
public class BaseDTO {
    private Long id;
    private LocalDateTime updatedAt;
}
