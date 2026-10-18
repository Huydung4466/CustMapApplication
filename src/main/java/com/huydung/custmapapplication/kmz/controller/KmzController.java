package com.huydung.custmapapplication.kmz.controller;

import com.huydung.custmapapplication.kmz.dto.KmzImportResultDTO;
import com.huydung.custmapapplication.kmz.service.KmzService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/kmz")
public class KmzController {

    private final KmzService kmzService;

    public KmzController(KmzService kmzService) {
        this.kmzService = kmzService;
    }

    // 1. API Upload và bóc tách file .kmz vào Database
    @PostMapping("/import")
    public ResponseEntity<KmzImportResultDTO> importKmz(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(kmzService.importKmzFile(file));
    }

    // 2. API Tra cứu tọa độ & link Google Maps theo Mã tủ (VD: /api/kmz/location?cabinetCode=DLK0010-DN01-SN01)
    @GetMapping("/location")
    public ResponseEntity<?> getLocationByCabinetCode(@RequestParam("cabinetCode") String cabinetCode) {
        return kmzService.getLocationByCabinetCode(cabinetCode)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("message", "Không tìm thấy tọa độ cho mã tủ: " + cabinetCode)));
    }

    // 3. API Kiểm tra tổng số mã tủ hiện có trong Database
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTotalCount() {
        return ResponseEntity.ok(Map.of("totalLocations", kmzService.countTotalLocations()));
    }
}
