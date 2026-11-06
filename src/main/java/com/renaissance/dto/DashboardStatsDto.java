package com.renaissance.dto;

import java.util.List;
import java.util.Map;

public class DashboardStatsDto {
    private long totalStudents;
    private long totalClassesConducted;
    private double averageAttendancePercentage;
    private long todayAttendance;
    private long activeSessions;
    private List<SessionDto> recentActiveSessions;
    private Map<String, Double> subjectAttendanceTrends;
    private Map<String, Long> weeklyAttendanceCounts;

    public DashboardStatsDto() {
    }

    public DashboardStatsDto(long totalStudents, long totalClassesConducted,
                             double averageAttendancePercentage, long todayAttendance,
                             long activeSessions) {
        this.totalStudents = totalStudents;
        this.totalClassesConducted = totalClassesConducted;
        this.averageAttendancePercentage = Math.round(averageAttendancePercentage * 100.0) / 100.0;
        this.todayAttendance = todayAttendance;
        this.activeSessions = activeSessions;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalClassesConducted() {
        return totalClassesConducted;
    }

    public void setTotalClassesConducted(long totalClassesConducted) {
        this.totalClassesConducted = totalClassesConducted;
    }

    public double getAverageAttendancePercentage() {
        return averageAttendancePercentage;
    }

    public void setAverageAttendancePercentage(double averageAttendancePercentage) {
        this.averageAttendancePercentage = Math.round(averageAttendancePercentage * 100.0) / 100.0;
    }

    public long getTodayAttendance() {
        return todayAttendance;
    }

    public void setTodayAttendance(long todayAttendance) {
        this.todayAttendance = todayAttendance;
    }

    public long getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(long activeSessions) {
        this.activeSessions = activeSessions;
    }

    public List<SessionDto> getRecentActiveSessions() {
        return recentActiveSessions;
    }

    public void setRecentActiveSessions(List<SessionDto> recentActiveSessions) {
        this.recentActiveSessions = recentActiveSessions;
    }

    public Map<String, Double> getSubjectAttendanceTrends() {
        return subjectAttendanceTrends;
    }

    public void setSubjectAttendanceTrends(Map<String, Double> subjectAttendanceTrends) {
        this.subjectAttendanceTrends = subjectAttendanceTrends;
    }

    public Map<String, Long> getWeeklyAttendanceCounts() {
        return weeklyAttendanceCounts;
    }

    public void setWeeklyAttendanceCounts(Map<String, Long> weeklyAttendanceCounts) {
        this.weeklyAttendanceCounts = weeklyAttendanceCounts;
    }
}
