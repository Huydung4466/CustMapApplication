package com.huydung.custmapapplication.auth.dto;

import java.util.List;

public class CurrentUserResponse {
    private Long id;
    private String username;
    private String fullName;
    private boolean isAdmin;
    private List<String> roles;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(Long id, String username, String fullName, boolean isAdmin, List<String> roles) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.isAdmin = isAdmin;
        this.roles = roles;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles;}
}