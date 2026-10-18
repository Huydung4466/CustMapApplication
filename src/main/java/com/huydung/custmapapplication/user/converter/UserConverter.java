package com.huydung.custmapapplication.user.converter;

import com.huydung.custmapapplication.user.dto.CreateUserRequest;
import com.huydung.custmapapplication.user.dto.UserDTO;
import com.huydung.custmapapplication.user.entity.RoleEntity;
import com.huydung.custmapapplication.user.entity.UserEntity;
import org.springframework.stereotype.Component;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserConverter {

    private final ModelMapper modelMapper;

    public UserConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public UserDTO toDTO(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        // 1. ModelMapper tự động map: id, username, fullName, phone, status
        UserDTO dto = modelMapper.map(entity, UserDTO.class);

        // 2. Map danh sách RoleEntity sang List<String> mã quyền (ROLE_ADMIN, ROLE_STAFF)
        if (entity.getRoles() != null) {
            List<String> roleCodes = entity.getRoles().stream()
                    .map(RoleEntity::getCode)
                    .collect(Collectors.toList());
            dto.setRoles(roleCodes);
        } else {
            dto.setRoles(Collections.emptyList());
        }

        return dto;
    }

    public UserEntity toEntity(CreateUserRequest request) {
        if (request == null) {
            return null;
        }
        UserEntity entity = modelMapper.map(request, UserEntity.class);
        if (entity.getUsername() != null) {
            entity.setUsername(entity.getUsername().trim());
        }
        if (entity.getFullName() != null) {
            entity.setFullName(entity.getFullName().trim());
        }
        entity.setStatus(1);
        return entity;
    }
}
