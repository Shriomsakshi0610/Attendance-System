package com.renaissance.service;

import com.renaissance.dto.CreateSessionRequest;
import com.renaissance.dto.MarkAttendanceRequest;
import com.renaissance.dto.SessionDto;
import com.renaissance.entity.*;
import com.renaissance.exception.AttendanceValidationException;
import com.renaissance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionFiveMinuteWindowTest {

    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private AttendanceAttemptLogRepository attemptLogRepository;
    @Mock
    private AuthService authService;

    private SessionService sessionService;
    private AttendanceService attendanceService;
    private LocationVerificationService locationService;

    private Teacher testTeacher;
    private CollegeClass testClass;
    private Subject testSubject;
    private Student testStudent;
    private AttendanceSession expiredSession;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
                sessionRepository,
                classRepository,
                subjectRepository,
                studentRepository,
                recordRepository,
                authService
        );

        locationService = new LocationVerificationService();
        ReflectionTestUtils.setField(locationService, "defaultRadiusMeters", 40.0);
        ReflectionTestUtils.setField(locationService, "maxAllowedAccuracyMeters", 100.0);

        attendanceService = new AttendanceService(
                sessionRepository,
                studentRepository,
                recordRepository,
                attemptLogRepository,
                locationService,
                null
        );

        Department dept = new Department("CSE", "Computer Science", "CSE Dept");
        dept.setId("dept-1");

        testTeacher = new Teacher("Dr. Alan Turing", "turing@renaissance.edu", "pwd", "EMP001", dept, "ROLE_TEACHER");
        testTeacher.setId("teacher-1");

        testClass = new CollegeClass(dept, 5, "A", "2022-2026");
        testClass.setId("class-1");

        testSubject = new Subject("CS501", "Algorithms", dept, 5);
        testSubject.setId("subj-1");

        testStudent = new Student("2GI24IS042", "Aarav Sharma", "aarav@renaissance.edu", dept, testClass);
        testStudent.setId("stud-1");
        testStudent.setStatus("ACTIVE");

        // Session created 6 minutes ago -> expired past 5-minute window
        LocalDateTime sixMinutesAgo = LocalDateTime.now().minusMinutes(6);
        expiredSession = new AttendanceSession();
        expiredSession.setId("sess-expired");
        expiredSession.setSessionCode("EXPIRE");
        expiredSession.setTeacher(testTeacher);
        expiredSession.setCollegeClass(testClass);
        expiredSession.setSubject(testSubject);
        expiredSession.setTeacherLatitude(12.9716);
        expiredSession.setTeacherLongitude(77.5946);
        expiredSession.setGeofenceRadiusMeters(40.0);
        expiredSession.setStartTime(sixMinutesAgo);
        expiredSession.setEndTime(sixMinutesAgo.plusMinutes(5));
        expiredSession.setStatus(AttendanceSession.SessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("createAndStartSession enforces exactly 5-minute window (endTime = startTime + 5 min)")
    void testCreateSessionDurationIsExactlyFiveMinutes() {
        CreateSessionRequest req = new CreateSessionRequest(
                "class-1",
                "subj-1",
                12.9716,
                77.5946,
                40.0,
                5
        );

        when(authService.getCurrentTeacher()).thenReturn(testTeacher);
        when(classRepository.findById("class-1")).thenReturn(Optional.of(testClass));
        when(subjectRepository.findById("subj-1")).thenReturn(Optional.of(testSubject));
        when(sessionRepository.findActiveConflictingSession(any(), any(), any())).thenReturn(Optional.empty());
        when(sessionRepository.findBySessionCodeIgnoreCase(any())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(AttendanceSession.class))).thenAnswer(inv -> {
            AttendanceSession s = inv.getArgument(0);
            s.setId("new-session-id");
            return s;
        });

        SessionDto dto = sessionService.createAndStartSession(req);

        assertNotNull(dto);
        assertNotNull(dto.getStartTime());
        assertNotNull(dto.getEndTime());

        // Verify the duration between startTime and endTime is strictly 5 minutes
        long durationMinutes = Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
        assertEquals(5, durationMinutes, "Session window must be strictly 5 minutes");

        // Verify remainingSeconds is approximately 300 seconds
        assertTrue(dto.getRemainingSeconds() <= 300 && dto.getRemainingSeconds() >= 295,
                "Remaining seconds must start close to 300s");
        assertNotNull(dto.getServerTime());
    }

    @Test
    @DisplayName("Attendance submitted after 5-minute window is rejected with SESSION_EXPIRED")
    void testAttendanceSubmissionAfterFiveMinutesIsRejected() {
        MarkAttendanceRequest req = new MarkAttendanceRequest(
                "2GI24IS042",
                "EXPIRE",
                "class-1",
                12.9716,
                77.5946,
                10.0
        );

        when(sessionRepository.findBySessionCodeIgnoreCase("EXPIRE")).thenReturn(Optional.of(expiredSession));

        AttendanceValidationException ex = assertThrows(AttendanceValidationException.class, () ->
                attendanceService.markAttendance(req, null)
        );

        assertEquals("SESSION_EXPIRED", ex.getErrorCode(), "Must reject with SESSION_EXPIRED error code");
    }

    @Test
    @DisplayName("Active session within 5-minute window returns positive remainingSeconds")
    void testActiveSessionWithinFiveMinutesHasPositiveRemainingSeconds() {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        AttendanceSession activeSess = new AttendanceSession();
        activeSess.setId("sess-active");
        activeSess.setSessionCode("ACTIVE");
        activeSess.setTeacher(testTeacher);
        activeSess.setCollegeClass(testClass);
        activeSess.setSubject(testSubject);
        activeSess.setTeacherLatitude(12.9716);
        activeSess.setTeacherLongitude(77.5946);
        activeSess.setGeofenceRadiusMeters(40.0);
        activeSess.setStartTime(oneMinuteAgo);
        activeSess.setEndTime(oneMinuteAgo.plusMinutes(5));
        activeSess.setStatus(AttendanceSession.SessionStatus.ACTIVE);

        when(sessionRepository.findBySessionCodeIgnoreCase("ACTIVE")).thenReturn(Optional.of(activeSess));

        SessionDto dto = sessionService.getSessionByCode("ACTIVE");

        assertNotNull(dto);
        assertTrue(dto.getRemainingSeconds() > 200 && dto.getRemainingSeconds() < 250,
                "Remaining seconds should be around 240s");
    }
}
