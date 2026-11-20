package com.renaissance.service;

import com.renaissance.dto.HistoricalImportPreviewDto;
import com.renaissance.entity.*;
import com.renaissance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricalAttendanceImportTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private HistoricalAttendanceImportLogRepository importLogRepository;

    private HistoricalAttendanceImportService importService;

    private Department dept;
    private CollegeClass testClass;
    private Subject subject;
    private Teacher teacher;
    private Student student;

    @BeforeEach
    void setUp() {
        importService = new HistoricalAttendanceImportService(
                studentRepository,
                subjectRepository,
                classRepository,
                departmentRepository,
                teacherRepository,
                sessionRepository,
                recordRepository,
                importLogRepository,
                null
        );

        dept = new Department("CSE", "Computer Science", "CSE Dept");
        dept.setId("dept-cse");

        testClass = new CollegeClass(dept, 5, "A", "2022-2026");
        testClass.setId("class-5a");

        teacher = new Teacher("Dr. Alan Turing", "turing@renaissance.edu", "pwd", "EMP001", dept, "ROLE_TEACHER");
        teacher.setId("teacher-001");

        subject = new Subject("21CS51", "Database Systems", dept, 5);
        subject.setId("subj-21cs51");
        subject.setAssignedTeacher(teacher);

        student = new Student("2GI24IS123", "Aarav Sharma", "aarav@renaissance.edu", dept, testClass);
        student.setId("stud-123");
        student.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Preview valid CSV successfully parses and validates row")
    void testPreviewValidCsv() {
        String csvContent = "USN,SubjectCode,SubjectName,Semester,Section,Date,Session,Status,TeacherID\n" +
                "2GI24IS123,21CS51,Database Systems,5,A,2026-09-15,1,PRESENT,EMP001\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "attendance.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        when(studentRepository.findByUsnIgnoreCase("2GI24IS123")).thenReturn(Optional.of(student));
        when(subjectRepository.findByCodeIgnoreCase("21CS51")).thenReturn(Optional.of(subject));
        when(teacherRepository.findById("EMP001")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmailIgnoreCase("EMP001")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmployeeIdIgnoreCase("EMP001")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findByCollegeClass_IdAndSubject_IdOrderByStartTimeAsc(any(), any())).thenReturn(Collections.emptyList());

        HistoricalImportPreviewDto preview = importService.previewHistoricalAttendanceCsv(file, "admin@renaissance.edu");

        assertNotNull(preview);
        assertEquals(1, preview.getTotalRows());
        assertEquals(1, preview.getValidRows());
        assertEquals(0, preview.getInvalidRows());
        assertNotNull(preview.getImportToken());
        assertEquals(1, preview.getPreviewRows().size());
        assertTrue(preview.getPreviewRows().get(0).isValid());
    }

    @Test
    @DisplayName("CSV with unknown student USN reports invalid row error")
    void testPreviewInvalidStudentUsn() {
        String csvContent = "USN,SubjectCode,SubjectName,Semester,Section,Date,Session,Status,TeacherID\n" +
                "UNKNOWN999,21CS51,Database Systems,5,A,2026-09-15,1,PRESENT,EMP001\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "attendance.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        when(studentRepository.findByUsnIgnoreCase("UNKNOWN999")).thenReturn(Optional.empty());

        HistoricalImportPreviewDto preview = importService.previewHistoricalAttendanceCsv(file, "admin@renaissance.edu");

        assertNotNull(preview);
        assertEquals(1, preview.getTotalRows());
        assertEquals(0, preview.getValidRows());
        assertEquals(1, preview.getInvalidRows());
        assertFalse(preview.getPreviewRows().get(0).isValid());
        assertTrue(preview.getErrors().get(0).contains("not registered"));
    }

    @Test
    @DisplayName("CSV with in-file duplicate entries detects and marks duplicates")
    void testPreviewDuplicateEntriesInCsv() {
        String csvContent = "USN,SubjectCode,SubjectName,Semester,Section,Date,Session,Status,TeacherID\n" +
                "2GI24IS123,21CS51,Database Systems,5,A,2026-09-15,1,PRESENT,EMP001\n" +
                "2GI24IS123,21CS51,Database Systems,5,A,2026-09-15,1,PRESENT,EMP001\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "attendance.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        when(studentRepository.findByUsnIgnoreCase("2GI24IS123")).thenReturn(Optional.of(student));
        when(subjectRepository.findByCodeIgnoreCase("21CS51")).thenReturn(Optional.of(subject));
        when(teacherRepository.findByEmployeeIdIgnoreCase("EMP001")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findByCollegeClass_IdAndSubject_IdOrderByStartTimeAsc(any(), any())).thenReturn(Collections.emptyList());

        HistoricalImportPreviewDto preview = importService.previewHistoricalAttendanceCsv(file, "admin@renaissance.edu");

        assertNotNull(preview);
        assertEquals(2, preview.getTotalRows());
        assertEquals(1, preview.getValidRows());
        assertEquals(1, preview.getInvalidRows());
        assertEquals(1, preview.getDuplicateRows());
    }

    @Test
    @DisplayName("Empty or null CSV throws IllegalArgumentException")
    void testEmptyCsvThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        assertThrows(IllegalArgumentException.class, () ->
                importService.previewHistoricalAttendanceCsv(emptyFile, "admin@renaissance.edu")
        );
    }

    @Test
    @DisplayName("Error report CSV generates structured CSV output")
    void testGenerateErrorReportCsv() {
        List<String> errors = List.of(
                "Row 2: Student USN 'UNKNOWN' not registered.",
                "Row 3: Subject code 'INVALID' not recognized."
        );

        byte[] csv = importService.generateErrorReportCsv(errors);
        assertNotNull(csv);
        String text = new String(csv, StandardCharsets.UTF_8);
        assertTrue(text.contains("Error_Number"));
        assertTrue(text.contains("Failure_Reason"));
        assertTrue(text.contains("Row 2: Student USN"));
    }
}
