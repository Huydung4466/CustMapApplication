package com.huydung.custmapapplication.kmz.api;

import com.huydung.custmapapplication.kmz.dto.KmzImportResultDTO;
import com.huydung.custmapapplication.kmz.service.KmzService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** API THAY ĐỔI dữ liệu KMZ: import file .kmz / .kml vào Database. */
@RestController
@RequestMapping("/api/kmz")
public class KmzApiController {

    private final KmzService kmzService;

    public KmzApiController(KmzService kmzService) {
        this.kmzService = kmzService;
    }

    // Upload và bóc tách file .kmz (hoặc .kml) vào Database
    @PostMapping("/import")
    public ResponseEntity<KmzImportResultDTO> importKmz(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(kmzService.importKmzFile(file));
    }
}
