package com.renaissance.dto;

public class LoginResponse {
    private String message;
    private TeacherDto teacher;
    private boolean authenticated;
    private String refreshToken;
    private Long sessionTimeoutSeconds;

    public LoginResponse() {
    }

    public LoginResponse(String message, TeacherDto teacher, boolean authenticated) {
        this.message = message;
        this.teacher = teacher;
        this.authenticated = authenticated;
    }

    public LoginResponse(String message, TeacherDto teacher, boolean authenticated, String refreshToken, Long sessionTimeoutSeconds) {
        this.message = message;
        this.teacher = teacher;
        this.authenticated = authenticated;
        this.refreshToken = refreshToken;
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
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

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getSessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }

    public void setSessionTimeoutSeconds(Long sessionTimeoutSeconds) {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }
}
