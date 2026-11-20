package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity tracking registered and approved student devices for attendance verification.
 * Prevents arbitrary, unregistered, or proxy devices from marking attendance.
 */
@Document(collection = "student_devices")
@CompoundIndexes({
    @CompoundIndex(name = "idx_device_usn_status", def = "{'usn': 1, 'status': 1}"),
    @CompoundIndex(name = "idx_device_token_status", def = "{'deviceToken': 1, 'status': 1}")
})
public class StudentDevice {

    public enum DeviceStatus {
        PENDING,
        APPROVED,
        REJECTED,
        REVOKED
    }

    @Id
    private String id;

    @Indexed
    private String studentId;

    @Indexed
    private String usn;

    private String studentName;

    @Indexed(unique = true)
    private String deviceToken; // Cryptographically secure 64-char hex or UUID stored in student browser/app

    private String deviceFingerprint; // Browser canvas/hardware fingerprint hash

    private String deviceModel; // e.g. "Pixel 7 / Android 14" or "MacBook Pro / Chrome 122"

    private String operatingSystem; // Android, iOS, Windows, macOS, Linux

    private String browser;

    @Indexed
    private DeviceStatus status = DeviceStatus.PENDING;

    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime approvedAt;

    private String approvedBy; // Admin ID or email

    private LocalDateTime rejectedAt;

    private String rejectedReason;

    private LocalDateTime revokedAt;

    private String revocationReason;

    private LocalDateTime lastUsedAt;

    private String ipAddress;

    private String userAgent;

    public StudentDevice() {
    }

    public StudentDevice(String studentId, String usn, String studentName, String deviceToken,
                         String deviceFingerprint, String deviceModel, String operatingSystem,
                         String browser, String ipAddress, String userAgent) {
        this.studentId = studentId;
        this.usn = usn != null ? usn.trim().toUpperCase() : null;
        this.studentName = studentName;
        this.deviceToken = deviceToken;
        this.deviceFingerprint = deviceFingerprint;
        this.deviceModel = deviceModel;
        this.operatingSystem = operatingSystem;
        this.browser = browser;
        this.status = DeviceStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn != null ? usn.trim().toUpperCase() : null;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public boolean isApproved() {
        return this.status == DeviceStatus.APPROVED;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
