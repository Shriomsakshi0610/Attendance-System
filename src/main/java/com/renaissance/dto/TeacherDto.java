package com.renaissance.dto;

public class TeacherDto {
    private String id;
    private String name;
    private String email;
    private String employeeId;
    private String departmentName;
    private String departmentCode;
    private String role;

    public TeacherDto() {
    }

    public TeacherDto(String id, String name, String email, String employeeId,
                      String departmentName, String departmentCode, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.employeeId = employeeId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.role = role;
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

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
