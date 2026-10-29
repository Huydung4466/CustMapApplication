package com.huydung.custmapapplication.photo.dto;

/** Một người đã chụp ảnh và số ảnh của họ (để lọc theo người chụp). */
public record PhotoOwnerDTO(Long userId, String fullName, String username, long count) {
}
