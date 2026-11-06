package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "teachers")
public class Teacher {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String employeeId;

    private String phone;

    @DBRef
    private Department department;

    @DBRef
    private List<CollegeClass> assignedClasses = new ArrayList<>();

    @DBRef
    private List<Subject> assignedSubjects = new ArrayList<>();

    private String role = "ROLE_TEACHER"; // ROLE_TEACHER, ROLE_ADMIN

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Teacher() {
    }

    public Teacher(String name, String email, String password, String employeeId, Department department) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.employeeId = employeeId;
        this.department = department;
        this.role = "ROLE_TEACHER";
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Teacher(String name, String email, String password, String employeeId, Department department, String role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.employeeId = employeeId;
        this.department = department;
        this.role = role != null ? role : "ROLE_TEACHER";
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<CollegeClass> getAssignedClasses() {
        if (assignedClasses == null) {
            assignedClasses = new ArrayList<>();
        }
        return assignedClasses;
    }

    public void setAssignedClasses(List<CollegeClass> assignedClasses) {
        this.assignedClasses = assignedClasses;
    }

    public List<Subject> getAssignedSubjects() {
        if (assignedSubjects == null) {
            assignedSubjects = new ArrayList<>();
        }
        return assignedSubjects;
    }

    public void setAssignedSubjects(List<Subject> assignedSubjects) {
        this.assignedSubjects = assignedSubjects;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return active ? "ACTIVE" : "INACTIVE";
    }

    public void setStatus(String status) {
        this.active = "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(this.role);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
