package com.huydung.custmapapplication.kmz.repository.impl;

import com.huydung.custmapapplication.kmz.dto.KmzLocationDTO;
import com.huydung.custmapapplication.kmz.entity.KmzLocationEntity;
import com.huydung.custmapapplication.kmz.repository.CustomKmzRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class CustomKmzRepositoryImpl implements CustomKmzRepository {

    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public CustomKmzRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int batchUpsertLocations(List<KmzLocationDTO> locations) {
        if (locations == null || locations.isEmpty()) {
            return 0;
        }

        String sql = """
                INSERT INTO kmz_locations (cabinet_code, latitude, longitude, source_file, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (cabinet_code)
                DO UPDATE SET
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude,
                    source_file = EXCLUDED.source_file,
                    updated_at = EXCLUDED.updated_at
                """;

        int batchSize = 500;
        int totalProcessed = 0;
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        for (int i = 0; i < locations.size(); i += batchSize) {
            List<KmzLocationDTO> batchList = locations.subList(i, Math.min(i + batchSize, locations.size()));

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int index) throws SQLException {
                    KmzLocationDTO item = batchList.get(index);
                    ps.setString(1, item.getCabinetCode().trim().toUpperCase());
                    ps.setDouble(2, item.getLatitude());
                    ps.setDouble(3, item.getLongitude());
                    ps.setString(4, item.getSourceFile());
                    ps.setTimestamp(5, now);
                }

                @Override
                public int getBatchSize() {
                    return batchList.size();
                }
            });

            totalProcessed += batchList.size();
        }

        return totalProcessed;
    }

    @Override
    public Map<String, KmzLocationEntity> findMapByCabinetCodes(Collection<String> cabinetCodes) {
        if (cabinetCodes == null || cabinetCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> normalizedCodes = cabinetCodes.stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase())
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toSet());

        if (normalizedCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT k FROM KmzLocationEntity k WHERE UPPER(k.cabinetCode) IN :codes";
        List<KmzLocationEntity> entities = entityManager.createQuery(jpql, KmzLocationEntity.class)
                .setParameter("codes", normalizedCodes)
                .getResultList();

        return entities.stream().collect(Collectors.toMap(
                k -> k.getCabinetCode().trim().toUpperCase(),
                k -> k,
                (existing, replacement) -> existing
        ));
    }
}
