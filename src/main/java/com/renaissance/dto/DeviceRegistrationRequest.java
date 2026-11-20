package com.renaissance.dto;

import jakarta.validation.constraints.NotBlank;

public class DeviceRegistrationRequest {

    @NotBlank(message = "USN is required")
    private String usn;

    @NotBlank(message = "Device token is required")
    private String deviceToken;

    private String deviceFingerprint;
    private String deviceModel;
    private String operatingSystem;
    private String browser;

    public DeviceRegistrationRequest() {
    }

    public DeviceRegistrationRequest(String usn, String deviceToken, String deviceFingerprint,
                                   String deviceModel, String operatingSystem, String browser) {
        this.usn = usn;
        this.deviceToken = deviceToken;
        this.deviceFingerprint = deviceFingerprint;
        this.deviceModel = deviceModel;
        this.operatingSystem = operatingSystem;
        this.browser = browser;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }
}
