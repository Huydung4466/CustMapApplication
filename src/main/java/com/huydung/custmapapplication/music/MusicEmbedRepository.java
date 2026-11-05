package com.huydung.custmapapplication.music;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MusicEmbedRepository extends JpaRepository<MusicEmbedEntity, Long> {

    Optional<MusicEmbedEntity> findByProviderAndRef(String provider, String ref);

    List<MusicEmbedEntity> findAllByOrderByIdDesc();
}
