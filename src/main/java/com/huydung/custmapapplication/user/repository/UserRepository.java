package com.huydung.custmapapplication.user.repository;

import com.huydung.custmapapplication.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long>, CustomUserRepository {

    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);
}
