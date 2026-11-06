package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "subjects")
public class Subject {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code; // e.g. "21CS51", "21CS52"

    private String name; // e.g. "Database Management Systems"

    @DBRef
    private Department department;

    private Integer semester;

    @DBRef
    private CollegeClass collegeClass; // Optional specific class/section assignment

    @DBRef
    private Teacher assignedTeacher; // Faculty member assigned to instruct this course

    private Integer weeklyHours = 4;

    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    private LocalDateTime createdAt = LocalDateTime.now();

    public Subject() {
    }

    public Subject(String code, String name, Department department, Integer semester) {
        this.code = code;
        this.name = name;
        this.department = department;
        this.semester = semester;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public Subject(String code, String name, Department department, Integer semester, CollegeClass collegeClass, Teacher assignedTeacher, Integer weeklyHours) {
        this.code = code;
        this.name = name;
        this.department = department;
        this.semester = semester;
        this.collegeClass = collegeClass;
        this.assignedTeacher = assignedTeacher;
        this.weeklyHours = weeklyHours != null ? weeklyHours : 4;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public CollegeClass getCollegeClass() {
        return collegeClass;
    }

    public void setCollegeClass(CollegeClass collegeClass) {
        this.collegeClass = collegeClass;
    }

    public Teacher getAssignedTeacher() {
        return assignedTeacher;
    }

    public void setAssignedTeacher(Teacher assignedTeacher) {
        this.assignedTeacher = assignedTeacher;
    }

    public Integer getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(Integer weeklyHours) {
        this.weeklyHours = weeklyHours;
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
}
