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

    private String batchYear; // e.g. "2024-2028"

    private LocalDateTime createdAt = LocalDateTime.now();

    public CollegeClass() {
    }

    public CollegeClass(Department department, Integer semester, String section, String batchYear) {
        this.department = department;
        this.semester = semester;
        this.section = section;
        this.batchYear = batchYear;
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
        String deptCode = department != null ? department.getCode() : "DEPT";
        return deptCode + " - Sem " + semester + " (" + section + ")";
    }
}
