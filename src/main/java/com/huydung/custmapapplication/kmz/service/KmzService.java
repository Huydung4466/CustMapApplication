package com.huydung.custmapapplication.kmz.service;

import com.huydung.custmapapplication.kmz.dto.KmzImportResultDTO;
import com.huydung.custmapapplication.kmz.dto.KmzLocationDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface KmzService {
    KmzImportResultDTO importKmzFile(MultipartFile file);
    Optional<KmzLocationDTO> getLocationByCabinetCode(String cabinetCode);
    long countTotalLocations();
}
