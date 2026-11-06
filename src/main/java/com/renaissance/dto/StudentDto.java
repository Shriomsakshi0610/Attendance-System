package com.renaissance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StudentDto {
    private Long id;

    @NotBlank(message = "USN is required")
    private String usn;

    @NotBlank(message = "Name is required")
    private String name;

    private String email;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private String departmentCode;
    private String departmentName;

    @NotNull(message = "Class ID is required")
    private Long classId;

    private String className;
    private Integer semester;
    private String section;
    private String status;

    public StudentDto() {
    }

    public StudentDto(Long id, String usn, String name, String email,
                      Long departmentId, String departmentCode, String departmentName,
                      Long classId, String className, Integer semester, String section, String status) {
        this.id = id;
        this.usn = usn;
        this.name = name;
        this.email = email;
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.classId = classId;
        this.className = className;
        this.semester = semester;
        this.section = section;
        this.status = status;
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
        this.usn = usn;
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
