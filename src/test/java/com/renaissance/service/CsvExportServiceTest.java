package com.renaissance.service;

import com.renaissance.entity.*;
import com.renaissance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CsvExportServiceTest {

    @Mock
    private ReportService reportService;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private AttendanceRecordRepository recordRepository;
    @Mock
    private AuthService authService;

    private CsvExportService csvExportService;

    private CollegeClass testClass;
    private Subject testSubject;
    private Teacher testTeacher;
    private List<Student> testStudents;

    @BeforeEach
    void setUp() {
        csvExportService = new CsvExportService(
                reportService,
                classRepository,
                subjectRepository,
                studentRepository,
                sessionRepository,
                recordRepository,
                authService
        );

        Department dept = new Department("CSE", "Computer Science", "Engineering");

        testTeacher = new Teacher("Dr. Alan Turing", "alan@college.edu", "secret", "EMP001", dept);
        testTeacher.setId("teacher-1");

        testClass = new CollegeClass(dept, 3, "A", "2024-2028", "2026-2027", testTeacher);
        testClass.setId("class-1");

        testSubject = new Subject("21CS51", "Database Management Systems", dept, 3, testClass, testTeacher, 4);
        testSubject.setId("subject-1");

        Student s1 = new Student("1MS21CS001", "Alice Smith", "alice@college.edu", dept, testClass);
        s1.setId("student-1");
        Student s2 = new Student("1MS21CS002", "Bob Jones", "bob@college.edu", dept, testClass);
        s2.setId("student-2");
        Student s3 = new Student("1MS21CS003", "Charlie Brown", "charlie@college.edu", dept, testClass);
        s3.setId("student-3");

        testStudents = new ArrayList<>(List.of(s2, s1, s3)); // unsorted initially
    }

    private List<String> readCsvLines(byte[] csvBytes) throws Exception {
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length >= 3);
        // Verify UTF-8 BOM
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        // Read lines ignoring BOM
        ByteArrayInputStream bais = new ByteArrayInputStream(csvBytes, 3, csvBytes.length - 3);
        BufferedReader reader = new BufferedReader(new InputStreamReader(bais, StandardCharsets.UTF_8));
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    @Test
    @DisplayName("Verify College Theory Attendance Register Form No. 1 format, headers, P/A cells, and percentage")
    void testExportAttendanceSummaryCsv_CollegeRegisterFormat() throws Exception {
        when(classRepository.findById("class-1")).thenReturn(Optional.of(testClass));
        when(subjectRepository.findById("subject-1")).thenReturn(Optional.of(testSubject));
        when(studentRepository.findByCollegeClassId("class-1")).thenReturn(new ArrayList<>(testStudents));

        // Create 3 sessions on distinct dates
        LocalDateTime d1 = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime d2 = LocalDateTime.of(2026, 10, 3, 10, 0);
        LocalDateTime d3 = LocalDateTime.of(2026, 10, 5, 11, 0);

        AttendanceSession sess1 = new AttendanceSession("SESS01", testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, d1, d1.plusMinutes(50));
        sess1.setId("sess-1");
        sess1.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        AttendanceSession sess2 = new AttendanceSession("SESS02", testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, d2, d2.plusMinutes(50));
        sess2.setId("sess-2");
        sess2.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        AttendanceSession sess3 = new AttendanceSession("SESS03", testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, d3, d3.plusMinutes(50));
        sess3.setId("sess-3");
        sess3.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        when(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc("class-1", "subject-1"))
                .thenReturn(List.of(sess1, sess2, sess3));

        // Setup attendance records:
        // Alice (1MS21CS001): Present in all 3 -> 100%
        // Bob (1MS21CS002): Present in sess1 & sess2, Absent in sess3 -> 66.7%
        // Charlie (1MS21CS003): Present in sess1, Absent in sess2 & sess3 -> 33.3%
        Student s1 = testStudents.stream().filter(s -> s.getId().equals("student-1")).findFirst().get();
        Student s2 = testStudents.stream().filter(s -> s.getId().equals("student-2")).findFirst().get();
        Student s3 = testStudents.stream().filter(s -> s.getId().equals("student-3")).findFirst().get();

        AttendanceRecord r1_s1 = new AttendanceRecord(sess1, s1, AttendanceRecord.AttendanceStatus.PRESENT, 12.0, 77.0, 5.0, 10.0);
        AttendanceRecord r1_s2 = new AttendanceRecord(sess1, s2, AttendanceRecord.AttendanceStatus.PRESENT, 12.0, 77.0, 5.0, 10.0);
        AttendanceRecord r1_s3 = new AttendanceRecord(sess1, s3, AttendanceRecord.AttendanceStatus.PRESENT, 12.0, 77.0, 5.0, 10.0);
        when(recordRepository.findBySessionIdOrderByMarkedAtDesc("sess-1")).thenReturn(List.of(r1_s1, r1_s2, r1_s3));

        AttendanceRecord r2_s1 = new AttendanceRecord(sess2, s1, AttendanceRecord.AttendanceStatus.PRESENT, 12.0, 77.0, 5.0, 10.0);
        AttendanceRecord r2_s2 = new AttendanceRecord(sess2, s2, AttendanceRecord.AttendanceStatus.PRESENT, 12.0, 77.0, 5.0, 10.0);
        when(recordRepository.findBySessionIdOrderByMarkedAtDesc("sess-2")).thenReturn(List.of(r2_s1, r2_s2));

        AttendanceRecord r3_s1 = new AttendanceRecord(sess3, s1, AttendanceRecord.AttendanceStatus.PRESENT, 12.0, 77.0, 5.0, 10.0);
        when(recordRepository.findBySessionIdOrderByMarkedAtDesc("sess-3")).thenReturn(List.of(r3_s1));

        byte[] result = csvExportService.exportAttendanceSummaryCsv("class-1", "subject-1");
        List<String> lines = readCsvLines(result);

        // 1. Verify Header Section
        assertTrue(lines.size() >= 13, "Should have header, table headers, and student data rows");
        assertEquals("Form No. 1", lines.get(0));
        assertEquals("Theory Attendance Record", lines.get(1));
        assertTrue(lines.get(2).contains("Academic Year") && lines.get(2).contains("2026-2027"));
        assertTrue(lines.get(3).contains("Semester") && lines.get(3).contains("3rd Semester B.E. (Section A)"));
        assertTrue(lines.get(4).contains("Faculty Name") && lines.get(4).contains("Dr. Alan Turing"));
        assertTrue(lines.get(5).contains("Course / Subject Name") && lines.get(5).contains("Database Management Systems"));
        assertTrue(lines.get(6).contains("Course Code") && lines.get(6).contains("21CS51"));
        assertTrue(lines.get(7).contains("Attendance Record") && lines.get(7).contains("Theory"));
        assertEquals("", lines.get(8).trim(), "Line 9 should be empty separating header from table");

        // 2. Verify Table Column Headers
        String tableHeader = lines.get(9);
        assertEquals("S.No,Student Name,USN,01/10/2026,03/10/2026,05/10/2026,Attendance Percentage (%)", tableHeader);

        // 3. Verify Students sorted by USN ascending
        // Alice: 1MS21CS001 -> P, P, P -> 100%
        String rowAlice = lines.get(10);
        assertEquals("1,Alice Smith,1MS21CS001,P,P,P,100%", rowAlice);

        // Bob: 1MS21CS002 -> P, P, A -> 66.7%
        String rowBob = lines.get(11);
        assertEquals("2,Bob Jones,1MS21CS002,P,P,A,66.7%", rowBob);

        // Charlie: 1MS21CS003 -> P, A, A -> 33.3%
        String rowCharlie = lines.get(12);
        assertEquals("3,Charlie Brown,1MS21CS003,P,A,A,33.3%", rowCharlie);
    }

    @Test
    @DisplayName("Verify dynamic date columns for 8 conducted sessions")
    void testExportAttendanceSummaryCsv_Dynamic8Sessions() throws Exception {
        when(classRepository.findById("class-1")).thenReturn(Optional.of(testClass));
        when(subjectRepository.findById("subject-1")).thenReturn(Optional.of(testSubject));
        when(studentRepository.findByCollegeClassId("class-1")).thenReturn(new ArrayList<>(testStudents));

        List<AttendanceSession> sessions = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            LocalDateTime dt = LocalDateTime.of(2026, 9, i, 10, 0);
            AttendanceSession s = new AttendanceSession("SESS" + i, testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, dt, dt.plusMinutes(50));
            s.setId("sess-" + i);
            s.setStatus(AttendanceSession.SessionStatus.COMPLETED);
            sessions.add(s);
            when(recordRepository.findBySessionIdOrderByMarkedAtDesc("sess-" + i)).thenReturn(Collections.emptyList());
        }

        when(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc("class-1", "subject-1"))
                .thenReturn(sessions);

        byte[] result = csvExportService.exportAttendanceSummaryCsv("class-1", "subject-1");
        List<String> lines = readCsvLines(result);

        String tableHeader = lines.get(9);
        String[] columns = tableHeader.split(",");
        // S.No, Student Name, USN, 8 date columns, Attendance Percentage (%) -> 12 columns total
        assertEquals(12, columns.length);
        assertEquals("S.No", columns[0]);
        assertEquals("Student Name", columns[1]);
        assertEquals("USN", columns[2]);
        assertEquals("01/09/2026", columns[3]);
        assertEquals("08/09/2026", columns[10]);
        assertEquals("Attendance Percentage (%)", columns[11]);

        // Student row has 8 'A's and 0%
        String studentRow = lines.get(10);
        String[] rowCols = studentRow.split(",");
        assertEquals(12, rowCols.length);
        assertEquals("0%", rowCols[11]);
    }

    @Test
    @DisplayName("Verify zero conducted sessions outputs table with 0% and no date columns")
    void testExportAttendanceSummaryCsv_ZeroSessions() throws Exception {
        when(classRepository.findById("class-1")).thenReturn(Optional.of(testClass));
        when(subjectRepository.findById("subject-1")).thenReturn(Optional.of(testSubject));
        when(studentRepository.findByCollegeClassId("class-1")).thenReturn(new ArrayList<>(testStudents));
        when(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc("class-1", "subject-1"))
                .thenReturn(Collections.emptyList());

        byte[] result = csvExportService.exportAttendanceSummaryCsv("class-1", "subject-1");
        List<String> lines = readCsvLines(result);

        String tableHeader = lines.get(9);
        assertEquals("S.No,Student Name,USN,Attendance Percentage (%)", tableHeader);

        String row1 = lines.get(10);
        assertEquals("1,Alice Smith,1MS21CS001,0%", row1);
    }

    @Test
    @DisplayName("Verify future sessions and cancelled sessions are excluded from register")
    void testExportAttendanceSummaryCsv_ExcludesFutureAndCancelledSessions() throws Exception {
        when(classRepository.findById("class-1")).thenReturn(Optional.of(testClass));
        when(subjectRepository.findById("subject-1")).thenReturn(Optional.of(testSubject));
        when(studentRepository.findByCollegeClassId("class-1")).thenReturn(new ArrayList<>(testStudents));

        LocalDateTime pastDate = LocalDateTime.now().minusDays(2);
        AttendanceSession pastSession = new AttendanceSession("PAST", testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, pastDate, pastDate.plusMinutes(50));
        pastSession.setId("sess-past");
        pastSession.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        LocalDateTime futureDate = LocalDateTime.now().plusDays(5);
        AttendanceSession futureSession = new AttendanceSession("FUT", testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, futureDate, futureDate.plusMinutes(50));
        futureSession.setId("sess-fut");
        futureSession.setStatus(AttendanceSession.SessionStatus.ACTIVE);

        AttendanceSession cancelledSession = new AttendanceSession("CANC", testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, pastDate.minusDays(1), pastDate.minusDays(1).plusMinutes(50));
        cancelledSession.setId("sess-canc");
        cancelledSession.setStatus(AttendanceSession.SessionStatus.EXPIRED);

        when(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc("class-1", "subject-1"))
                .thenReturn(List.of(cancelledSession, pastSession, futureSession));
        when(recordRepository.findBySessionIdOrderByMarkedAtDesc("sess-past")).thenReturn(Collections.emptyList());

        byte[] result = csvExportService.exportAttendanceSummaryCsv("class-1", "subject-1");
        List<String> lines = readCsvLines(result);

        String tableHeader = lines.get(9);
        String[] cols = tableHeader.split(",");
        // S.No, Student Name, USN, 1 past date, Attendance Percentage (%) -> 5 columns total
        assertEquals(5, cols.length);
        assertFalse(tableHeader.contains(futureDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }

    @Test
    @DisplayName("Verify date range filtering includes only sessions within range")
    void testExportAttendanceSummaryCsv_DateRangeFiltering() throws Exception {
        when(classRepository.findById("class-1")).thenReturn(Optional.of(testClass));
        when(subjectRepository.findById("subject-1")).thenReturn(Optional.of(testSubject));
        when(studentRepository.findByCollegeClassId("class-1")).thenReturn(new ArrayList<>(testStudents));

        LocalDateTime d1 = LocalDateTime.of(2026, 9, 10, 10, 0);
        LocalDateTime d2 = LocalDateTime.of(2026, 9, 20, 10, 0);
        LocalDateTime d3 = LocalDateTime.of(2026, 9, 30, 10, 0);

        AttendanceSession s1 = new AttendanceSession("S1", testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, d1, d1.plusMinutes(50));
        s1.setId("s1");
        s1.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        AttendanceSession s2 = new AttendanceSession("S2", testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, d2, d2.plusMinutes(50));
        s2.setId("s2");
        s2.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        AttendanceSession s3 = new AttendanceSession("S3", testTeacher, testClass, testSubject, 12.0, 77.0, 5.0, 40.0, d3, d3.plusMinutes(50));
        s3.setId("s3");
        s3.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        when(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc("class-1", "subject-1"))
                .thenReturn(List.of(s1, s2, s3));
        when(recordRepository.findBySessionIdOrderByMarkedAtDesc("s2")).thenReturn(Collections.emptyList());

        // Filter between Sept 15 and Sept 25 -> only s2 should be included
        byte[] result = csvExportService.exportAttendanceSummaryCsv("class-1", "subject-1", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 25));
        List<String> lines = readCsvLines(result);

        String tableHeader = lines.get(9);
        assertEquals("S.No,Student Name,USN,20/09/2026,Attendance Percentage (%)", tableHeader);
    }
}
