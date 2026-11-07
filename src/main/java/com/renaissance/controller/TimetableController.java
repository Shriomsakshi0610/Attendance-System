package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.TimetableScheduleDto;
import com.renaissance.entity.Teacher;
import com.renaissance.service.AuthService;
import com.renaissance.service.TimetableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    private final TimetableService timetableService;
    private final AuthService authService;

    public TimetableController(TimetableService timetableService, AuthService authService) {
        this.timetableService = timetableService;
        this.authService = authService;
    }

    @GetMapping({"/my-schedule", "/teacher"})
    public ResponseEntity<ApiResponse<List<TimetableScheduleDto>>> getMySchedule(
            @RequestParam(required = false) String day) {
        Teacher currentTeacher = authService.getCurrentTeacher();
        List<TimetableScheduleDto> schedules = timetableService.getSchedulesByTeacher(currentTeacher.getId());
        if (day != null && !day.isBlank()) {
            String dayUpper = day.trim().toUpperCase();
            schedules = schedules.stream()
                    .filter(s -> (s.getDayName() != null && s.getDayName().equalsIgnoreCase(dayUpper)) ||
                            (s.getDayOfWeek() != null && s.getDayOfWeek().name().equalsIgnoreCase(dayUpper)))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.ok(schedules));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<TimetableScheduleDto>>> getScheduleByClass(@PathVariable String classId) {
        List<TimetableScheduleDto> schedules = timetableService.getSchedulesByClass(classId);
        return ResponseEntity.ok(ApiResponse.ok(schedules));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<List<TimetableScheduleDto>>> getScheduleByTeacher(@PathVariable String teacherId) {
        List<TimetableScheduleDto> schedules = timetableService.getSchedulesByTeacher(teacherId);
        return ResponseEntity.ok(ApiResponse.ok(schedules));
    }
}
