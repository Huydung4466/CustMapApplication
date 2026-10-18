package com.huydung.custmapapplication.kmz.repository;

import com.huydung.custmapapplication.kmz.dto.KmzLocationDTO;
import com.huydung.custmapapplication.kmz.entity.KmzLocationEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CustomKmzRepository {
    // Lưu hàng loạt điểm tọa độ KMZ bằng cơ chế Batch Upsert của PostgreSQL (nhanh gấp hàng chục lần save thường)
    int batchUpsertLocations(List<KmzLocationDTO> locations);

    // Tra cứu danh sách tọa độ theo danh sách mã tủ (Phục vụ nối dữ liệu với file Excel)
    Map<String, KmzLocationEntity> findMapByCabinetCodes(Collection<String> cabinetCodes);
}
