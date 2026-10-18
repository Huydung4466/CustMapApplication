package com.huydung.custmapapplication.user.entity;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RoleEntity extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code; // VD: ROLE_ADMIN, ROLE_STAFF

    @Column(name = "name", nullable = false, length = 100)
    private String name; // VD: Quản trị viên, Nhân viên địa bàn

    public RoleEntity() {
    }

    public RoleEntity(String code, String name) {
        this.code = code;
        this.name = name;
    }

    // Getters & Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
