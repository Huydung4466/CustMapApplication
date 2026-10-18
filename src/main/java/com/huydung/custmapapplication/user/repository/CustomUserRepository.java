package com.huydung.custmapapplication.user.repository;

import com.huydung.custmapapplication.user.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface CustomUserRepository {
    Optional<UserEntity> findByNormalizedFullName(String fullName);

    List<UserEntity> searchUsersCustom(String keyword, Integer status);
}
