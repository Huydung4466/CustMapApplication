package com.huydung.custmapapplication.user.repository;

import com.huydung.custmapapplication.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long>, CustomUserRepository {

    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("select r.code from UserEntity u join u.roles r where u.id = :id and u.status = 1")
    List<String> findActiveRoleCodes(@Param("id") Long id);
}
