package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Persistent Refresh Token for Smart Teacher Session Management.
 * Stored in MongoDB so authentication sessions survive Render Free Tier
 * container sleep cycles, JVM restarts, and redeployments.
 */
@Document(collection = "refresh_tokens")
public class UserRefreshToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String teacherId;

    @Indexed
    private String email;

    private String userRole = "ROLE_TEACHER";

    private String deviceInfo;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Indexed
    private LocalDateTime expiresAt;

    private boolean revoked = false;

    private LocalDateTime lastUsedAt = LocalDateTime.now();

    public UserRefreshToken() {
    }

    public UserRefreshToken(String token, String teacherId, String email, String userRole, String deviceInfo, LocalDateTime expiresAt) {
        this.token = token;
        this.teacherId = teacherId;
        this.email = email;
        this.userRole = userRole != null ? userRole : "ROLE_TEACHER";
        this.deviceInfo = deviceInfo;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.lastUsedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
