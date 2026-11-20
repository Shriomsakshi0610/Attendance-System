package com.renaissance.service;

import com.renaissance.config.CacheConfig;
import com.renaissance.dto.ClassDto;
import com.renaissance.dto.DashboardStatsDto;
import com.renaissance.dto.SubjectDto;
import com.renaissance.entity.AttendanceSession;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import com.renaissance.entity.Subject;
import com.renaissance.entity.Teacher;
import com.renaissance.event.TeacherLoginEvent;
import com.renaissance.repository.AttendanceRecordRepository;
import com.renaissance.repository.AttendanceSessionRepository;
import com.renaissance.repository.ClassRepository;
import com.renaissance.repository.StudentRepository;
import com.renaissance.repository.SubjectRepository;
import com.renaissance.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherDashboardPreWarmTest {

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
    @Mock
    private TimetableService timetableService;

    private ConcurrentMapCacheManager cacheManager;
    private SessionService sessionService;
    private ReportService reportService;
    private CacheWarmUpService cacheWarmUpService;

    private Teacher teacher;
    private CollegeClass class5A;
    private Subject dbmsSubject;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager(
                CacheConfig.CACHE_DEPARTMENTS,
                CacheConfig.CACHE_CLASSES,
                CacheConfig.CACHE_SUBJECTS,
                CacheConfig.CACHE_TIMETABLE,
                CacheConfig.CACHE_DASHBOARD_STATS,
                CacheConfig.CACHE_TEACHER_CLASSES,
                CacheConfig.CACHE_TEACHER_SUBJECTS
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

        cacheWarmUpService = new CacheWarmUpService(
                teacherService,
                reportService,
                timetableService
        );

        Department dept = new Department("CSE", "Computer Science");
        dept.setId("dept_cse");

        class5A = new CollegeClass(dept, 5, "A", "2024-2028");
        class5A.setId("class_5a");

        dbmsSubject = new Subject("Database Systems", "21CS51", dept, 5);
        dbmsSubject.setId("subj_dbms");

        teacher = new Teacher();
        teacher.setId("teacher_ada");
        teacher.setName("Dr. Ada Lovelace");
        teacher.setEmail("ada@renaissance.edu");
        teacher.setRole("ROLE_TEACHER");
        teacher.setAssignedClasses(new ArrayList<>(List.of(class5A)));
        teacher.setAssignedSubjects(new ArrayList<>(List.of(dbmsSubject)));
    }

    @Test
    @DisplayName("Pre-Warm: warmDashboardStats stores computed stats into CACHE_DASHBOARD_STATS")
    void testWarmDashboardStatsDirectly() {
        when(teacherRepository.findById("teacher_ada")).thenReturn(Optional.of(teacher));
        when(teacherService.getClassesForTeacher(teacher, null))
                .thenReturn(List.of(new ClassDto("class_5a", "dept_cse", "CSE", "Computer Science", 5, "A", "2024-2028")));
        when(teacherService.getSubjectsForTeacher(teacher, null, null))
                .thenReturn(List.of(new SubjectDto("subj_dbms", "21CS51", "Database Systems", "dept_cse", "CSE", 5)));
        when(studentRepository.findByCollegeClassIdIn(any())).thenReturn(Collections.emptyList());
        when(sessionRepository.findByTeacher_IdOrderByCreatedAtDesc("teacher_ada")).thenReturn(Collections.emptyList());

        // Warm up cache
        reportService.warmDashboardStats("teacher_ada");

        Cache cache = cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS);
        assertNotNull(cache);
        DashboardStatsDto cached = cache.get("TEACHER_teacher_ada", DashboardStatsDto.class);
        assertNotNull(cached, "Cache must contain pre-warmed dashboard stats for teacher");
        assertEquals(1, cached.getAssignedClasses());
        assertEquals(1, cached.getAssignedSubjects());

        // Now calling getDashboardStats should serve from RAM instantly
        when(authService.getCurrentTeacher()).thenReturn(teacher);
        long start = System.nanoTime();
        DashboardStatsDto result = reportService.getDashboardStats();
        long elapsedNanos = System.nanoTime() - start;

        assertNotNull(result);
        assertEquals(cached, result, "Returned stats should match cached entry");
        System.out.println("[BENCHMARK] Warm cache retrieval duration: " + (elapsedNanos / 1_000_000.0) + " ms");
    }

    @Test
    @DisplayName("Event Listener: TeacherLoginEvent invokes warmTeacherDashboard successfully")
    void testTeacherLoginEventTriggersCachePreWarm() throws InterruptedException {
        when(teacherRepository.findById("teacher_ada")).thenReturn(Optional.of(teacher));
        when(teacherService.getClassesForTeacher(teacher, null))
                .thenReturn(List.of(new ClassDto("class_5a", "dept_cse", "CSE", "Computer Science", 5, "A", "2024-2028")));
        when(teacherService.getSubjectsForTeacher(teacher, null, null))
                .thenReturn(List.of(new SubjectDto("subj_dbms", "21CS51", "Database Systems", "dept_cse", "CSE", 5)));
        when(studentRepository.findByCollegeClassIdIn(any())).thenReturn(Collections.emptyList());
        when(sessionRepository.findByTeacher_IdOrderByCreatedAtDesc("teacher_ada")).thenReturn(Collections.emptyList());

        TeacherLoginEvent event = new TeacherLoginEvent(teacher);
        cacheWarmUpService.onTeacherLogin(event);

        // Allow async thread pool to execute
        Thread.sleep(150);

        Cache cache = cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS);
        assertNotNull(cache);
        assertNotNull(cache.get("TEACHER_teacher_ada"), "Pre-warm must populate dashboard stats in RAM on login event");
        verify(timetableService).getSchedulesByTeacher("teacher_ada");
    }

    @Test
    @DisplayName("Eviction: evictDashboardStats immediately clears teacher entry preventing stale reads")
    void testEvictDashboardStats() {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS);
        assertNotNull(cache);

        DashboardStatsDto initial = new DashboardStatsDto(40, 12, 95.0, 38, 1);
        cache.put("TEACHER_teacher_ada", initial);

        assertNotNull(cache.get("TEACHER_teacher_ada"));

        reportService.evictDashboardStats("teacher_ada");

        assertNull(cache.get("TEACHER_teacher_ada"), "Teacher cache entry must be evicted immediately upon mutation");
    }
}
