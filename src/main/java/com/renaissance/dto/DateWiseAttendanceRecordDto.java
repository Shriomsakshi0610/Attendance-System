package com.renaissance.dto;

public class DateWiseAttendanceRecordDto {
    private String date;
    private String sessionId;
    private String sessionCode;
    private String studentId;
    private String studentName;
    private String usn;
    private String className;
    private String section;
    private String subjectCode;
    private String subjectName;
    private String status;
    private String markedTime;
    private String distanceFromTeacher;

    public DateWiseAttendanceRecordDto() {
    }

    public DateWiseAttendanceRecordDto(String date, String sessionId, String sessionCode,
                                       String studentId, String studentName, String usn,
                                       String className, String section, String subjectCode,
                                       String subjectName, String status, String markedTime,
                                       String distanceFromTeacher) {
        this.date = date;
        this.sessionId = sessionId;
        this.sessionCode = sessionCode;
        this.studentId = studentId;
        this.studentName = studentName;
        this.usn = usn;
        this.className = className;
        this.section = section;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.status = status;
        this.markedTime = markedTime;
        this.distanceFromTeacher = distanceFromTeacher;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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

    public String getStudentUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMarkedTime() {
        return markedTime;
    }

    public void setMarkedTime(String markedTime) {
        this.markedTime = markedTime;
    }

    public String getDistanceFromTeacher() {
        return distanceFromTeacher;
    }

    public void setDistanceFromTeacher(String distanceFromTeacher) {
        this.distanceFromTeacher = distanceFromTeacher;
    }
}
