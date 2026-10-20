package com.example.news.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;            // BCrypt hash
    @Column(nullable = false)
    private String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;
    private boolean enabled = true;

    protected AppUser() {}
    public AppUser(String username, String password, String fullName, Role role) {
        this.username = username; this.password = password; this.fullName = fullName; this.role = role;
    }
    public void disable() { this.enabled = false; }   // soft delete

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
}
