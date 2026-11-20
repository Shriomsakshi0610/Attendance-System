package com.renaissance.dto;

public class DeviceStatusResponse {

    private boolean isApproved;
    private String status; // APPROVED, PENDING, REJECTED, REVOKED, NOT_REGISTERED, MISMATCH
    private String usn;
    private String studentName;
    private String message;
    private String deviceId;

    public DeviceStatusResponse() {
    }

    public DeviceStatusResponse(boolean isApproved, String status, String usn, String studentName, String message, String deviceId) {
        this.isApproved = isApproved;
        this.status = status;
        this.usn = usn;
        this.studentName = studentName;
        this.message = message;
        this.deviceId = deviceId;
    }

    public static DeviceStatusResponse approved(String usn, String studentName, String deviceId) {
        return new DeviceStatusResponse(true, "APPROVED", usn, studentName, "Device is verified and approved.", deviceId);
    }

    public static DeviceStatusResponse pending(String usn, String studentName) {
        return new DeviceStatusResponse(false, "PENDING", usn, studentName, "Device registration is pending administrator approval.", null);
    }

    public static DeviceStatusResponse notRegistered(String message) {
        return new DeviceStatusResponse(false, "NOT_REGISTERED", null, null, message != null ? message : "Device is not registered. Please submit a registration request.", null);
    }

    public static DeviceStatusResponse rejected(String message) {
        return new DeviceStatusResponse(false, "REJECTED", null, null, message != null ? message : "Device registration was rejected by administrator.", null);
    }

    public static DeviceStatusResponse revoked(String message) {
        return new DeviceStatusResponse(false, "REVOKED", null, null, message != null ? message : "Device registration has been revoked.", null);
    }

    public static DeviceStatusResponse mismatch(String registeredUsn) {
        return new DeviceStatusResponse(false, "MISMATCH", registeredUsn, null, "This device is registered to another student account (" + registeredUsn + "). Proxy marking is strictly prohibited.", null);
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
