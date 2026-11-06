package com.renaissance.dto;

public class ClassDto {
    private String id;
    private String departmentId;
    private String departmentCode;
    private String departmentName;
    private Integer semester;
    private String section;
    private String batchYear;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
