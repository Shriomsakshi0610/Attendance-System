package com.renaissance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MarkAttendanceRequest {

    @NotBlank(message = "USN is required")
    private String usn;

    @NotBlank(message = "Attendance code is required")
    private String attendanceCode;

    private String classId;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Double accuracy; // reported by browser Geolocation API in meters

    private String bleBeaconUuid; // optional/required if BLE enabled for session
    private Integer bleRssi; // signal strength in dBm

    public MarkAttendanceRequest() {
    }

    public MarkAttendanceRequest(String usn, String attendanceCode, String classId, Double latitude, Double longitude, Double accuracy) {
        this.usn = usn;
        this.attendanceCode = attendanceCode;
        this.classId = classId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    public MarkAttendanceRequest(String usn, String attendanceCode, String classId, Double latitude, Double longitude, Double accuracy, String bleBeaconUuid, Integer bleRssi) {
        this.usn = usn;
        this.attendanceCode = attendanceCode;
        this.classId = classId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.bleBeaconUuid = bleBeaconUuid;
        this.bleRssi = bleRssi;
    }

    public String getBleBeaconUuid() {
        return bleBeaconUuid;
    }

    public void setBleBeaconUuid(String bleBeaconUuid) {
        this.bleBeaconUuid = bleBeaconUuid;
    }

    public Integer getBleRssi() {
        return bleRssi;
    }

    public void setBleRssi(Integer bleRssi) {
        this.bleRssi = bleRssi;
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

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
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
