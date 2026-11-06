package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "attendance_records")
@CompoundIndex(name = "uk_session_student", def = "{'session': 1, 'student': 1}", unique = true)
public class AttendanceRecord {

    public enum AttendanceStatus {
        PRESENT,
        ABSENT
    }

    @Id
    private String id;

    @DBRef
    private AttendanceSession session;

    @DBRef
    private Student student;

    @DBRef
    private CollegeClass collegeClass;

    @DBRef
    private Subject subject;

    private AttendanceStatus status = AttendanceStatus.PRESENT;

    private Double studentLatitude;

    private Double studentLongitude;

    private Double studentAccuracy;

    private Double distanceFromTeacher;

    private boolean bleVerified = false;

    private Integer bleRssi;

    private LocalDateTime markedAt = LocalDateTime.now();

    public AttendanceRecord() {
    }

    public AttendanceRecord(AttendanceSession session, Student student, AttendanceStatus status,
                            Double studentLatitude, Double studentLongitude, Double studentAccuracy,
                            Double distanceFromTeacher) {
        this.session = session;
        this.student = student;
        if (session != null) {
            this.collegeClass = session.getCollegeClass();
            this.subject = session.getSubject();
        }
        this.status = status;
        this.studentLatitude = studentLatitude;
        this.studentLongitude = studentLongitude;
        this.studentAccuracy = studentAccuracy;
        this.distanceFromTeacher = distanceFromTeacher;
        this.markedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AttendanceSession getSession() {
        return session;
    }

    public void setSession(AttendanceSession session) {
        this.session = session;
        if (session != null) {
            if (this.collegeClass == null) {
                this.collegeClass = session.getCollegeClass();
            }
            if (this.subject == null) {
                this.subject = session.getSubject();
            }
        }
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
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

    public boolean isBleVerified() {
        return bleVerified;
    }

    public void setBleVerified(boolean bleVerified) {
        this.bleVerified = bleVerified;
    }

    public Integer getBleRssi() {
        return bleRssi;
    }

    public void setBleRssi(Integer bleRssi) {
        this.bleRssi = bleRssi;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }
}
