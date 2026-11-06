package com.renaissance.dto;

public class DepartmentDto {
    private String id;
    private String code;
    private String name;
    private String description;
    private String status;

    public DepartmentDto() {
    }

    public DepartmentDto(String id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = "ACTIVE";
    }

    public DepartmentDto(String id, String code, String name, String description, String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = status;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
