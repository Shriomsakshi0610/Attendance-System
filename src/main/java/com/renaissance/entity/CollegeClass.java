package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "classes")
@CompoundIndex(name = "uk_dept_sem_sec", def = "{'department': 1, 'semester': 1, 'section': 1}", unique = true)
public class CollegeClass {

    @Id
    private String id;

    @DBRef
    private Department department;

    private Integer semester; // e.g. 1 to 8

    private String section; // e.g. "A", "B", "C"

    private String className; // e.g. "ISE 4th Semester"

    private String batchYear; // e.g. "2024-2028"

    private String academicYear = "2026-2027";

    @DBRef
    private Teacher classTeacher; // Optional assigned faculty mentor / class teacher

    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    private LocalDateTime createdAt = LocalDateTime.now();

    public CollegeClass() {
    }

    public CollegeClass(Department department, Integer semester, String section, String batchYear) {
        this.department = department;
        this.semester = semester;
        this.section = section;
        this.batchYear = batchYear;
        this.academicYear = "2026-2027";
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public CollegeClass(Department department, Integer semester, String section, String batchYear, String academicYear, Teacher classTeacher) {
        this.department = department;
        this.semester = semester;
        this.section = section;
        this.batchYear = batchYear;
        this.academicYear = academicYear != null ? academicYear : "2026-2027";
        this.classTeacher = classTeacher;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getBatchYear() {
        return batchYear;
    }

    public void setBatchYear(String batchYear) {
        this.batchYear = batchYear;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public Teacher getClassTeacher() {
        return classTeacher;
    }

    public void setClassTeacher(Teacher classTeacher) {
        this.classTeacher = classTeacher;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDisplayName() {
        String deptCode = department != null ? department.getCode() : "DEPT";
        if (className != null && !className.isBlank()) {
            return className + " (" + section + ")";
        }
        return deptCode + " - Sem " + semester + " (" + section + ")";
    }
}
