package com.huydung.custmapapplication.user.repository.impl;

import com.huydung.custmapapplication.user.entity.UserEntity;
import com.huydung.custmapapplication.user.repository.CustomUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<UserEntity> findByNormalizedFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return Optional.empty();
        }
        String jpql = "SELECT u FROM UserEntity u WHERE UPPER(TRIM(u.fullName)) = UPPER(TRIM(:fullName))";
        List<UserEntity> results = entityManager.createQuery(jpql, UserEntity.class)
                .setParameter("fullName", fullName)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<UserEntity> searchUsersCustom(String keyword, Integer status) {
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            jpql.append("AND (LOWER(u.username) LIKE LOWER(:kw) OR LOWER(u.fullName) LIKE LOWER(:kw)) ");
            params.put("kw", "%" + keyword.trim() + "%");
        }
        if (status != null) {
            jpql.append("AND u.status = :status ");
            params.put("status", status);
        }
        jpql.append("ORDER BY u.fullName ASC");

        TypedQuery<UserEntity> query = entityManager.createQuery(jpql.toString(), UserEntity.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }
}
