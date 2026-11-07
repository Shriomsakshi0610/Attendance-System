package com.renaissance.service;

import com.renaissance.dto.ClassDto;
import com.renaissance.dto.DashboardStatsDto;
import com.renaissance.dto.SubjectDto;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import com.renaissance.entity.Subject;
import com.renaissance.entity.Teacher;
import com.renaissance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyDashboardUserSpecificTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private SessionService sessionService;
    @Mock
    private AuthService authService;

    private TeacherService teacherService;
    private ReportService reportService;

    private Department cseDept;
    private CollegeClass class5A;
    private CollegeClass class5B;
    private Subject dbmsSubject;
    private Subject cnSubject;
    private Teacher newFaculty;

    @BeforeEach
    void setUp() {
        teacherService = new TeacherService(departmentRepository, classRepository, subjectRepository, teacherRepository);
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

        cseDept = new Department("CSE", "Computer Science and Engineering");
        cseDept.setId("dept_cse");

        class5A = new CollegeClass(cseDept, 5, "A", "2024-2028");
        class5A.setId("class_5a");

        class5B = new CollegeClass(cseDept, 5, "B", "2024-2028");
        class5B.setId("class_5b");

        dbmsSubject = new Subject("21CS51", "Database Management Systems", cseDept, 5);
        dbmsSubject.setId("subj_dbms");

        cnSubject = new Subject("21CS52", "Computer Networks", cseDept, 5);
        cnSubject.setId("subj_cn");

        newFaculty = new Teacher("Dr. Ada Lovelace", "ada.lovelace@renaissance.edu", "hashed_pwd", "EMP042", cseDept, "ROLE_TEACHER");
        newFaculty.setId("teacher_ada_42");
    }

    @Test
    @DisplayName("Requirement 1: Newly created faculty with exactly ONE assigned class returns classes count = 1, not global count (5)")
    void testNewFacultyWithOneClassAssigned_returnsClassesCountOne() {
        // Faculty is assigned only ONE class: class5A
        newFaculty.setAssignedClasses(new ArrayList<>(List.of(class5A)));
        newFaculty.setAssignedSubjects(new ArrayList<>());

        when(teacherRepository.findById("teacher_ada_42")).thenReturn(Optional.of(newFaculty));
        when(classRepository.findById("class_5a")).thenReturn(Optional.of(class5A));
        when(classRepository.findByClassTeacher_Id("teacher_ada_42")).thenReturn(Collections.emptyList());

        List<ClassDto> classes = teacherService.getClassesForTeacher(newFaculty, null);

        assertNotNull(classes, "Returned classes list must not be null");
        assertEquals(1, classes.size(), "Dashboard must display EXACTLY 1 assigned class, not global count");
        assertEquals("class_5a", classes.get(0).getId());
        assertEquals("CSE", classes.get(0).getDepartmentCode());
        assertEquals(5, classes.get(0).getSemester());
        assertEquals("A", classes.get(0).getSection());

        // Verify classRepository.findAll() was NOT called
        verify(classRepository, never()).findAll();
    }

    @Test
    @DisplayName("Requirement 2: Faculty with ONE assigned subject returns subjects count = 1, not global count (7)")
    void testNewFacultyWithOneSubjectAssigned_returnsSubjectsCountOne() {
        newFaculty.setAssignedClasses(new ArrayList<>(List.of(class5A)));
        newFaculty.setAssignedSubjects(new ArrayList<>(List.of(dbmsSubject)));

        when(teacherRepository.findById("teacher_ada_42")).thenReturn(Optional.of(newFaculty));
        when(subjectRepository.findById("subj_dbms")).thenReturn(Optional.of(dbmsSubject));
        when(subjectRepository.findByAssignedTeacher_Id("teacher_ada_42")).thenReturn(Collections.emptyList());

        List<SubjectDto> subjects = teacherService.getSubjectsForTeacher(newFaculty, null, null);

        assertNotNull(subjects, "Returned subjects list must not be null");
        assertEquals(1, subjects.size(), "Dashboard must display EXACTLY 1 assigned subject, not 7");
        assertEquals("subj_dbms", subjects.get(0).getId());
        assertEquals("21CS51", subjects.get(0).getCode());
        assertEquals("Database Management Systems", subjects.get(0).getName());

        // Verify subjectRepository.findAll() was NOT called
        verify(subjectRepository, never()).findAll();
    }

    @Test
    @DisplayName("Requirement 2b: Faculty with 0 assigned subjects returns subjects count = 0, never all database subjects")
    void testNewFacultyWithZeroSubjectsAssigned_returnsSubjectsCountZero() {
        newFaculty.setAssignedClasses(new ArrayList<>(List.of(class5A)));
        newFaculty.setAssignedSubjects(new ArrayList<>());

        when(teacherRepository.findById("teacher_ada_42")).thenReturn(Optional.of(newFaculty));
        when(subjectRepository.findByAssignedTeacher_Id("teacher_ada_42")).thenReturn(Collections.emptyList());

        List<SubjectDto> subjects = teacherService.getSubjectsForTeacher(newFaculty, null, null);

        assertNotNull(subjects);
        assertEquals(0, subjects.size(), "Subjects count must be 0 when no subjects are assigned to the faculty");
        verify(subjectRepository, never()).findAll();
    }

    @Test
    @DisplayName("Requirement 3: Fetch statistics using authenticated faculty ID and filter classes/subjects based on actual assignments")
    void testDashboardStatsUserSpecificFiltering() {
        newFaculty.setAssignedClasses(new ArrayList<>(List.of(class5A)));
        newFaculty.setAssignedSubjects(new ArrayList<>(List.of(dbmsSubject)));

        when(authService.getCurrentTeacher()).thenReturn(newFaculty);
        when(teacherRepository.findById("teacher_ada_42")).thenReturn(Optional.of(newFaculty));
        when(classRepository.findById("class_5a")).thenReturn(Optional.of(class5A));
        when(classRepository.findByClassTeacher_Id("teacher_ada_42")).thenReturn(Collections.emptyList());
        when(subjectRepository.findById("subj_dbms")).thenReturn(Optional.of(dbmsSubject));
        when(subjectRepository.findByAssignedTeacher_Id("teacher_ada_42")).thenReturn(Collections.emptyList());

        // 35 students in assigned class 5A
        List<com.renaissance.entity.Student> mockStudents = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            com.renaissance.entity.Student s = mock(com.renaissance.entity.Student.class);
            when(s.getId()).thenReturn("stud_" + i);
            mockStudents.add(s);
        }
        when(studentRepository.findByCollegeClassId("class_5a")).thenReturn(mockStudents);
        when(sessionRepository.findByTeacher_IdOrderByCreatedAtDesc("teacher_ada_42")).thenReturn(Collections.emptyList());

        DashboardStatsDto stats = reportService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(1, stats.getClassesCount(), "Assigned classes count must be 1");
        assertEquals(1, stats.getAssignedClasses(), "Assigned classes must be 1");
        assertEquals(1, stats.getSubjectsCount(), "Assigned subjects count must be 1");
        assertEquals(1, stats.getAssignedSubjects(), "Assigned subjects must be 1");
        assertEquals(35, stats.getTotalStudents(), "Total students must reflect only assigned class students (35), not all 150");
        assertEquals(0, stats.getTotalClassesConducted(), "Total classes conducted must be 0 for newly created faculty");
        assertEquals(0, stats.getActiveSessions(), "Active sessions must be 0");
    }

    @Test
    @DisplayName("Requirement 4: Data consistency - updates immediately when admin adds or removes a class assignment")
    void testDataConsistencyWhenAdminUpdatesAssignments() {
        // Step 1: Faculty has 1 class
        newFaculty.setAssignedClasses(new ArrayList<>(List.of(class5A)));
        when(teacherRepository.findById("teacher_ada_42")).thenReturn(Optional.of(newFaculty));
        when(classRepository.findById("class_5a")).thenReturn(Optional.of(class5A));
        when(classRepository.findByClassTeacher_Id("teacher_ada_42")).thenReturn(Collections.emptyList());

        List<ClassDto> initialClasses = teacherService.getClassesForTeacher(newFaculty, null);
        assertEquals(1, initialClasses.size(), "Initially must have 1 class");

        // Step 2: Admin adds class 5B to the faculty
        newFaculty.setAssignedClasses(new ArrayList<>(List.of(class5A, class5B)));
        when(classRepository.findById("class_5b")).thenReturn(Optional.of(class5B));

        List<ClassDto> updatedClasses = teacherService.getClassesForTeacher(newFaculty, null);
        assertEquals(2, updatedClasses.size(), "After admin assigns class 5B, classes count must automatically update to 2");

        // Step 3: Admin removes class 5A from the faculty
        newFaculty.setAssignedClasses(new ArrayList<>(List.of(class5B)));

        List<ClassDto> afterRemovalClasses = teacherService.getClassesForTeacher(newFaculty, null);
        assertEquals(1, afterRemovalClasses.size(), "After removing class 5A, classes count must automatically reflect 1");
        assertEquals("class_5b", afterRemovalClasses.get(0).getId());
    }
}
