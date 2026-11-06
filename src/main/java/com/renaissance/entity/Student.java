package com.renaissance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students", uniqueConstraints = {
    @UniqueConstraint(name = "uk_student_usn", columnNames = {"usn"})
}, indexes = {
    @Index(name = "idx_student_usn", columnList = "usn")
})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String usn; // e.g. "1MS21CS001"

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private CollegeClass collegeClass;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Student() {
    }

    public Student(String usn, String name, String email, Department department, CollegeClass collegeClass) {
        this.usn = usn != null ? usn.trim().toUpperCase() : null;
        this.name = name != null ? name.trim() : null;
        this.email = email != null ? email.trim() : null;
        this.department = department;
        this.collegeClass = collegeClass;
        this.status = "ACTIVE";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.usn != null) {
            this.usn = this.usn.trim().toUpperCase();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn != null ? usn.trim().toUpperCase() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public CollegeClass getCollegeClass() {
        return collegeClass;
    }

    public void setCollegeClass(CollegeClass collegeClass) {
        this.collegeClass = collegeClass;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
