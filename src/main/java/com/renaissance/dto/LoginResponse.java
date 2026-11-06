package com.renaissance.dto;

public class LoginResponse {
    private String message;
    private TeacherDto teacher;
    private boolean authenticated;

    public LoginResponse() {
    }

    public LoginResponse(String message, TeacherDto teacher, boolean authenticated) {
        this.message = message;
        this.teacher = teacher;
        this.authenticated = authenticated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TeacherDto getTeacher() {
        return teacher;
    }

    public void setTeacher(TeacherDto teacher) {
        this.teacher = teacher;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
