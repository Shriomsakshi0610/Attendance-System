package com.renaissance.service;

import com.renaissance.dto.StudentProfileDto;
import com.renaissance.entity.AttendanceRecord;
import com.renaissance.entity.AttendanceSession;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import com.renaissance.entity.Student;
import com.renaissance.entity.Subject;
import com.renaissance.entity.Teacher;
import com.renaissance.exception.UnauthorizedException;
import com.renaissance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubjectSpecificAttendanceHistoryTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private SessionService sessionService;
    @Mock
    private AuthService authService;
    @Mock
    private TeacherService teacherService;
    @Mock
    private TeacherRepository teacherRepository;

    private ReportService reportService;

    private Department dept;
    private CollegeClass classA;
    private CollegeClass classB;
    private Subject spmSubject;
    private Subject iotSubject;
    private Teacher spmTeacher;
    private Student student1;

    private AttendanceSession spmSession1;
    private AttendanceSession spmSession2;
    private AttendanceSession iotSession1;

    private AttendanceRecord spmRecord1;
    private AttendanceRecord iotRecord1;

    @BeforeEach
    void setUp() {
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

        dept = new Department("CSE", "Computer Science and Engineering");
        dept.setId("dept-1");

        classA = new CollegeClass(dept, 6, "A", "2024-2028");
        classA.setId("class-A");

        classB = new CollegeClass(dept, 6, "B", "2024-2028");
        classB.setId("class-B");

        spmSubject = new Subject("CS601", "Software Project Management", dept, 6);
        spmSubject.setId("subj-spm");

        iotSubject = new Subject("CS602", "Internet of Things", dept, 6);
        iotSubject.setId("subj-iot");

        student1 = new Student();
        student1.setId("stud-1");
        student1.setUsn("1MS21CS001");
        student1.setName("Alice Smith");
        student1.setEmail("alice@test.com");
        student1.setDepartment(dept);
        student1.setCollegeClass(classA);

        spmTeacher = new Teacher("SPM Prof", "spm@test.com", "hash", "EMP01", dept, "ROLE_TEACHER");
        spmTeacher.setId("teacher-spm");
        spmTeacher.setAssignedClasses(new ArrayList<>(Collections.singletonList(classA)));
        spmTeacher.setAssignedSubjects(new ArrayList<>(Collections.singletonList(spmSubject)));

        // SPM Sessions
        spmSession1 = new AttendanceSession();
        spmSession1.setId("sess-spm-1");
        spmSession1.setSessionCode("CODE01");
        spmSession1.setTeacher(spmTeacher);
        spmSession1.setCollegeClass(classA);
        spmSession1.setSubject(spmSubject);
        spmSession1.setStartTime(LocalDateTime.now().minusDays(2));
        spmSession1.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        spmSession2 = new AttendanceSession();
        spmSession2.setId("sess-spm-2");
        spmSession2.setSessionCode("CODE02");
        spmSession2.setTeacher(spmTeacher);
        spmSession2.setCollegeClass(classA);
        spmSession2.setSubject(spmSubject);
        spmSession2.setStartTime(LocalDateTime.now().minusDays(1));
        spmSession2.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        // IoT Session
        iotSession1 = new AttendanceSession();
        iotSession1.setId("sess-iot-1");
        iotSession1.setSessionCode("IOT001");
        iotSession1.setCollegeClass(classA);
        iotSession1.setSubject(iotSubject);
        iotSession1.setStartTime(LocalDateTime.now().minusDays(3));
        iotSession1.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        // Student Records: Present in SPM Session 1, Present in IoT Session 1 (Missed SPM Session 2)
        spmRecord1 = new AttendanceRecord();
        spmRecord1.setId("rec-spm-1");
        spmRecord1.setSession(spmSession1);
        spmRecord1.setStudent(student1);
        spmRecord1.setStatus(AttendanceRecord.AttendanceStatus.PRESENT);
        spmRecord1.setSubject(spmSubject);
        spmRecord1.setCollegeClass(classA);
        spmRecord1.setMarkedAt(LocalDateTime.now().minusDays(2));

        iotRecord1 = new AttendanceRecord();
        iotRecord1.setId("rec-iot-1");
        iotRecord1.setSession(iotSession1);
        iotRecord1.setStudent(student1);
        iotRecord1.setStatus(AttendanceRecord.AttendanceStatus.PRESENT);
        iotRecord1.setSubject(iotSubject);
        iotRecord1.setCollegeClass(classA);
        iotRecord1.setMarkedAt(LocalDateTime.now().minusDays(3));
    }

    @Test
    @DisplayName("Requirement 1 & 3: Selecting SPM shows ONLY SPM records (no IoT leakage)")
    void testFilterBySpmSubjectReturnsOnlySpmRecords() {
        when(authService.getCurrentTeacher()).thenReturn(spmTeacher);
        when(studentRepository.findById("stud-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findById("subj-spm")).thenReturn(Optional.of(spmSubject));
        when(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc("class-A", "subj-spm"))
                .thenReturn(Arrays.asList(spmSession1, spmSession2));
        when(recordRepository.findByStudentIdOrderByMarkedAtDesc("stud-1"))
                .thenReturn(Arrays.asList(spmRecord1, iotRecord1));

        StudentProfileDto profile = reportService.getStudentProfile("stud-1", "class-A", "subj-spm");

        assertNotNull(profile);
        assertEquals("stud-1", profile.getId());
        assertEquals("Software Project Management", profile.getFilteredSubjectName());
        assertEquals(2, profile.getTotalConducted()); // 2 SPM sessions
        assertEquals(1, profile.getTotalPresent());   // 1 present in SPM
        assertEquals(1, profile.getTotalAbsent());    // 1 missed in SPM
        assertEquals(50.0, profile.getOverallPercentage());

        // Check History: MUST contain ONLY 1 record (SPM), NEVER IoT!
        assertEquals(1, profile.getAttendanceHistory().size());
        StudentProfileDto.HistoryRecordDto history = profile.getAttendanceHistory().get(0);
        assertEquals("Software Project Management", history.getSubjectName());
        assertEquals("CS601", history.getSubjectCode());
        assertEquals("CODE01", history.getSessionCode());
    }

    @Test
    @DisplayName("Requirement 2: Student has IoT records but 0 SPM sessions -> SPM empty state (no fallback to IoT)")
    void testZeroSpmSessionsReturnsEmptyHistoryAndNeverFallsBackToIot() {
        when(authService.getCurrentTeacher()).thenReturn(spmTeacher);
        when(studentRepository.findById("stud-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findById("subj-spm")).thenReturn(Optional.of(spmSubject));
        when(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc("class-A", "subj-spm"))
                .thenReturn(Collections.emptyList()); // 0 conducted SPM sessions
        when(recordRepository.findByStudentIdOrderByMarkedAtDesc("stud-1"))
                .thenReturn(Collections.singletonList(iotRecord1)); // Student has IoT record

        StudentProfileDto profile = reportService.getStudentProfile("stud-1", "class-A", "subj-spm");

        assertNotNull(profile);
        assertEquals(0, profile.getTotalConducted());
        assertEquals(0, profile.getTotalPresent());
        assertEquals(0, profile.getTotalAbsent());
        assertEquals(100.0, profile.getOverallPercentage());

        // MUST be empty! MUST NEVER fall back to IoT!
        assertTrue(profile.getAttendanceHistory().isEmpty());
    }

    @Test
    @DisplayName("Requirement 4: Authorized teacher for IoT can view IoT records")
    void testAuthorizedTeacherForIotCanViewIotRecords() {
        Teacher iotTeacher = new Teacher("IoT Prof", "iot@test.com", "hash", "EMP02", dept, "ROLE_TEACHER");
        iotTeacher.setId("teacher-iot");
        iotTeacher.setAssignedClasses(new ArrayList<>(Collections.singletonList(classA)));
        iotTeacher.setAssignedSubjects(new ArrayList<>(Collections.singletonList(iotSubject)));

        when(authService.getCurrentTeacher()).thenReturn(iotTeacher);
        when(studentRepository.findById("stud-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findById("subj-iot")).thenReturn(Optional.of(iotSubject));
        when(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc("class-A", "subj-iot"))
                .thenReturn(Collections.singletonList(iotSession1));
        when(recordRepository.findByStudentIdOrderByMarkedAtDesc("stud-1"))
                .thenReturn(Arrays.asList(spmRecord1, iotRecord1));

        StudentProfileDto profile = reportService.getStudentProfile("stud-1", "class-A", "subj-iot");

        assertNotNull(profile);
        assertEquals(1, profile.getTotalConducted());
        assertEquals(1, profile.getTotalPresent());
        assertEquals(0, profile.getTotalAbsent());
        assertEquals(100.0, profile.getOverallPercentage());

        // MUST contain only IoT, zero SPM
        assertEquals(1, profile.getAttendanceHistory().size());
        assertEquals("Internet of Things", profile.getAttendanceHistory().get(0).getSubjectName());
    }

    @Test
    @DisplayName("Requirement 5: All Subjects still returns combined history")
    void testAllSubjectsReturnsCombinedHistory() {
        when(authService.getCurrentTeacher()).thenReturn(spmTeacher);
        when(studentRepository.findById("stud-1")).thenReturn(Optional.of(student1));
        when(sessionRepository.findByCollegeClassIdOrderByStartTimeDesc("class-A"))
                .thenReturn(Arrays.asList(spmSession1, spmSession2, iotSession1));
        when(recordRepository.findByStudentIdOrderByMarkedAtDesc("stud-1"))
                .thenReturn(Arrays.asList(spmRecord1, iotRecord1));

        // When subjectId is null or empty
        StudentProfileDto profile = reportService.getStudentProfile("stud-1", "class-A", null);

        assertNotNull(profile);
        assertEquals(3, profile.getTotalConducted());
        assertEquals(2, profile.getTotalPresent());
        assertEquals(1, profile.getTotalAbsent());
        assertEquals(2, profile.getAttendanceHistory().size());
    }

    @Test
    @DisplayName("Requirement 7: Unauthorized teacher cannot access another teacher's subject")
    void testUnauthorizedTeacherSubjectAccessDenied() {
        // spmTeacher is NOT assigned to iotSubject
        when(authService.getCurrentTeacher()).thenReturn(spmTeacher);
        when(studentRepository.findById("stud-1")).thenReturn(Optional.of(student1));

        assertThrows(UnauthorizedException.class, () -> {
            reportService.getStudentProfile("stud-1", "class-A", "subj-iot");
        });
    }

    @Test
    @DisplayName("Requirement 7B: Unauthorized teacher cannot access a class they are not assigned to")
    void testUnauthorizedTeacherClassAccessDenied() {
        // spmTeacher is assigned to classA, NOT classB
        when(authService.getCurrentTeacher()).thenReturn(spmTeacher);
        when(studentRepository.findById("stud-1")).thenReturn(Optional.of(student1));

        assertThrows(UnauthorizedException.class, () -> {
            reportService.getStudentProfile("stud-1", "class-B", "subj-spm");
        });
    }
}
