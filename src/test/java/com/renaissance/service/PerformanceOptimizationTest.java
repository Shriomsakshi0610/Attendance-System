package com.renaissance.service;

import com.renaissance.config.CacheConfig;
import com.renaissance.dto.DashboardStatsDto;
import com.renaissance.dto.SessionDto;
import com.renaissance.entity.*;
import com.renaissance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PerformanceOptimizationTest {

    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private AuthService authService;
    @Mock
    private TeacherService teacherService;
    @Mock
    private TeacherRepository teacherRepository;

    private ConcurrentMapCacheManager cacheManager;
    private SessionService sessionService;
    private ReportService reportService;

    private Teacher teacherA;
    private Teacher teacherB;
    private CollegeClass class5A;
    private Subject dbmsSubject;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager(
                CacheConfig.CACHE_DEPARTMENTS,
                CacheConfig.CACHE_CLASSES,
                CacheConfig.CACHE_SUBJECTS,
                CacheConfig.CACHE_TIMETABLE,
                CacheConfig.CACHE_DASHBOARD_STATS
        );

        sessionService = new SessionService(
                sessionRepository,
                classRepository,
                subjectRepository,
                studentRepository,
                recordRepository,
                authService
        );

        reportService = new ReportService(
                studentRepository,
                sessionRepository,
                recordRepository,
                classRepository,
                subjectRepository,
                sessionService,
                authService,
                teacherService,
                teacherRepository
        );
        reportService.setCacheManager(cacheManager);

        Department cse = new Department("CSE", "Computer Science");
        cse.setId("dept_cse");

        teacherA = new Teacher("Prof. Alan Turing", "turing@renaissance.edu", "pass123", "EMP001", cse, "ROLE_TEACHER");
        teacherA.setId("teacher_a_id");

        teacherB = new Teacher("Prof. Grace Hopper", "hopper@renaissance.edu", "pass456", "EMP002", cse, "ROLE_TEACHER");
        teacherB.setId("teacher_b_id");

        class5A = new CollegeClass(cse, 5, "A", "2024-2028");
        class5A.setId("class_5a_id");

        dbmsSubject = new Subject("21CS51", "Database Systems", cse, 5);
        dbmsSubject.setId("subj_dbms_id");
    }

    @Test
    @DisplayName("Active Session Fetching: Returns only active non-expired sessions using batch count mapping")
    void testGetActiveSessions_returnsActiveSessionsWithBatchCounts() {
        LocalDateTime now = LocalDateTime.now();

        AttendanceSession activeSession = new AttendanceSession(
                "CODE01", teacherA, class5A, dbmsSubject,
                12.9716, 77.5946, 10.0, 40.0,
                now.minusMinutes(5), now.plusMinutes(10)
        );
        activeSession.setId("session_active_1");
        activeSession.setStatus(AttendanceSession.SessionStatus.ACTIVE);

        AttendanceSession expiredSession = new AttendanceSession(
                "CODE02", teacherA, class5A, dbmsSubject,
                12.9716, 77.5946, 10.0, 40.0,
                now.minusMinutes(30), now.minusMinutes(15)
        );
        expiredSession.setId("session_expired_2");
        expiredSession.setStatus(AttendanceSession.SessionStatus.ACTIVE); // but end time is in the past!

        when(sessionRepository.findByStatus(AttendanceSession.SessionStatus.ACTIVE))
                .thenReturn(Arrays.asList(activeSession, expiredSession));

        when(studentRepository.countStudentsByClassIds(anyCollection()))
                .thenReturn(Map.of("class_5a_id", 45L));
        when(recordRepository.countRecordsBySessionIds(anyCollection()))
                .thenReturn(Map.of("session_active_1", 12L));

        List<SessionDto> result = sessionService.getActiveSessions();

        assertNotNull(result);
        assertEquals(1, result.size(), "Only the non-expired active session should be returned");
        assertEquals("CODE01", result.get(0).getSessionCode());
        assertEquals(45L, result.get(0).getTotalEnrolled());
        assertEquals(12L, result.get(0).getPresentCount());

        // Verify batch queries were called instead of loop count queries
        verify(studentRepository, times(1)).countStudentsByClassIds(anyCollection());
        verify(recordRepository, times(1)).countRecordsBySessionIds(anyCollection());
        verify(studentRepository, never()).countByCollegeClassId(anyString());
        verify(recordRepository, never()).countBySessionId(anyString());
    }

    @Test
    @DisplayName("Session Count Accuracy: Historical session list fetches counts via batch aggregation without N+1 loops")
    void testGetTeacherSessions_batchAggregationEliminatesNPlusOne() {
        when(authService.getCurrentTeacher()).thenReturn(teacherA);

        List<AttendanceSession> sessions = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            AttendanceSession s = new AttendanceSession(
                    "SESS" + i, teacherA, class5A, dbmsSubject,
                    12.97, 77.59, 10.0, 40.0,
                    LocalDateTime.now().minusDays(i), LocalDateTime.now().minusDays(i).plusMinutes(45)
            );
            s.setId("sess_id_" + i);
            s.setStatus(AttendanceSession.SessionStatus.COMPLETED);
            sessions.add(s);
        }

        when(sessionRepository.findByTeacherIdOrderByCreatedAtDesc("teacher_a_id")).thenReturn(sessions);

        when(studentRepository.countStudentsByClassIds(anyCollection()))
                .thenReturn(Map.of("class_5a_id", 60L));

        Map<String, Long> mockSessionCounts = new HashMap<>();
        for (int i = 1; i <= 20; i++) {
            mockSessionCounts.put("sess_id_" + i, (long) (i * 2));
        }
        when(recordRepository.countRecordsBySessionIds(anyCollection())).thenReturn(mockSessionCounts);

        List<SessionDto> dtos = sessionService.getTeacherSessions();

        assertEquals(20, dtos.size());
        assertEquals(60L, dtos.get(0).getTotalEnrolled());
        assertEquals(2L, dtos.get(0).getPresentCount());
        assertEquals(40L, dtos.get(19).getPresentCount());

        // Verify that 20 sessions triggered EXACTLY ONE batch student query and ONE batch record query
        verify(studentRepository, times(1)).countStudentsByClassIds(anyCollection());
        verify(recordRepository, times(1)).countRecordsBySessionIds(anyCollection());
        verify(studentRepository, never()).countByCollegeClassId(anyString());
        verify(recordRepository, never()).countBySessionId(anyString());
    }

    @Test
    @DisplayName("Faculty Data Isolation: Teacher A sees only their assigned classes, subjects, and stats; never Teacher B")
    void testFacultyDataIsolation_dashboardStatsIsolatedPerTeacher() {
        teacherA.setAssignedClasses(new ArrayList<>(List.of(class5A)));
        teacherA.setAssignedSubjects(new ArrayList<>(List.of(dbmsSubject)));

        when(authService.getCurrentTeacher()).thenReturn(teacherA);
        when(teacherRepository.findById("teacher_a_id")).thenReturn(Optional.of(teacherA));

        when(teacherService.getClassesForTeacher(teacherA, null))
                .thenReturn(List.of(new com.renaissance.dto.ClassDto("class_5a_id", "dept_cse", "CSE", "Computer Science", 5, "A", "2024-2028")));
        when(teacherService.getSubjectsForTeacher(teacherA, null, null))
                .thenReturn(List.of(new com.renaissance.dto.SubjectDto("subj_dbms_id", "21CS51", "Database Systems", "dept_cse", "CSE", 5)));

        when(sessionRepository.findByTeacher_IdOrderByCreatedAtDesc("teacher_a_id"))
                .thenReturn(Collections.emptyList());

        Student s1 = mock(Student.class);
        when(s1.getId()).thenReturn("stud_1");
        when(studentRepository.findByCollegeClassId("class_5a_id")).thenReturn(List.of(s1));

        DashboardStatsDto statsA = reportService.getDashboardStats();

        assertNotNull(statsA);
        assertEquals(1, statsA.getClassesCount());
        assertEquals(1, statsA.getSubjectsCount());
        assertEquals(1, statsA.getTotalStudents());

        // Verify cache is populated under Teacher A's isolated key
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS);
        assertNotNull(cache);
        assertNotNull(cache.get("TEACHER_teacher_a_id"));
        assertNull(cache.get("TEACHER_teacher_b_id"), "Teacher B must have NO entry in Teacher A's cache slot");
    }

    @Test
    @DisplayName("Cache Invalidation: Dashboard stats cache is evicted when requested for a specific teacher")
    void testCacheInvalidation_evictsCorrectTeacherCacheEntry() {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS);
        assertNotNull(cache);

        DashboardStatsDto statsA = new DashboardStatsDto(50, 10, 92.5, 45, 0);
        DashboardStatsDto statsB = new DashboardStatsDto(30, 5, 88.0, 25, 0);

        cache.put("TEACHER_teacher_a_id", statsA);
        cache.put("TEACHER_teacher_b_id", statsB);

        // Evict only Teacher A's cache entry
        reportService.evictDashboardStats("teacher_a_id");

        assertNull(cache.get("TEACHER_teacher_a_id"), "Teacher A's cache should be evicted");
        assertNotNull(cache.get("TEACHER_teacher_b_id"), "Teacher B's cache must remain intact");
    }

    @Test
    @DisplayName("Attendance Consistency: Dashboard stats reflect batch calculated attendance and average percentage")
    void testAttendanceConsistency_percentageCalculatedAccurately() {
        when(authService.getCurrentTeacher()).thenReturn(teacherA);
        when(teacherRepository.findById("teacher_a_id")).thenReturn(Optional.of(teacherA));

        when(teacherService.getClassesForTeacher(teacherA, null)).thenReturn(Collections.emptyList());
        when(teacherService.getSubjectsForTeacher(teacherA, null, null)).thenReturn(Collections.emptyList());

        AttendanceSession s1 = new AttendanceSession("C1", teacherA, class5A, dbmsSubject, 12.0, 77.0, 5.0, 40.0, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusMinutes(45));
        s1.setId("s1_id");
        s1.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        AttendanceSession s2 = new AttendanceSession("C2", teacherA, class5A, dbmsSubject, 12.0, 77.0, 5.0, 40.0, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusMinutes(45));
        s2.setId("s2_id");
        s2.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        when(sessionRepository.findByTeacher_IdOrderByCreatedAtDesc("teacher_a_id")).thenReturn(List.of(s1, s2));

        when(studentRepository.countStudentsByClassIds(anyCollection())).thenReturn(Map.of("class_5a_id", 50L));
        when(recordRepository.countRecordsBySessionIds(anyCollection())).thenReturn(Map.of("s1_id", 40L, "s2_id", 45L));
        when(recordRepository.countPresentBySessionIds(anyCollection())).thenReturn(Map.of("s1_id", 40L, "s2_id", 45L));

        DashboardStatsDto stats = reportService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(2, stats.getTotalClassesConducted());
        // Total expected = 50 + 50 = 100. Total present = 40 + 45 = 85. Average = 85.0%
        assertEquals(85.0, stats.getAverageAttendancePercentage(), 0.01);
    }
}
