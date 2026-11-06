package com.renaissance.dto;

import jakarta.validation.constraints.NotNull;

public class CreateSessionRequest {

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Teacher latitude is required")
    private Double latitude;

    @NotNull(message = "Teacher longitude is required")
    private Double longitude;

    private Double accuracy;

    private Integer durationMinutes = 15; // default 15 minutes

    public CreateSessionRequest() {
    }

    public CreateSessionRequest(Long classId, Long subjectId, Double latitude, Double longitude, Double accuracy, Integer durationMinutes) {
        this.classId = classId;
        this.subjectId = subjectId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.durationMinutes = durationMinutes != null ? durationMinutes : 15;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
