package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.AttendanceResultDto;
import com.renaissance.dto.MarkAttendanceRequest;
import com.renaissance.dto.SessionDto;
import com.renaissance.entity.AttendanceRecord;
import com.renaissance.service.AttendanceService;
import com.renaissance.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SessionService sessionService;

    public AttendanceController(AttendanceService attendanceService, SessionService sessionService) {
        this.attendanceService = attendanceService;
        this.sessionService = sessionService;
    }

    @PostMapping("/mark")
    public ResponseEntity<ApiResponse<AttendanceResultDto>> markAttendance(
            @Valid @RequestBody MarkAttendanceRequest request,
            HttpServletRequest httpRequest) {
        AttendanceResultDto result = attendanceService.markAttendance(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Attendance marked as PRESENT", result));
    }

    @GetMapping("/session-info/{code}")
    public ResponseEntity<ApiResponse<SessionDto>> getSessionInfo(@PathVariable String code) {
        SessionDto session = sessionService.getSessionByCode(code);
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    @GetMapping("/session/{sessionId}/records")
    public ResponseEntity<ApiResponse<List<AttendanceRecord>>> getSessionRecords(@PathVariable String sessionId) {
        List<AttendanceRecord> records = attendanceService.getSessionRecords(sessionId);
        return ResponseEntity.ok(ApiResponse.ok(records));
    }
}
