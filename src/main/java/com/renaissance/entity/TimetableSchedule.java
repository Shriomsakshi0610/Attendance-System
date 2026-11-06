package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Document(collection = "timetable_schedules")
public class TimetableSchedule {

    @Id
    private String id;

    @DBRef
    private Department department;

    @DBRef
    private CollegeClass collegeClass;

    @DBRef
    private Subject subject;

    @DBRef
    private Teacher teacher;

    private String dayOfWeek; // MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY

    private LocalTime startTime;

    private LocalTime endTime;

    private String roomNumber;

    private String status = "ACTIVE"; // ACTIVE, CANCELLED

    private LocalDateTime createdAt = LocalDateTime.now();

    public TimetableSchedule() {
    }

    public TimetableSchedule(Department department, CollegeClass collegeClass, Subject subject,
                             Teacher teacher, String dayOfWeek, LocalTime startTime, LocalTime endTime,
                             String roomNumber) {
        this.department = department;
        this.collegeClass = collegeClass;
        this.subject = subject;
        this.teacher = teacher;
        this.dayOfWeek = dayOfWeek != null ? dayOfWeek.trim().toUpperCase() : "MONDAY";
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomNumber = roomNumber != null ? roomNumber.trim() : null;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public CollegeClass getCollegeClass() {
        return collegeClass;
    }

    public void setCollegeClass(CollegeClass collegeClass) {
        this.collegeClass = collegeClass;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek != null ? dayOfWeek.trim().toUpperCase() : null;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
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

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
