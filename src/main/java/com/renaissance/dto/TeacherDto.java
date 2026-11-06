package com.renaissance.dto;

import java.util.ArrayList;
import java.util.List;

public class TeacherDto {
    private String id;
    private String name;
    private String email;
    private String employeeId;
    private String departmentId;
    private String departmentName;
    private String departmentCode;
    private String phone;
    private String role;
    private String status;
    private List<String> assignedClassIds = new ArrayList<>();
    private List<String> assignedClassNames = new ArrayList<>();
    private List<String> assignedSubjectIds = new ArrayList<>();
    private List<String> assignedSubjectNames = new ArrayList<>();

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
        this.status = "ACTIVE";
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

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getAssignedClassIds() {
        return assignedClassIds;
    }

    public void setAssignedClassIds(List<String> assignedClassIds) {
        this.assignedClassIds = assignedClassIds;
    }

    public List<String> getAssignedClassNames() {
        return assignedClassNames;
    }

    public void setAssignedClassNames(List<String> assignedClassNames) {
        this.assignedClassNames = assignedClassNames;
    }

    public List<String> getAssignedSubjectIds() {
        return assignedSubjectIds;
    }

    public void setAssignedSubjectIds(List<String> assignedSubjectIds) {
        this.assignedSubjectIds = assignedSubjectIds;
    }

    public List<String> getAssignedSubjectNames() {
        return assignedSubjectNames;
    }

    public void setAssignedSubjectNames(List<String> assignedSubjectNames) {
        this.assignedSubjectNames = assignedSubjectNames;
    }
}
