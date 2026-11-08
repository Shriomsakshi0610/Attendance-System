package com.renaissance.dto;

import java.util.List;

public class StudentProfileDto {
    private String id;
    private String usn;
    private String name;
    private String email;
    private String department;
    private Integer semester;
    private String section;
    private double overallPercentage;
    private long totalConducted;
    private long totalPresent;
    private long totalAbsent;
    private boolean below75;
    private List<HistoryRecordDto> attendanceHistory;
    private String filteredSubjectId;
    private String filteredSubjectName;
    private String filteredSubjectCode;

    public static class HistoryRecordDto {
        private String sessionId;
        private String sessionCode;
        private String subjectCode;
        private String subjectName;
        private String sessionDate;
        private String sessionTime;
        private String status;
        private String markedTime;
        private String distanceFromTeacher;

        public HistoryRecordDto() {
        }

        public HistoryRecordDto(String sessionId, String sessionCode, String subjectCode, String subjectName,
                                String sessionDate, String sessionTime, String status,
                                String markedTime, String distanceFromTeacher) {
            this.sessionId = sessionId;
            this.sessionCode = sessionCode;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.sessionDate = sessionDate;
            this.sessionTime = sessionTime;
            this.status = status;
            this.markedTime = markedTime;
            this.distanceFromTeacher = distanceFromTeacher;
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

        public String getSessionDate() {
            return sessionDate;
        }

        public void setSessionDate(String sessionDate) {
            this.sessionDate = sessionDate;
        }

        public String getSessionTime() {
            return sessionTime;
        }

        public void setSessionTime(String sessionTime) {
            this.sessionTime = sessionTime;
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

    public StudentProfileDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public double getOverallPercentage() {
        return overallPercentage;
    }

    public void setOverallPercentage(double overallPercentage) {
        this.overallPercentage = overallPercentage;
    }

    public long getTotalConducted() {
        return totalConducted;
    }

    public void setTotalConducted(long totalConducted) {
        this.totalConducted = totalConducted;
    }

    public long getTotalPresent() {
        return totalPresent;
    }

    public void setTotalPresent(long totalPresent) {
        this.totalPresent = totalPresent;
    }

    public long getTotalAbsent() {
        return totalAbsent;
    }

    public void setTotalAbsent(long totalAbsent) {
        this.totalAbsent = totalAbsent;
    }

    public boolean isBelow75() {
        return below75;
    }

    public void setBelow75(boolean below75) {
        this.below75 = below75;
    }

    private List<SubjectAttendanceDto> subjectWiseAttendance = new java.util.ArrayList<>();

    public static class SubjectAttendanceDto {
        private String subjectId;
        private String subjectCode;
        private String subjectName;
        private long totalConducted;
        private long attended;
        private long absent;
        private double percentage;

        public SubjectAttendanceDto() {
        }

        public SubjectAttendanceDto(String subjectId, String subjectCode, String subjectName,
                                    long totalConducted, long attended, long absent, double percentage) {
            this.subjectId = subjectId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.totalConducted = totalConducted;
            this.attended = attended;
            this.absent = absent;
            this.percentage = percentage;
        }

        public String getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(String subjectId) {
            this.subjectId = subjectId;
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

        public long getTotalConducted() {
            return totalConducted;
        }

        public void setTotalConducted(long totalConducted) {
            this.totalConducted = totalConducted;
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
    }

    public List<SubjectAttendanceDto> getSubjectWiseAttendance() {
        if (subjectWiseAttendance == null) {
            subjectWiseAttendance = new java.util.ArrayList<>();
        }
        return subjectWiseAttendance;
    }

    public void setSubjectWiseAttendance(List<SubjectAttendanceDto> subjectWiseAttendance) {
        this.subjectWiseAttendance = subjectWiseAttendance;
    }

    public List<HistoryRecordDto> getAttendanceHistory() {
        return attendanceHistory;
    }

    public void setAttendanceHistory(List<HistoryRecordDto> attendanceHistory) {
        this.attendanceHistory = attendanceHistory;
    }

    public String getFilteredSubjectId() {
        return filteredSubjectId;
    }

    public void setFilteredSubjectId(String filteredSubjectId) {
        this.filteredSubjectId = filteredSubjectId;
    }

    public String getFilteredSubjectName() {
        return filteredSubjectName;
    }

    public void setFilteredSubjectName(String filteredSubjectName) {
        this.filteredSubjectName = filteredSubjectName;
    }

    public String getFilteredSubjectCode() {
        return filteredSubjectCode;
    }

    public void setFilteredSubjectCode(String filteredSubjectCode) {
        this.filteredSubjectCode = filteredSubjectCode;
    }
}
