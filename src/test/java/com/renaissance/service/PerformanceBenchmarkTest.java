package com.renaissance.service;

import com.renaissance.config.MongoTestConfig;
import com.renaissance.dto.DashboardStatsDto;
import com.renaissance.dto.SessionDto;
import com.renaissance.dto.StudentDto;
import com.renaissance.entity.*;
import com.renaissance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Import(MongoTestConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class PerformanceBenchmarkTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceRecordRepository recordRepository;

    private Department dept;
    private CollegeClass cseClass;
    private Subject subject;
    private Teacher teacher;

    @BeforeEach
    void setUpData() {
        recordRepository.deleteAll();
        sessionRepository.deleteAll();
        studentRepository.deleteAll();
        subjectRepository.deleteAll();
        classRepository.deleteAll();
        departmentRepository.deleteAll();
        teacherRepository.deleteAll();

        dept = departmentRepository.save(new Department("CSE", "Computer Science", "CSE Dept", "ACTIVE"));
        teacher = teacherRepository.save(new Teacher("Prof. Test", "prof.test@renaissance.edu", "pass", "EMP099", dept));
        cseClass = classRepository.save(new CollegeClass(dept, 5, "A", "2024-2028", "2026-2027", teacher));
        subject = subjectRepository.save(new Subject("21CS51", "Database Systems", dept, 5, cseClass, teacher, 4));

        teacher.setAssignedClasses(List.of(cseClass));
        teacher.setAssignedSubjects(List.of(subject));
        teacherRepository.save(teacher);

        // Authenticate as test teacher
        com.renaissance.security.SecurityUser userDetails = new com.renaissance.security.SecurityUser(teacher);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Seed 50 students
        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            students.add(new Student(String.format("1MS21CS%03d", i), "Student " + i, "s" + i + "@test.com", dept, cseClass));
        }
        studentRepository.saveAll(students);

        // Seed 10 sessions with 20 attendance records each = 200 attendance records
        for (int s = 1; s <= 10; s++) {
            AttendanceSession session = new AttendanceSession("SESS" + s, teacher, cseClass, subject,
                    12.9716, 77.5946, 5.0, 15.0, LocalDateTime.now().minusDays(s), LocalDateTime.now().minusDays(s).plusMinutes(50));
            session.setStatus(AttendanceSession.SessionStatus.COMPLETED);
            AttendanceSession savedSession = sessionRepository.save(session);

            List<AttendanceRecord> records = new ArrayList<>();
            for (int r = 0; r < 20; r++) {
                records.add(new AttendanceRecord(savedSession, students.get(r), AttendanceRecord.AttendanceStatus.PRESENT,
                        12.9716, 77.5946, 5.0, 3.2));
            }
            recordRepository.saveAll(records);
        }
    }

    @Test
    void benchmarkAllEndpoints() {
        // Warm up JVM
        studentService.getAllStudents(null, null, null);
        reportService.getDashboardStats();

        // 1. Measure Student Listing
        long start = System.nanoTime();
        List<StudentDto> students = studentService.getAllStudents(null, null, null);
        long studentListingTimeMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[BENCHMARK] studentService.getAllStudents (50 students): " + studentListingTimeMs + " ms");
        assertNotNull(students);
        org.junit.jupiter.api.Assertions.assertEquals(50, students.size());

        // 2. Measure Dashboard Stats
        start = System.nanoTime();
        DashboardStatsDto stats = reportService.getDashboardStats();
        long dashboardStatsTimeMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[BENCHMARK] reportService.getDashboardStats: " + dashboardStatsTimeMs + " ms");
        assertNotNull(stats);
        org.junit.jupiter.api.Assertions.assertEquals(50, stats.getTotalStudents());
        org.junit.jupiter.api.Assertions.assertEquals(10, stats.getTotalClassesConducted());

        // 3. Measure Attendance Summary
        start = System.nanoTime();
        var summary = reportService.getAttendanceSummary(cseClass.getId(), subject.getId());
        long attendanceSummaryTimeMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[BENCHMARK] reportService.getAttendanceSummary: " + attendanceSummaryTimeMs + " ms");
        assertNotNull(summary);
        org.junit.jupiter.api.Assertions.assertEquals(50, summary.size());

        // 4. Measure Teacher Sessions
        start = System.nanoTime();
        List<SessionDto> sessions = sessionService.getTeacherSessions();
        long teacherSessionsTimeMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[BENCHMARK] sessionService.getTeacherSessions: " + teacherSessionsTimeMs + " ms");
        assertNotNull(sessions);
        org.junit.jupiter.api.Assertions.assertEquals(10, sessions.size());

        // 5. Measure Metadata Listing (Departments, Classes, Subjects)
        start = System.nanoTime();
        teacherService.getAllDepartments();
        teacherService.getClassesByDepartment(null);
        teacherService.getSubjects(null, null);
        long metadataTimeMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[BENCHMARK] teacherService metadata (3 calls): " + metadataTimeMs + " ms");

        // 6. Measure Metadata Cache Hit
        start = System.nanoTime();
        teacherService.getAllDepartments();
        teacherService.getClassesByDepartment(null);
        teacherService.getSubjects(null, null);
        long cachedMetadataTimeMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[BENCHMARK] teacherService cached metadata (3 cache hits): " + cachedMetadataTimeMs + " ms");
    }
}
