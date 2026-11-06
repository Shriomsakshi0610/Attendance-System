package com.renaissance.dto;

public class SubjectDto {
    private String id;
    private String code;
    private String name;
    private String departmentId;
    private String departmentCode;
    private String departmentName;
    private Integer semester;
    private String classId;
    private String className;
    private String assignedTeacherId;
    private String assignedTeacherName;
    private Integer weeklyHours;
    private String status;

    public SubjectDto() {
    }

    public SubjectDto(String id, String code, String name, String departmentId, String departmentCode, Integer semester) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.semester = semester;
        this.status = "ACTIVE";
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

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
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

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getAssignedTeacherId() {
        return assignedTeacherId;
    }

    public void setAssignedTeacherId(String assignedTeacherId) {
        this.assignedTeacherId = assignedTeacherId;
    }

    public String getAssignedTeacherName() {
        return assignedTeacherName;
    }

    public void setAssignedTeacherName(String assignedTeacherName) {
        this.assignedTeacherName = assignedTeacherName;
    }

    public Integer getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(Integer weeklyHours) {
        this.weeklyHours = weeklyHours;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
