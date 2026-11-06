package com.renaissance.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardStatsDto {
    private long totalDepartments;
    private long totalClasses;
    private long totalSubjects;
    private long totalStudents;
    private long totalTeachers;
    private long activeSessionsCount;
    private long todayAttendanceCount;
    private List<SessionDto> activeSessions = new ArrayList<>();

    public AdminDashboardStatsDto() {
    }

    public long getTotalDepartments() {
        return totalDepartments;
    }

    public void setTotalDepartments(long totalDepartments) {
        this.totalDepartments = totalDepartments;
    }

    public long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public long getTotalSubjects() {
        return totalSubjects;
    }

    public void setTotalSubjects(long totalSubjects) {
        this.totalSubjects = totalSubjects;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalTeachers() {
        return totalTeachers;
    }

    public void setTotalTeachers(long totalTeachers) {
        this.totalTeachers = totalTeachers;
    }

    public long getActiveSessionsCount() {
        return activeSessionsCount;
    }

    public void setActiveSessionsCount(long activeSessionsCount) {
        this.activeSessionsCount = activeSessionsCount;
    }

    public long getTodayAttendanceCount() {
        return todayAttendanceCount;
    }

    public void setTodayAttendanceCount(long todayAttendanceCount) {
        this.todayAttendanceCount = todayAttendanceCount;
    }

    public List<SessionDto> getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(List<SessionDto> activeSessions) {
        this.activeSessions = activeSessions;
    }
}
