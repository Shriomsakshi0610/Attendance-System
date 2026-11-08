package com.renaissance.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    private String deviceInfo;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken, String deviceInfo) {
        this.refreshToken = refreshToken;
        this.deviceInfo = deviceInfo;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}
