package com.renaissance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateTimetableScheduleRequest {

    @NotBlank(message = "Class ID is required")
    private String classId;

    @NotBlank(message = "Subject ID is required")
    private String subjectId;

    @NotBlank(message = "Teacher ID is required")
    private String teacherId;

    @NotBlank(message = "Day of week is required (e.g. MONDAY)")
    private String dayOfWeek;

    @NotBlank(message = "Start time is required (HH:mm)")
    private String startTime;

    @NotBlank(message = "End time is required (HH:mm)")
    private String endTime;

    private String roomNumber;

    private Integer periodNumber;

    public CreateTimetableScheduleRequest() {
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(Integer periodNumber) {
        this.periodNumber = periodNumber;
    }
}
