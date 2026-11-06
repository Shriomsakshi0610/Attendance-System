package com.renaissance.service;

import com.renaissance.dto.MarkAttendanceRequest;
import com.renaissance.entity.*;
import com.renaissance.exception.AttendanceValidationException;
import com.renaissance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private AttendanceAttemptLogRepository attemptLogRepository;
    @Mock
    private org.springframework.messaging.simp.SimpMessageSendingOperations messagingTemplate;

    private LocationVerificationService locationService;
    private AttendanceService attendanceService;

    private AttendanceSession activeSession;
    private Student validStudent;
    private CollegeClass testClass;
    private Department testDept;
    private Subject testSubject;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() {
        locationService = new LocationVerificationService();
        ReflectionTestUtils.setField(locationService, "defaultRadiusMeters", 15.0);
        ReflectionTestUtils.setField(locationService, "maxAllowedAccuracyMeters", 100.0);

        attendanceService = new AttendanceService(
                sessionRepository,
                studentRepository,
                recordRepository,
                attemptLogRepository,
                locationService,
                messagingTemplate
        );

        testDept = new Department("CSE", "Computer Science");
        testDept.setId(1L);

        testClass = new CollegeClass(testDept, 5, "A", "2024-2028");
        testClass.setId(10L);

        testSubject = new Subject("21CS51", "DBMS", testDept, 5);
        testSubject.setId(100L);

        testTeacher = new Teacher("Teacher", "teacher@test.com", "pass", "EMP1", testDept);
        testTeacher.setId(50L);

        activeSession = new AttendanceSession(
                "RN1234",
                testTeacher,
                testClass,
                testSubject,
                13.030500,
                77.565000,
                5.0,
                15.0,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(10)
        );
        activeSession.setId(500L);

        validStudent = new Student("1MS21CS001", "Aarav", "aarav@test.com", testDept, testClass);
        validStudent.setId(1000L);
    }

    @Test
    @DisplayName("Should successfully mark attendance when all validations pass (inside 15m radius)")
    void testSuccessfulAttendance() {
        // Coordinate ~3 meters away from teacher
        MarkAttendanceRequest req = new MarkAttendanceRequest(
                "1MS21CS001",
                "RN1234",
                10L,
                13.030520,
                77.565010,
                4.0
        );

        when(sessionRepository.findBySessionCodeIgnoreCase("RN1234")).thenReturn(Optional.of(activeSession));
        when(studentRepository.findByUsnIgnoreCase("1MS21CS001")).thenReturn(Optional.of(validStudent));
        when(recordRepository.existsBySessionIdAndStudentId(500L, 1000L)).thenReturn(false);
        when(recordRepository.save(any(AttendanceRecord.class))).thenAnswer(i -> i.getArgument(0));

        var response = attendanceService.markAttendance(req, null);

        assertNotNull(response);
        assertEquals("Aarav", response.getStudentName());
        assertEquals("1MS21CS001", response.getUsn());
        assertEquals("PRESENT", response.getAttendanceStatus());
        verify(recordRepository, times(1)).save(any(AttendanceRecord.class));
    }

    @Test
    @DisplayName("Should reject attendance with invalid session code")
    void testInvalidSessionCode() {
        MarkAttendanceRequest req = new MarkAttendanceRequest(
                "1MS21CS001",
                "WRONGCODE",
                10L,
                13.0305,
                77.5650,
                5.0
        );

        when(sessionRepository.findBySessionCodeIgnoreCase("WRONGCODE")).thenReturn(Optional.empty());

        AttendanceValidationException ex = assertThrows(AttendanceValidationException.class, () ->
                attendanceService.markAttendance(req, null)
        );
        assertEquals("Invalid attendance code.", ex.getMessage());
        assertEquals("INVALID_CODE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should strictly reject attendance when student is outside 15m radius (~80 meters away)")
    void testOutsideRadiusRejection() {
        // ~80 meters away from teacher
        MarkAttendanceRequest req = new MarkAttendanceRequest(
                "1MS21CS001",
                "RN1234",
                10L,
                13.031200,
                77.565000,
                5.0
        );

        when(sessionRepository.findBySessionCodeIgnoreCase("RN1234")).thenReturn(Optional.of(activeSession));
        when(studentRepository.findByUsnIgnoreCase("1MS21CS001")).thenReturn(Optional.of(validStudent));
        when(recordRepository.existsBySessionIdAndStudentId(500L, 1000L)).thenReturn(false);

        AttendanceValidationException ex = assertThrows(AttendanceValidationException.class, () ->
                attendanceService.markAttendance(req, null)
        );
        assertEquals("You are outside the 15-meter attendance radius.", ex.getMessage());
        assertEquals("OUTSIDE_RADIUS", ex.getErrorCode());
        assertNotNull(ex.getCalculatedDistance());
        assertTrue(ex.getCalculatedDistance() > 15.0);
    }

    @Test
    @DisplayName("Should reject duplicate attendance submission")
    void testDuplicateAttendance() {
        MarkAttendanceRequest req = new MarkAttendanceRequest(
                "1MS21CS001",
                "RN1234",
                10L,
                13.0305,
                77.5650,
                5.0
        );

        when(sessionRepository.findBySessionCodeIgnoreCase("RN1234")).thenReturn(Optional.of(activeSession));
        when(studentRepository.findByUsnIgnoreCase("1MS21CS001")).thenReturn(Optional.of(validStudent));
        when(recordRepository.existsBySessionIdAndStudentId(500L, 1000L)).thenReturn(true);

        AttendanceValidationException ex = assertThrows(AttendanceValidationException.class, () ->
                attendanceService.markAttendance(req, null)
        );
        assertEquals("Attendance has already been marked.", ex.getMessage());
        assertEquals("ALREADY_MARKED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject attendance for expired session")
    void testExpiredSession() {
        activeSession.setEndTime(LocalDateTime.now().minusMinutes(2));

        MarkAttendanceRequest req = new MarkAttendanceRequest(
                "1MS21CS001",
                "RN1234",
                10L,
                13.0305,
                77.5650,
                5.0
        );

        when(sessionRepository.findBySessionCodeIgnoreCase("RN1234")).thenReturn(Optional.of(activeSession));

        AttendanceValidationException ex = assertThrows(AttendanceValidationException.class, () ->
                attendanceService.markAttendance(req, null)
        );
        assertEquals("This attendance session has expired.", ex.getMessage());
        assertEquals("SESSION_EXPIRED", ex.getErrorCode());
    }
}
