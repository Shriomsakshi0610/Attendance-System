package com.renaissance.dto;

import java.time.LocalDateTime;

public class SessionDto {
    private Long id;
    private String sessionCode;
    private Long teacherId;
    private String teacherName;
    private Long classId;
    private String className;
    private String departmentName;
    private String departmentCode;
    private Integer semester;
    private String section;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private Double teacherLatitude;
    private Double teacherLongitude;
    private Double geofenceRadiusMeters;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private long presentCount;
    private long totalEnrolled;
    private String attendanceLink;

    public SessionDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public Double getTeacherLatitude() {
        return teacherLatitude;
    }

    public void setTeacherLatitude(Double teacherLatitude) {
        this.teacherLatitude = teacherLatitude;
    }

    public Double getTeacherLongitude() {
        return teacherLongitude;
    }

    public void setTeacherLongitude(Double teacherLongitude) {
        this.teacherLongitude = teacherLongitude;
    }

    public Double getGeofenceRadiusMeters() {
        return geofenceRadiusMeters;
    }

    public void setGeofenceRadiusMeters(Double geofenceRadiusMeters) {
        this.geofenceRadiusMeters = geofenceRadiusMeters;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(long presentCount) {
        this.presentCount = presentCount;
    }

    public long getTotalEnrolled() {
        return totalEnrolled;
    }

    public void setTotalEnrolled(long totalEnrolled) {
        this.totalEnrolled = totalEnrolled;
    }

    public String getAttendanceLink() {
        return attendanceLink;
    }

    public void setAttendanceLink(String attendanceLink) {
        this.attendanceLink = attendanceLink;
    }
}
