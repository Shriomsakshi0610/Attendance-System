package com.renaissance.service;

import com.renaissance.dto.CreateSessionRequest;
import com.renaissance.dto.SessionDto;
import com.renaissance.entity.AttendanceSession;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Subject;
import com.renaissance.entity.Teacher;
import com.renaissance.exception.ResourceNotFoundException;
import com.renaissance.repository.*;
import com.renaissance.util.AttendanceCodeGenerator;
import com.renaissance.util.GeoLocationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private final AttendanceSessionRepository sessionRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AuthService authService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.cache.CacheManager cacheManager;

    @Value("${renaissance.geofence.radius-meters:40.0}")
    private double defaultRadiusMeters;

    public SessionService(AttendanceSessionRepository sessionRepository,
                          ClassRepository classRepository,
                          SubjectRepository subjectRepository,
                          StudentRepository studentRepository,
                          AttendanceRecordRepository recordRepository,
                          AuthService authService) {
        this.sessionRepository = sessionRepository;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.recordRepository = recordRepository;
        this.authService = authService;
    }

    public SessionDto createAndStartSession(CreateSessionRequest request) {
        Teacher teacher = authService.getCurrentTeacher();

        CollegeClass collegeClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with ID: " + request.getClassId()));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));

        // Validate teacher coordinates
        if (!GeoLocationUtils.isValidCoordinate(request.getLatitude(), request.getLongitude())) {
            throw new IllegalArgumentException("Invalid GPS coordinates received from teacher's browser.");
        }

        // Check for conflicting active session for the same class and subject
        LocalDateTime now = LocalDateTime.now();
        Optional<AttendanceSession> conflicting = sessionRepository.findActiveConflictingSession(
                collegeClass.getId(), subject.getId(), now
        );
        if (conflicting.isPresent()) {
            throw new IllegalArgumentException(
                    "An active attendance session (" + conflicting.get().getSessionCode() +
                    ") is already running for " + collegeClass.getDisplayName() + " - " + subject.getName() +
                    ". Please end the existing session before starting a new one."
            );
        }

        // Generate unique attendance code
        String sessionCode;
        int attempts = 0;
        do {
            sessionCode = AttendanceCodeGenerator.generateCode();
            attempts++;
        } while (sessionRepository.findBySessionCodeIgnoreCase(sessionCode).isPresent() && attempts < 10);

        int duration = request.getDurationMinutes() != null && request.getDurationMinutes() > 0 ?
                request.getDurationMinutes() : 15;

        LocalDateTime startTime = now;
        LocalDateTime endTime = startTime.plusMinutes(duration);

        AttendanceSession session = new AttendanceSession(
                sessionCode,
                teacher,
                collegeClass,
                subject,
                request.getLatitude(),
                request.getLongitude(),
                request.getAccuracy(),
                defaultRadiusMeters,
                startTime,
                endTime
        );

        if (Boolean.TRUE.equals(request.getEnableBluetoothVerification())) {
            session.setBluetoothVerificationEnabled(true);
            session.setBleBeaconUuid(java.util.UUID.randomUUID().toString());
            if (request.getBleRssiThreshold() != null) {
                session.setBleRssiThreshold(request.getBleRssiThreshold());
            }
        }

        AttendanceSession saved = sessionRepository.save(session);
        evictTeacherDashboardCache(teacher);
        return toDto(saved);
    }

    public SessionDto endSession(String sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        session.setStatus(AttendanceSession.SessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        AttendanceSession saved = sessionRepository.save(session);
        evictTeacherDashboardCache(saved.getTeacher());
        return toDto(saved);
    }

    private void evictTeacherDashboardCache(Teacher teacher) {
        if (cacheManager != null) {
            org.springframework.cache.Cache cache = cacheManager.getCache(com.renaissance.config.CacheConfig.CACHE_DASHBOARD_STATS);
            if (cache != null) {
                if (teacher != null && teacher.getId() != null) {
                    cache.evict("TEACHER_" + teacher.getId());
                } else {
                    cache.clear();
                }
            }
        }
    }

    public SessionDto getSessionById(String sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));
        return toDto(session);
    }

    public SessionDto getSessionByCode(String sessionCode) {
        AttendanceSession session = sessionRepository.findBySessionCodeIgnoreCase(sessionCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with code: " + sessionCode));
        return toDto(session);
    }

    public List<SessionDto> getTeacherSessions() {
        Teacher teacher = authService.getCurrentTeacher();
        List<AttendanceSession> sessions = sessionRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId());
        return toDtos(sessions);
    }

    public List<SessionDto> getActiveSessions() {
        List<AttendanceSession> active = sessionRepository.findByStatus(AttendanceSession.SessionStatus.ACTIVE);
        List<AttendanceSession> nonExpired = active.stream()
                .filter(s -> !s.isExpired())
                .collect(Collectors.toList());
        return toDtos(nonExpired);
    }

    public List<SessionDto> toDtos(List<AttendanceSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        java.util.Set<String> classIds = new java.util.HashSet<>();
        List<String> sessionIds = new java.util.ArrayList<>(sessions.size());

        for (AttendanceSession session : sessions) {
            if (session.getStatus() == AttendanceSession.SessionStatus.ACTIVE && session.getEndTime() != null && now.isAfter(session.getEndTime())) {
                session.setStatus(AttendanceSession.SessionStatus.EXPIRED);
            }
            if (session.getId() != null) {
                sessionIds.add(session.getId());
            }
            if (session.getCollegeClass() != null && session.getCollegeClass().getId() != null) {
                classIds.add(session.getCollegeClass().getId());
            }
        }

        // Batch count queries via MongoDB aggregation pipelines (eliminates N+1 loop)
        java.util.Map<String, Long> classCounts = classIds.isEmpty() ?
                java.util.Collections.emptyMap() : studentRepository.countStudentsByClassIds(classIds);
        java.util.Map<String, Long> sessionCounts = sessionIds.isEmpty() ?
                java.util.Collections.emptyMap() : recordRepository.countRecordsBySessionIds(sessionIds);

        return sessions.stream().map(session -> {
            CollegeClass c = session.getCollegeClass();
            Subject s = session.getSubject();
            Teacher t = session.getTeacher();

            long enrolledCount = (c != null && c.getId() != null) ? classCounts.getOrDefault(c.getId(), 0L) : 0L;
            long presentCount = (session.getId() != null) ? sessionCounts.getOrDefault(session.getId(), 0L) : 0L;

            return mapSessionDto(session, t, c, s, enrolledCount, presentCount);
        }).collect(Collectors.toList());
    }

    public SessionDto toDto(AttendanceSession session) {
        if (session == null) return null;
        List<SessionDto> dtos = toDtos(java.util.Collections.singletonList(session));
        return dtos.isEmpty() ? null : dtos.get(0);
    }

    private SessionDto mapSessionDto(AttendanceSession session, Teacher t, CollegeClass c, Subject s, long enrolledCount, long presentCount) {
        SessionDto dto = new SessionDto();
        dto.setId(session.getId());
        dto.setSessionCode(session.getSessionCode());
        if (t != null) {
            dto.setTeacherId(t.getId());
            dto.setTeacherName(t.getName());
        }
        if (c != null) {
            dto.setClassId(c.getId());
            dto.setClassName(c.getDisplayName());
            if (c.getDepartment() != null) {
                dto.setDepartmentName(c.getDepartment().getName());
                dto.setDepartmentCode(c.getDepartment().getCode());
            }
            dto.setSemester(c.getSemester());
            dto.setSection(c.getSection());
        }
        if (s != null) {
            dto.setSubjectId(s.getId());
            dto.setSubjectName(s.getName());
            dto.setSubjectCode(s.getCode());
        }
        dto.setTeacherLatitude(session.getTeacherLatitude());
        dto.setTeacherLongitude(session.getTeacherLongitude());
        dto.setGeofenceRadiusMeters(session.getGeofenceRadiusMeters());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setStatus(session.getStatus().name());
        dto.setPresentCount(presentCount);
        dto.setTotalEnrolled(enrolledCount);
        dto.setAttendanceLink("/student-attendance.html?code=" + session.getSessionCode());
        dto.setBluetoothVerificationEnabled(session.isBluetoothVerificationEnabled());
        dto.setBleBeaconUuid(session.getBleBeaconUuid());
        dto.setBleRssiThreshold(session.getBleRssiThreshold());

        return dto;
    }
}
