package com.renaissance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"code"})
})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String code; // e.g. "CSE", "ISE", "ECE", "MECH"

    @Column(nullable = false, length = 100)
    private String name; // e.g. "Computer Science & Engineering"

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Department() {
    }

    public Department(String code, String name) {
        this.code = code;
        this.name = name;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
