package com.renaissance.dto;

public class ClassDto {
    private String id;
    private String departmentId;
    private String departmentCode;
    private String departmentName;
    private Integer semester;
    private String section;
    private String batchYear;
    private String academicYear;
    private String className;
    private String classTeacherId;
    private String classTeacherName;
    private String status;
    private String displayName;

    public ClassDto() {
    }

    public ClassDto(String id, String departmentId, String departmentCode, String departmentName,
                    Integer semester, String section, String batchYear) {
        this.id = id;
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.semester = semester;
        this.section = section;
        this.batchYear = batchYear;
        this.academicYear = batchYear;
        this.className = departmentCode + " Sem " + semester + " (" + section + ")";
        this.status = "ACTIVE";
        this.displayName = departmentCode + " - Sem " + semester + " (" + section + ")";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getBatchYear() {
        return batchYear;
    }

    public void setBatchYear(String batchYear) {
        this.batchYear = batchYear;
    }

    public String getAcademicYear() {
        return academicYear != null ? academicYear : batchYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassTeacherId() {
        return classTeacherId;
    }

    public void setClassTeacherId(String classTeacherId) {
        this.classTeacherId = classTeacherId;
    }

    public String getClassTeacherName() {
        return classTeacherName;
    }

    public void setClassTeacherName(String classTeacherName) {
        this.classTeacherName = classTeacherName;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : (departmentCode + " - Sem " + semester + " (" + section + ")");
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
