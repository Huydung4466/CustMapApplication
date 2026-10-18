package com.huydung.custmapapplication.kmz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KmzLocationDTO {
    private String cabinetCode;
    private Double latitude;
    private Double longitude;
    private String sourceFile;
    private String googleMapsUrl;

    public KmzLocationDTO(String cabinetCode, Double latitude, Double longitude, String sourceFile) {
        this.cabinetCode = cabinetCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sourceFile = sourceFile;
        if (latitude != null && longitude != null) {
            this.googleMapsUrl = "https://www.google.com/maps?q=" + latitude + "," + longitude;
        }
    }
}
