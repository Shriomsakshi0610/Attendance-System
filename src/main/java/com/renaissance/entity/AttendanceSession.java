package com.renaissance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_sessions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_session_code", columnNames = {"session_code"})
}, indexes = {
    @Index(name = "idx_session_code", columnList = "session_code"),
    @Index(name = "idx_session_status", columnList = "status")
})
public class AttendanceSession {

    public enum SessionStatus {
        ACTIVE,
        COMPLETED,
        EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_code", nullable = false, length = 10, unique = true)
    private String sessionCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private CollegeClass collegeClass;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "teacher_latitude", nullable = false)
    private Double teacherLatitude;

    @Column(name = "teacher_longitude", nullable = false)
    private Double teacherLongitude;

    @Column(name = "teacher_accuracy")
    private Double teacherAccuracy;

    @Column(name = "geofence_radius_meters", nullable = false)
    private Double geofenceRadiusMeters = 15.0;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = SessionStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.startTime == null) {
            this.startTime = LocalDateTime.now();
        }
    }

    public boolean isExpired() {
        return this.status == SessionStatus.EXPIRED ||
               (this.status == SessionStatus.ACTIVE && LocalDateTime.now().isAfter(this.endTime));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
