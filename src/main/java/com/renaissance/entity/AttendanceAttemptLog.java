package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "attendance_attempt_logs")
public class AttendanceAttemptLog {

    @Id
    private String id;

    private String sessionId;

    @Indexed
    private String sessionCode;

    @Indexed
    private String usn;

    private String attemptStatus; // SUCCESS, FAILED

    private String failureReason; // OUTSIDE_RADIUS, INVALID_CODE, etc.

    private Double studentLatitude;

    private Double studentLongitude;

    private Double studentAccuracy;

    private Double calculatedDistance;

    private String ipAddress;

    private String userAgent;

    @Indexed
    private LocalDateTime attemptTime = LocalDateTime.now();

    public AttendanceAttemptLog() {
    }

    public AttendanceAttemptLog(String sessionId, String sessionCode, String usn, String attemptStatus,
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
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
