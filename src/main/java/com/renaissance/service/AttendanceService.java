package com.renaissance.service;

import com.renaissance.dto.AttendanceResultDto;
import com.renaissance.dto.LiveAttendanceUpdateDto;
import com.renaissance.dto.MarkAttendanceRequest;
import com.renaissance.entity.*;
import com.renaissance.exception.AttendanceValidationException;
import com.renaissance.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceAttemptLogRepository attemptLogRepository;
    private final LocationVerificationService locationService;
    private final SimpMessageSendingOperations messagingTemplate;

    public AttendanceService(AttendanceSessionRepository sessionRepository,
                             StudentRepository studentRepository,
                             AttendanceRecordRepository recordRepository,
                             AttendanceAttemptLogRepository attemptLogRepository,
                             LocationVerificationService locationService,
                             SimpMessageSendingOperations messagingTemplate) {
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.recordRepository = recordRepository;
        this.attemptLogRepository = attemptLogRepository;
        this.locationService = locationService;
        this.messagingTemplate = messagingTemplate;
    }

    public AttendanceResultDto markAttendance(MarkAttendanceRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "unknown";

        String usn = request.getUsn() != null ? request.getUsn().trim().toUpperCase() : null;
        String code = request.getAttendanceCode() != null ? request.getAttendanceCode().trim().toUpperCase() : null;

        // 1. Validate Code Presence and Session State
        if (code == null || code.isEmpty()) {
            logAttempt(null, code, usn, "FAILED", "INVALID_CODE", request, null, clientIp, userAgent);
            throw new AttendanceValidationException("Invalid attendance code.", "INVALID_CODE");
        }

        AttendanceSession session = sessionRepository.findBySessionCodeIgnoreCase(code)
                .orElse(null);

        if (session == null) {
            logAttempt(null, code, usn, "FAILED", "INVALID_CODE", request, null, clientIp, userAgent);
            throw new AttendanceValidationException("Invalid attendance code.", "INVALID_CODE");
        }

        // 2. Check Session Expiration and Status
        if (session.getStatus() != AttendanceSession.SessionStatus.ACTIVE || session.isExpired()) {
            if (session.getStatus() == AttendanceSession.SessionStatus.ACTIVE) {
                session.setStatus(AttendanceSession.SessionStatus.EXPIRED);
                sessionRepository.save(session);
            }
            logAttempt(session.getId(), code, usn, "FAILED", "SESSION_EXPIRED", request, null, clientIp, userAgent);
            throw new AttendanceValidationException("This attendance session has expired.", "SESSION_EXPIRED");
        }

        // 3. Validate Student Enrollment and USN
        if (usn == null || usn.isEmpty()) {
            logAttempt(session.getId(), code, usn, "FAILED", "INVALID_USN", request, null, clientIp, userAgent);
            throw new AttendanceValidationException("USN cannot be empty.", "INVALID_USN");
        }

        Student student = studentRepository.findByUsnIgnoreCase(usn).orElse(null);
        if (student == null) {
            logAttempt(session.getId(), code, usn, "FAILED", "STUDENT_NOT_FOUND", request, null, clientIp, userAgent);
            throw new AttendanceValidationException("You are not in the class.", "NOT_IN_CLASS");
        }

        // Check if student belongs to the session class
        if (student.getCollegeClass() == null || !student.getCollegeClass().getId().equals(session.getCollegeClass().getId())) {
            logAttempt(session.getId(), code, usn, "FAILED", "WRONG_CLASS", request, null, clientIp, userAgent);
            throw new AttendanceValidationException("You are not in the class.", "NOT_IN_CLASS");
        }

        // 4. Check Duplicate Attendance
        if (recordRepository.existsBySessionIdAndStudentId(session.getId(), student.getId())) {
            logAttempt(session.getId(), code, usn, "FAILED", "ALREADY_MARKED", request, null, clientIp, userAgent);
            throw new AttendanceValidationException("Attendance has already been marked.", "ALREADY_MARKED");
        }

        // 5. GPS Location & Geofencing Verification (15-Meter Geofence)
        LocationVerificationService.VerificationResult verificationResult;
        try {
            verificationResult = locationService.verifyLocation(
                    session.getTeacherLatitude(),
                    session.getTeacherLongitude(),
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getAccuracy(),
                    session.getGeofenceRadiusMeters()
            );
        } catch (AttendanceValidationException ex) {
            logAttempt(session.getId(), code, usn, "FAILED", ex.getErrorCode(), request, ex.getCalculatedDistance(), clientIp, userAgent);
            throw ex;
        } catch (Exception ex) {
            logAttempt(session.getId(), code, usn, "FAILED", "LOCATION_ERROR", request, null, clientIp, userAgent);
            throw new AttendanceValidationException("Unable to verify your location. Please try again.", "LOCATION_ERROR");
        }

        // 6. Save Attendance Record (Status PRESENT)
        AttendanceRecord record = new AttendanceRecord(
                session,
                student,
                AttendanceRecord.AttendanceStatus.PRESENT,
                request.getLatitude(),
                request.getLongitude(),
                request.getAccuracy(),
                verificationResult.getDistanceMeters()
        );

        recordRepository.save(record);

        // 7. Log Success Attempt for Audit
        logAttempt(session.getId(), code, usn, "SUCCESS", null, request, verificationResult.getDistanceMeters(), clientIp, userAgent);

        // 8. Broadcast Real-Time STOMP Update to Teacher Dashboard
        try {
            long presentCount = recordRepository.countBySessionId(session.getId());
            long totalEnrolled = studentRepository.findByCollegeClassId(session.getCollegeClass().getId()).size();
            double currentPercentage = totalEnrolled > 0 ? ((double) presentCount / totalEnrolled) * 100.0 : 0.0;

            LiveAttendanceUpdateDto liveUpdate = new LiveAttendanceUpdateDto(
                    session.getId(),
                    session.getSessionCode(),
                    student.getUsn(),
                    student.getName(),
                    "PRESENT",
                    verificationResult.getFormattedDistance(),
                    presentCount,
                    totalEnrolled,
                    Math.round(currentPercentage * 100.0) / 100.0,
                    LocalDateTime.now()
            );

            messagingTemplate.convertAndSend("/topic/session/" + session.getId(), liveUpdate);
            messagingTemplate.convertAndSend("/topic/sessions", liveUpdate);
        } catch (Exception wsEx) {
            // Non-blocking if WebSocket broadcast encounters client disconnect
        }

        // 9. Construct Response DTO
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return new AttendanceResultDto(
                student.getName(),
                student.getUsn(),
                student.getDepartment().getName(),
                session.getCollegeClass().getSemester(),
                session.getCollegeClass().getSection(),
                session.getSubject().getName(),
                LocalDateTime.now().format(dtf),
                verificationResult.getFormattedDistance(),
                verificationResult.getDistanceMeters(),
                request.getAccuracy(),
                "PRESENT"
        );
    }

    public List<AttendanceRecord> getSessionRecords(String sessionId) {
        return recordRepository.findBySessionIdOrderByMarkedAtDesc(sessionId);
    }

    private void logAttempt(String sessionId, String sessionCode, String usn,
                            String status, String failureReason,
                            MarkAttendanceRequest req, Double distance,
                            String ip, String userAgent) {
        try {
            AttendanceAttemptLog log = new AttendanceAttemptLog(
                    sessionId,
                    sessionCode,
                    usn,
                    status,
                    failureReason,
                    req != null ? req.getLatitude() : null,
                    req != null ? req.getLongitude() : null,
                    req != null ? req.getAccuracy() : null,
                    distance,
                    ip,
                    userAgent
            );
            attemptLogRepository.save(log);
        } catch (Exception e) {
            // Best-effort attempt logging
        }
    }
}
