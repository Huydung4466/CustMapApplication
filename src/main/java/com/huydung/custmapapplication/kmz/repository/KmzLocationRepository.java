package com.huydung.custmapapplication.kmz.repository;

import com.huydung.custmapapplication.kmz.entity.KmzLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KmzLocationRepository extends JpaRepository<KmzLocationEntity, Long>, CustomKmzRepository {

    Optional<KmzLocationEntity> findByCabinetCodeIgnoreCase(String cabinetCode);
}
