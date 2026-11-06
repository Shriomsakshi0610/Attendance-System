package com.renaissance.dto;

public class AttendanceResultDto {
    private String studentName;
    private String usn;
    private String department;
    private Integer semester;
    private String section;
    private String subject;
    private String dateTime;
    private String distanceFromTeacher;
    private Double rawDistanceMeters;
    private Double accuracyMeters;
    private String attendanceStatus;

    public AttendanceResultDto() {
    }

    public AttendanceResultDto(String studentName, String usn, String department, Integer semester,
                               String section, String subject, String dateTime,
                               String distanceFromTeacher, Double rawDistanceMeters,
                               Double accuracyMeters, String attendanceStatus) {
        this.studentName = studentName;
        this.usn = usn;
        this.department = department;
        this.semester = semester;
        this.section = section;
        this.subject = subject;
        this.dateTime = dateTime;
        this.distanceFromTeacher = distanceFromTeacher;
        this.rawDistanceMeters = rawDistanceMeters;
        this.accuracyMeters = accuracyMeters;
        this.attendanceStatus = attendanceStatus;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getDistanceFromTeacher() {
        return distanceFromTeacher;
    }

    public void setDistanceFromTeacher(String distanceFromTeacher) {
        this.distanceFromTeacher = distanceFromTeacher;
    }

    public Double getRawDistanceMeters() {
        return rawDistanceMeters;
    }

    public void setRawDistanceMeters(Double rawDistanceMeters) {
        this.rawDistanceMeters = rawDistanceMeters;
    }

    public Double getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(Double accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }
}
