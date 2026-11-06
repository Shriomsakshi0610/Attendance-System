package com.renaissance.dto;

public class SubjectDto {
    private Long id;
    private String code;
    private String name;
    private Long departmentId;
    private String departmentCode;
    private Integer semester;

    public SubjectDto() {
    }

    public SubjectDto(Long id, String code, String name, Long departmentId, String departmentCode, Integer semester) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.semester = semester;
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

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }
}
