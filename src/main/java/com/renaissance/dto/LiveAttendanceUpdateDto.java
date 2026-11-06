package com.renaissance.dto;

import java.time.LocalDateTime;

public class LiveAttendanceUpdateDto {
    private String sessionId;
    private String sessionCode;
    private String usn;
    private String studentName;
    private String status;
    private String distanceFromTeacher;
    private long presentCount;
    private long totalEnrolled;
    private double currentPercentage;
    private LocalDateTime markedAt;

    public LiveAttendanceUpdateDto() {
    }

    public LiveAttendanceUpdateDto(String sessionId, String sessionCode, String usn, String studentName,
                                  String status, String distanceFromTeacher, long presentCount,
                                  long totalEnrolled, double currentPercentage, LocalDateTime markedAt) {
        this.sessionId = sessionId;
        this.sessionCode = sessionCode;
        this.usn = usn;
        this.studentName = studentName;
        this.status = status;
        this.distanceFromTeacher = distanceFromTeacher;
        this.presentCount = presentCount;
        this.totalEnrolled = totalEnrolled;
        this.currentPercentage = currentPercentage;
        this.markedAt = markedAt;
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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDistanceFromTeacher() {
        return distanceFromTeacher;
    }

    public void setDistanceFromTeacher(String distanceFromTeacher) {
        this.distanceFromTeacher = distanceFromTeacher;
    }

    public long getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(long presentCount) {
        this.presentCount = presentCount;
    }

    public long getTotalEnrolled() {
        return totalEnrolled;
    }

    public void setTotalEnrolled(long totalEnrolled) {
        this.totalEnrolled = totalEnrolled;
    }

    public double getCurrentPercentage() {
        return currentPercentage;
    }

    public void setCurrentPercentage(double currentPercentage) {
        this.currentPercentage = currentPercentage;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }
}
