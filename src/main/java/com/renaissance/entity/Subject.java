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

    private LocalDateTime createdAt = LocalDateTime.now();

    public Subject() {
    }

    public Subject(String code, String name, Department department, Integer semester) {
        this.code = code;
        this.name = name;
        this.department = department;
        this.semester = semester;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
