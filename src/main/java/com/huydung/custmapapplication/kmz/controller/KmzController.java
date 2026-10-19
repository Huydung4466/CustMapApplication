package com.huydung.custmapapplication.kmz.controller;

import com.huydung.custmapapplication.kmz.service.KmzService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Chỉ chứa các API HIỂN THỊ dữ liệu KMZ (tra cứu tọa độ, đếm).
 * API import file KMZ (thay đổi dữ liệu) nằm ở package kmz.api.
 */
@RestController
@RequestMapping("/api/kmz")
public class KmzController {

    private final KmzService kmzService;

    public KmzController(KmzService kmzService) {
        this.kmzService = kmzService;
    }

    // 1. API Tra cứu tọa độ & link Google Maps theo Mã tủ (VD: /api/kmz/location?cabinetCode=DLK0010-DN01-SN01)
    @GetMapping("/location")
    public ResponseEntity<?> getLocationByCabinetCode(@RequestParam("cabinetCode") String cabinetCode) {
        return kmzService.getLocationByCabinetCode(cabinetCode)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("message", "Không tìm thấy tọa độ cho mã tủ: " + cabinetCode)));
    }

    // 2. API Kiểm tra tổng số mã tủ hiện có trong Database
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTotalCount() {
        return ResponseEntity.ok(Map.of("totalLocations", kmzService.countTotalLocations()));
    }
}
