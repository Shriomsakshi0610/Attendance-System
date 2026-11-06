package com.renaissance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_attempt_logs", indexes = {
    @Index(name = "idx_attempt_usn", columnList = "usn"),
    @Index(name = "idx_attempt_code", columnList = "session_code"),
    @Index(name = "idx_attempt_time", columnList = "attempt_time")
})
public class AttendanceAttemptLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "session_code", length = 20)
    private String sessionCode;

    @Column(length = 30)
    private String usn;

    @Column(name = "attempt_status", nullable = false, length = 20)
    private String attemptStatus; // SUCCESS, FAILED

    @Column(name = "failure_reason", length = 100)
    private String failureReason; // OUTSIDE_RADIUS, INVALID_CODE, etc.

    @Column(name = "student_latitude")
    private Double studentLatitude;

    @Column(name = "student_longitude")
    private Double studentLongitude;

    @Column(name = "student_accuracy")
    private Double studentAccuracy;

    @Column(name = "calculated_distance")
    private Double calculatedDistance;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    public AttendanceAttemptLog() {
    }

    public AttendanceAttemptLog(Long sessionId, String sessionCode, String usn, String attemptStatus,
                                String failureReason, Double studentLatitude, Double studentLongitude,
                                Double studentAccuracy, Double calculatedDistance,
                                String ipAddress, String userAgent) {
        this.sessionId = sessionId;
        this.sessionCode = sessionCode;
        this.usn = usn;
        this.attemptStatus = attemptStatus;
        this.failureReason = failureReason;
        this.studentLatitude = studentLatitude;
        this.studentLongitude = studentLongitude;
        this.studentAccuracy = studentAccuracy;
        this.calculatedDistance = calculatedDistance;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
        this.attemptTime = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.attemptTime == null) {
            this.attemptTime = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getAttemptStatus() {
        return attemptStatus;
    }

    public void setAttemptStatus(String attemptStatus) {
        this.attemptStatus = attemptStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Double getStudentLatitude() {
        return studentLatitude;
    }

    public void setStudentLatitude(Double studentLatitude) {
        this.studentLatitude = studentLatitude;
    }

    public Double getStudentLongitude() {
        return studentLongitude;
    }

    public void setStudentLongitude(Double studentLongitude) {
        this.studentLongitude = studentLongitude;
    }

    public Double getStudentAccuracy() {
        return studentAccuracy;
    }

    public void setStudentAccuracy(Double studentAccuracy) {
        this.studentAccuracy = studentAccuracy;
    }

    public Double getCalculatedDistance() {
        return calculatedDistance;
    }

    public void setCalculatedDistance(Double calculatedDistance) {
        this.calculatedDistance = calculatedDistance;
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
        this.userAgent = userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }

    public LocalDateTime getAttemptTime() {
        return attemptTime;
    }

    public void setAttemptTime(LocalDateTime attemptTime) {
        this.attemptTime = attemptTime;
    }
}
