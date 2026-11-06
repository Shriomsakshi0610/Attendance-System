package com.renaissance.dto;

public class AttendanceSummaryDto {
    private String studentId;
    private String usn;
    private String studentName;
    private String department;
    private Integer semester;
    private String section;
    private String subject;
    private long totalClasses;
    private long attended;
    private long absent;
    private double percentage;
    private boolean below75;

    public AttendanceSummaryDto() {
    }

    public AttendanceSummaryDto(String studentId, String usn, String studentName, String department,
                                Integer semester, String section, String subject,
                                long totalClasses, long attended, long absent, double percentage) {
        this.studentId = studentId;
        this.usn = usn;
        this.studentName = studentName;
        this.department = department;
        this.semester = semester;
        this.section = section;
        this.subject = subject;
        this.totalClasses = totalClasses;
        this.attended = attended;
        this.absent = absent;
        this.percentage = Math.round(percentage * 100.0) / 100.0;
        this.below75 = this.percentage < 75.0;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public long getAttended() {
        return attended;
    }

    public void setAttended(long attended) {
        this.attended = attended;
    }

    public long getAbsent() {
        return absent;
    }

    public void setAbsent(long absent) {
        this.absent = absent;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public boolean isBelow75() {
        return below75;
    }

    public void setBelow75(boolean below75) {
        this.below75 = below75;
    }
}
