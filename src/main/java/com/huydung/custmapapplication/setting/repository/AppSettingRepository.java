package com.huydung.custmapapplication.setting.repository;

import com.huydung.custmapapplication.setting.entity.AppSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSettingEntity, Long> {

    Optional<AppSettingEntity> findBySettingKey(String settingKey);

    List<AppSettingEntity> findBySettingKeyStartingWith(String prefix);
}
