package com.renaissance.dto;

public class RefreshResponse {

    private String refreshToken;
    private TeacherDto teacher;
    private long expiresInSeconds;

    public RefreshResponse() {
    }

    public RefreshResponse(String refreshToken, TeacherDto teacher, long expiresInSeconds) {
        this.refreshToken = refreshToken;
        this.teacher = teacher;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public TeacherDto getTeacher() {
        return teacher;
    }

    public void setTeacher(TeacherDto teacher) {
        this.teacher = teacher;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
