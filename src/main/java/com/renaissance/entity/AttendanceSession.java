package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "attendance_sessions")
public class AttendanceSession {

    public enum SessionStatus {
        ACTIVE,
        COMPLETED,
        EXPIRED
    }

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionCode;

    @DBRef
    private Teacher teacher;

    @DBRef
    private CollegeClass collegeClass;

    @DBRef
    private Subject subject;

    private Double teacherLatitude;

    private Double teacherLongitude;

    private Double teacherAccuracy;

    private Double geofenceRadiusMeters = 15.0;

    private boolean bluetoothVerificationEnabled = false;

    private String bleBeaconUuid;

    private Integer bleRssiThreshold = -75;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Indexed
    private SessionStatus status = SessionStatus.ACTIVE;

    private LocalDateTime createdAt = LocalDateTime.now();

    public AttendanceSession() {
    }

    public AttendanceSession(String sessionCode, Teacher teacher, CollegeClass collegeClass,
                             Subject subject, Double teacherLatitude, Double teacherLongitude,
                             Double teacherAccuracy, Double geofenceRadiusMeters,
                             LocalDateTime startTime, LocalDateTime endTime) {
        this.sessionCode = sessionCode;
        this.teacher = teacher;
        this.collegeClass = collegeClass;
        this.subject = subject;
        this.teacherLatitude = teacherLatitude;
        this.teacherLongitude = teacherLongitude;
        this.teacherAccuracy = teacherAccuracy;
        this.geofenceRadiusMeters = geofenceRadiusMeters != null ? geofenceRadiusMeters : 15.0;
        this.startTime = startTime != null ? startTime : LocalDateTime.now();
        this.endTime = endTime;
        this.status = SessionStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return this.status == SessionStatus.EXPIRED ||
               (this.status == SessionStatus.ACTIVE && LocalDateTime.now().isAfter(this.endTime));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public CollegeClass getCollegeClass() {
        return collegeClass;
    }

    public void setCollegeClass(CollegeClass collegeClass) {
        this.collegeClass = collegeClass;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Double getTeacherLatitude() {
        return teacherLatitude;
    }

    public void setTeacherLatitude(Double teacherLatitude) {
        this.teacherLatitude = teacherLatitude;
    }

    public Double getTeacherLongitude() {
        return teacherLongitude;
    }

    public void setTeacherLongitude(Double teacherLongitude) {
        this.teacherLongitude = teacherLongitude;
    }

    public Double getTeacherAccuracy() {
        return teacherAccuracy;
    }

    public void setTeacherAccuracy(Double teacherAccuracy) {
        this.teacherAccuracy = teacherAccuracy;
    }

    public Double getGeofenceRadiusMeters() {
        return geofenceRadiusMeters;
    }

    public void setGeofenceRadiusMeters(Double geofenceRadiusMeters) {
        this.geofenceRadiusMeters = geofenceRadiusMeters;
    }

    public boolean isBluetoothVerificationEnabled() {
        return bluetoothVerificationEnabled;
    }

    public void setBluetoothVerificationEnabled(boolean bluetoothVerificationEnabled) {
        this.bluetoothVerificationEnabled = bluetoothVerificationEnabled;
    }

    public String getBleBeaconUuid() {
        return bleBeaconUuid;
    }

    public void setBleBeaconUuid(String bleBeaconUuid) {
        this.bleBeaconUuid = bleBeaconUuid;
    }

    public Integer getBleRssiThreshold() {
        return bleRssiThreshold;
    }

    public void setBleRssiThreshold(Integer bleRssiThreshold) {
        this.bleRssiThreshold = bleRssiThreshold;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
