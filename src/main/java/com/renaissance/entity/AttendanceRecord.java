package com.renaissance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_session_student", columnNames = {"session_id", "student_id"})
}, indexes = {
    @Index(name = "idx_record_session", columnList = "session_id"),
    @Index(name = "idx_record_student", columnList = "student_id")
})
public class AttendanceRecord {

    public enum AttendanceStatus {
        PRESENT,
        ABSENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "student_latitude")
    private Double studentLatitude;

    @Column(name = "student_longitude")
    private Double studentLongitude;

    @Column(name = "student_accuracy")
    private Double studentAccuracy;

    @Column(name = "distance_from_teacher")
    private Double distanceFromTeacher;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt;

    public AttendanceRecord() {
    }

    public AttendanceRecord(AttendanceSession session, Student student, AttendanceStatus status,
                            Double studentLatitude, Double studentLongitude, Double studentAccuracy,
                            Double distanceFromTeacher) {
        this.session = session;
        this.student = student;
        this.status = status;
        this.studentLatitude = studentLatitude;
        this.studentLongitude = studentLongitude;
        this.studentAccuracy = studentAccuracy;
        this.distanceFromTeacher = distanceFromTeacher;
        this.markedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.markedAt == null) {
            this.markedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AttendanceSession getSession() {
        return session;
    }

    public void setSession(AttendanceSession session) {
        this.session = session;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
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

    public Double getDistanceFromTeacher() {
        return distanceFromTeacher;
    }

    public void setDistanceFromTeacher(Double distanceFromTeacher) {
        this.distanceFromTeacher = distanceFromTeacher;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }
}
