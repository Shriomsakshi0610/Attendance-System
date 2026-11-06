package com.renaissance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "classes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_dept_sem_sec", columnNames = {"department_id", "semester", "section"})
})
public class CollegeClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private Integer semester; // e.g. 1 to 8

    @Column(nullable = false, length = 10)
    private String section; // e.g. "A", "B", "C"

    @Column(name = "batch_year", length = 20)
    private String batchYear; // e.g. "2024-2028"

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CollegeClass() {
    }

    public CollegeClass(Department department, Integer semester, String section, String batchYear) {
        this.department = department;
        this.semester = semester;
        this.section = section;
        this.batchYear = batchYear;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getBatchYear() {
        return batchYear;
    }

    public void setBatchYear(String batchYear) {
        this.batchYear = batchYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDisplayName() {
        return department.getCode() + " - Sem " + semester + " (" + section + ")";
    }
}
