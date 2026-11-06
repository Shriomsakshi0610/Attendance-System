package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "enrollments")
@CompoundIndex(name = "uk_student_subject", def = "{'student': 1, 'subject': 1}", unique = true)
public class Enrollment {

    @Id
    private String id;

    @DBRef
    private Student student;

    @DBRef
    private CollegeClass collegeClass;

    @DBRef
    private Subject subject;

    private String academicYear = "2026-2027";

    private LocalDateTime createdAt = LocalDateTime.now();

    public Enrollment() {
    }

    public Enrollment(Student student, CollegeClass collegeClass, Subject subject, String academicYear) {
        this.student = student;
        this.collegeClass = collegeClass;
        this.subject = subject;
        this.academicYear = academicYear != null ? academicYear : "2026-2027";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
