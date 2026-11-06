package com.renaissance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MarkAttendanceRequest {

    @NotBlank(message = "USN is required")
    private String usn;

    @NotBlank(message = "Attendance code is required")
    private String attendanceCode;

    private Long classId;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Double accuracy; // reported by browser Geolocation API in meters

    public MarkAttendanceRequest() {
    }

    public MarkAttendanceRequest(String usn, String attendanceCode, Long classId, Double latitude, Double longitude, Double accuracy) {
        this.usn = usn;
        this.attendanceCode = attendanceCode;
        this.classId = classId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getAttendanceCode() {
        return attendanceCode;
    }

    public void setAttendanceCode(String attendanceCode) {
        this.attendanceCode = attendanceCode;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
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
}
