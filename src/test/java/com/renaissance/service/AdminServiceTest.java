package com.renaissance.service;

import com.renaissance.dto.CreateDepartmentRequest;
import com.renaissance.dto.DepartmentDto;
import com.renaissance.entity.Department;
import com.renaissance.entity.Student;
import com.renaissance.entity.Teacher;
import com.renaissance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

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
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private TimetableScheduleRepository timetableRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private StudentService studentService;
    private SessionService sessionService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, departmentRepository, classRepository);
        sessionService = new SessionService(sessionRepository, classRepository, subjectRepository, studentRepository, recordRepository, null);

        adminService = new AdminService(
                departmentRepository,
                classRepository,
                subjectRepository,
                teacherRepository,
                studentRepository,
                enrollmentRepository,
                sessionRepository,
                recordRepository,
                timetableRepository,
                studentService,
                sessionService,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Create department with valid data succeeds")
    void testCreateDepartment() {
        CreateDepartmentRequest req = new CreateDepartmentRequest();
        req.setCode("AI");
        req.setName("Artificial Intelligence");
        req.setDescription("AI & ML Department");

        when(departmentRepository.existsByCodeIgnoreCase("AI")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department d = invocation.getArgument(0);
            d.setId("dept-ai");
            return d;
        });

        DepartmentDto result = adminService.createDepartment(req);

        assertNotNull(result);
        assertEquals("AI", result.getCode());
        assertEquals("Artificial Intelligence", result.getName());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    @DisplayName("Prevent deleting department when dependent classes exist")
    void testDeleteDepartmentBlockedByDependencies() {
        Department dept = new Department("CSE", "Computer Science");
        dept.setId("dept-1");

        when(departmentRepository.findById("dept-1")).thenReturn(Optional.of(dept));
        when(classRepository.countByDepartment_Id("dept-1")).thenReturn(3L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            adminService.deleteDepartment("dept-1");
        });

        assertTrue(ex.getMessage().contains("Cannot delete Department 'CSE'"));
        assertTrue(ex.getMessage().contains("3 class(es)"));
        verify(departmentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Prevent deleting student with historical attendance records")
    void testDeleteStudentBlockedByAttendanceRecords() {
        Student s = new Student("1MS21CS001", "Aarav", "aarav@test.com", null, null);
        s.setId("s-1");

        when(studentRepository.findById("s-1")).thenReturn(Optional.of(s));
        when(recordRepository.countByStudent_Id("s-1")).thenReturn(15L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            adminService.deleteStudent("s-1");
        });

        assertTrue(ex.getMessage().contains("Cannot delete student '1MS21CS001 - Aarav' because they have 15 attendance record(s)"));
        verify(studentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Reset teacher password encodes new password using BCrypt")
    void testResetTeacherPassword() {
        Teacher teacher = new Teacher("Dr. Alan Turing", "turing@renaissance.edu", "oldEncoded", "EMP001", null);
        teacher.setId("t-1");

        when(teacherRepository.findById("t-1")).thenReturn(Optional.of(teacher));
        when(passwordEncoder.encode("NewSecret@123")).thenReturn("$2a$10$encodedNewPassword");

        adminService.resetTeacherPassword("t-1", "NewSecret@123");

        assertEquals("$2a$10$encodedNewPassword", teacher.getPassword());
        verify(teacherRepository).save(teacher);
    }
}
