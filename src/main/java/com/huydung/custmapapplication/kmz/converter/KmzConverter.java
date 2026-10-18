package com.huydung.custmapapplication.kmz.converter;

import com.huydung.custmapapplication.kmz.dto.KmzLocationDTO;
import com.huydung.custmapapplication.kmz.entity.KmzLocationEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class KmzConverter {

    private final ModelMapper modelMapper;

    public KmzConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public KmzLocationDTO toDTO(KmzLocationEntity entity) {
        if (entity == null) {
            return null;
        }
        KmzLocationDTO dto = modelMapper.map(entity, KmzLocationDTO.class);

        // 2. Tạo link Google Maps từ tọa độ
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            dto.setGoogleMapsUrl("https://www.google.com/maps?q="
                    + dto.getLatitude() + "," + dto.getLongitude());
        }
        return dto;
    }

    public KmzLocationDTO fromKmlPlacemark(String name, String coordsText, String sourceFile) {
        if (name == null || name.isBlank() || coordsText == null || coordsText.isBlank()) {
            return null;
        }
        try {
            String firstPoint = coordsText.trim().split("\\s+")[0];
            String[] parts = firstPoint.split(",");
            if (parts.length >= 2) {
                double longitude = Double.parseDouble(parts[0].trim()); // KML lưu Kinh độ trước
                double latitude = Double.parseDouble(parts[1].trim());  // KML lưu Vĩ độ sau
                return new KmzLocationDTO(name.trim().toUpperCase(), latitude, longitude, sourceFile);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }
}
