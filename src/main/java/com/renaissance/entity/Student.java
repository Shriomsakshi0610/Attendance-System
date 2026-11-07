package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "students")
@CompoundIndexes({
    @CompoundIndex(name = "idx_student_class_status", def = "{'collegeClass': 1, 'status': 1}"),
    @CompoundIndex(name = "idx_student_dept_status", def = "{'department': 1, 'status': 1}")
})
public class Student {

    @Id
    private String id;

    @Indexed(unique = true)
    private String usn; // e.g. "1MS21CS001"

    @Indexed
    private String name;

    private String email;

    @DBRef(lazy = true)
    private Department department;

    @DBRef(lazy = true)
    private CollegeClass collegeClass;

    @Indexed
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    private LocalDateTime createdAt = LocalDateTime.now();

    public Student() {
    }

    public Student(String usn, String name, String email, Department department, CollegeClass collegeClass) {
        this.usn = usn != null ? usn.trim().toUpperCase() : null;
        this.name = name != null ? name.trim() : null;
        this.email = email != null ? email.trim() : null;
        this.department = department;
        this.collegeClass = collegeClass;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
