package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "departments")
public class Department {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code; // e.g. "CSE", "ISE", "ECE", "MECH"

    private String name; // e.g. "Computer Science & Engineering"

    private String description;

    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    private LocalDateTime createdAt = LocalDateTime.now();

    public Department() {
    }

    public Department(String code, String name) {
        this.code = code;
        this.name = name;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public Department(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public Department(String code, String name, String description, String status) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = status != null ? status : "ACTIVE";
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
