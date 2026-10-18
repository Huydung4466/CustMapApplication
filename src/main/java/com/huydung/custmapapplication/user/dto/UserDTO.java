package com.huydung.custmapapplication.user.dto;

import java.util.List;

public class UserDTO {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private Integer status;
    private List<String> roles;

    public UserDTO() {
    }

    public UserDTO(Long id, String username, String fullName, String phone, Integer status, List<String> roles) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.status = status;
        this.roles = roles;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
