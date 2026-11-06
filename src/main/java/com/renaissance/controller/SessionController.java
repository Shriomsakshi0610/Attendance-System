package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.CreateSessionRequest;
import com.renaissance.dto.SessionDto;
import com.renaissance.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SessionDto>> startSession(@Valid @RequestBody CreateSessionRequest request) {
        SessionDto session = sessionService.createAndStartSession(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Session started successfully", session));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<SessionDto>> endSession(@PathVariable String id) {
        SessionDto session = sessionService.endSession(id);
        return ResponseEntity.ok(ApiResponse.ok("Session ended", session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionDto>> getSession(@PathVariable String id) {
        SessionDto session = sessionService.getSessionById(id);
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SessionDto>>> getActiveSessions() {
        List<SessionDto> activeSessions = sessionService.getActiveSessions();
        return ResponseEntity.ok(ApiResponse.ok(activeSessions));
    }

    @GetMapping("/my-sessions")
    public ResponseEntity<ApiResponse<List<SessionDto>>> getTeacherSessions() {
        List<SessionDto> sessions = sessionService.getTeacherSessions();
        return ResponseEntity.ok(ApiResponse.ok(sessions));
    }
}
