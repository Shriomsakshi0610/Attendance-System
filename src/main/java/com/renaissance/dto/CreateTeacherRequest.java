package com.renaissance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class CreateTeacherRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "Department ID is required")
    private String departmentId;

    private String phone;

    private String initialPassword;

    private List<String> assignedClassIds = new ArrayList<>();

    private List<String> assignedSubjectIds = new ArrayList<>();

    public CreateTeacherRequest() {
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

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }

    public List<String> getAssignedClassIds() {
        return assignedClassIds;
    }

    public void setAssignedClassIds(List<String> assignedClassIds) {
        this.assignedClassIds = assignedClassIds;
    }

    public List<String> getAssignedSubjectIds() {
        return assignedSubjectIds;
    }

    public void setAssignedSubjectIds(List<String> assignedSubjectIds) {
        this.assignedSubjectIds = assignedSubjectIds;
    }
}
