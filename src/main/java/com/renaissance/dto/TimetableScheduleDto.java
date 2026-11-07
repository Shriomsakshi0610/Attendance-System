package com.renaissance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class TimetableScheduleDto {
    private String id;
    private String departmentId;
    private String departmentName;
    private String classId;
    private String className;
    private Integer semester;
    private String section;
    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String teacherId;
    private String teacherName;
    private DayOfWeek dayOfWeek;
    private String dayName;
    private Integer periodNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime endTime;

    private String startTimeFormatted;
    private String endTimeFormatted;
    private String timeSlot;
    private String roomNumber;
    private String status;

    public TimetableScheduleDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
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

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        if (dayOfWeek != null) {
            this.dayName = dayOfWeek.name();
        }
    }

    public String getDayName() {
        return dayName != null ? dayName : (dayOfWeek != null ? dayOfWeek.name() : null);
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public Integer getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(Integer periodNumber) {
        this.periodNumber = periodNumber;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        if (startTime != null) {
            this.startTimeFormatted = String.format("%02d:%02d", startTime.getHour(), startTime.getMinute());
        }
        updateTimeSlot();
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        if (endTime != null) {
            this.endTimeFormatted = String.format("%02d:%02d", endTime.getHour(), endTime.getMinute());
        }
        updateTimeSlot();
    }

    public String getStartTimeFormatted() {
        if (startTimeFormatted == null && startTime != null) {
            startTimeFormatted = String.format("%02d:%02d", startTime.getHour(), startTime.getMinute());
        }
        return startTimeFormatted;
    }

    public void setStartTimeFormatted(String startTimeFormatted) {
        this.startTimeFormatted = startTimeFormatted;
    }

    public String getEndTimeFormatted() {
        if (endTimeFormatted == null && endTime != null) {
            endTimeFormatted = String.format("%02d:%02d", endTime.getHour(), endTime.getMinute());
        }
        return endTimeFormatted;
    }

    public void setEndTimeFormatted(String endTimeFormatted) {
        this.endTimeFormatted = endTimeFormatted;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    private void updateTimeSlot() {
        if (startTime != null && endTime != null) {
            String s = String.format("%02d:%02d", startTime.getHour(), startTime.getMinute());
            String e = String.format("%02d:%02d", endTime.getHour(), endTime.getMinute());
            this.timeSlot = s + " - " + e;
        }
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
