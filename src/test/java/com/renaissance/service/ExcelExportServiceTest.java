package com.renaissance.service;

import com.renaissance.entity.*;
import com.renaissance.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExcelExportServiceTest {

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

    private ExcelExportService excelExportService;

    private CollegeClass testClass;
    private Subject testSubject;
    private Teacher testTeacher;
    private List<Student> testStudents;

    @BeforeEach
    void setUp() {
        excelExportService = new ExcelExportService(
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

    private Workbook parseWorkbook(byte[] bytes) throws Exception {
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        return WorkbookFactory.create(new ByteArrayInputStream(bytes));
    }

    @Test
    @DisplayName("Verify exact college Theory Attendance Register layout in Excel (.xlsx)")
    void testExportAttendanceRegisterExcel_CollegeRegisterFormat() throws Exception {
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

        byte[] result = excelExportService.exportAttendanceRegisterExcel("class-1", "subject-1");
        try (Workbook wb = parseWorkbook(result)) {
            Sheet sheet = wb.getSheet("Theory Attendance Register");
            assertNotNull(sheet);

            // 1. Header Section
            // Row 1: Faculty Name (top-left, Col 0), Course Title (top, Col 2), Semester (top-right, Col 6)
            Row row1 = sheet.getRow(1);
            assertNotNull(row1);
            assertTrue(row1.getCell(0).getStringCellValue().contains("Faculty Name: Dr. Alan Turing"));
            assertTrue(row1.getCell(2).getStringCellValue().contains("Course Title: Database Management Systems"));
            assertTrue(row1.getCell(6).getStringCellValue().contains("Semester: 3rd Semester B.E. (Section A)"));

            // Row 2: Course Code (below Course Title, Col 2)
            Row row2 = sheet.getRow(2);
            assertNotNull(row2);
            assertTrue(row2.getCell(2).getStringCellValue().contains("Course Code: 21CS51"));

            // 2. Journal Marks Section
            // Row 4: Merged "Journal Marks" across Col 3 to Col 5
            Row row4 = sheet.getRow(4);
            assertEquals("Journal Marks", row4.getCell(3).getStringCellValue());
            boolean jmMerged = false;
            for (CellRangeAddress r : sheet.getMergedRegions()) {
                if (r.getFirstRow() == 4 && r.getLastRow() == 4 && r.getFirstColumn() == 3 && r.getLastColumn() == 5) {
                    jmMerged = true;
                    break;
                }
            }
            assertTrue(jmMerged, "Journal Marks must be merged across session columns 3-5");

            // Row 4 Col 6: Attendance Percentage (%) merged across rows 4 to 7
            assertEquals("Attendance Percentage (%)", row4.getCell(6).getStringCellValue());
            boolean pctMerged = false;
            for (CellRangeAddress r : sheet.getMergedRegions()) {
                if (r.getFirstRow() == 4 && r.getLastRow() == 7 && r.getFirstColumn() == 6 && r.getLastColumn() == 6) {
                    pctMerged = true;
                    break;
                }
            }
            assertTrue(pctMerged, "Attendance Percentage must be merged across rows 4-7");

            // 3. DATE row (Row 5)
            Row row5 = sheet.getRow(5);
            assertEquals("DATE:", row5.getCell(2).getStringCellValue());
            assertEquals("01/10/2026", row5.getCell(3).getStringCellValue());
            assertEquals("03/10/2026", row5.getCell(4).getStringCellValue());
            assertEquals("05/10/2026", row5.getCell(5).getStringCellValue());

            // 4. Course Session No. row (Row 6)
            Row row6 = sheet.getRow(6);
            assertEquals("Course Session No.", row6.getCell(2).getStringCellValue());
            assertEquals(1.0, row6.getCell(3).getNumericCellValue());
            assertEquals(2.0, row6.getCell(4).getNumericCellValue());
            assertEquals(3.0, row6.getCell(5).getNumericCellValue());

            // 5. Student Information Headers
            // S.No merged Rows 5-7 Col 0
            assertEquals("S.No", row5.getCell(0).getStringCellValue());
            // Student Name merged Rows 5-7 Col 1
            assertEquals("Student Name", row5.getCell(1).getStringCellValue());
            // USN in Row 7 Col 2
            Row row7 = sheet.getRow(7);
            assertEquals("USN", row7.getCell(2).getStringCellValue());

            // 6. Student Presence Rows (Sorted by USN ascending)
            // Alice (1MS21CS001): Row 8 -> P, P, P -> 100%
            Row rowAlice = sheet.getRow(8);
            assertEquals(1.0, rowAlice.getCell(0).getNumericCellValue());
            assertEquals("Alice Smith", rowAlice.getCell(1).getStringCellValue());
            assertEquals("1MS21CS001", rowAlice.getCell(2).getStringCellValue());
            assertEquals("P", rowAlice.getCell(3).getStringCellValue());
            assertEquals("P", rowAlice.getCell(4).getStringCellValue());
            assertEquals("P", rowAlice.getCell(5).getStringCellValue());
            assertEquals("100%", rowAlice.getCell(6).getStringCellValue());

            // Bob (1MS21CS002): Row 9 -> P, P, A -> 66.7%
            Row rowBob = sheet.getRow(9);
            assertEquals(2.0, rowBob.getCell(0).getNumericCellValue());
            assertEquals("Bob Jones", rowBob.getCell(1).getStringCellValue());
            assertEquals("1MS21CS002", rowBob.getCell(2).getStringCellValue());
            assertEquals("P", rowBob.getCell(3).getStringCellValue());
            assertEquals("P", rowBob.getCell(4).getStringCellValue());
            assertEquals("A", rowBob.getCell(5).getStringCellValue());
            assertEquals("66.7%", rowBob.getCell(6).getStringCellValue());

            // Charlie (1MS21CS003): Row 10 -> P, A, A -> 33.3%
            Row rowCharlie = sheet.getRow(10);
            assertEquals(3.0, rowCharlie.getCell(0).getNumericCellValue());
            assertEquals("Charlie Brown", rowCharlie.getCell(1).getStringCellValue());
            assertEquals("1MS21CS003", rowCharlie.getCell(2).getStringCellValue());
            assertEquals("P", rowCharlie.getCell(3).getStringCellValue());
            assertEquals("A", rowCharlie.getCell(4).getStringCellValue());
            assertEquals("A", rowCharlie.getCell(5).getStringCellValue());
            assertEquals("33.3%", rowCharlie.getCell(6).getStringCellValue());

            // Verify borders on data cells
            assertEquals(BorderStyle.THIN, rowAlice.getCell(0).getCellStyle().getBorderTop());
            assertEquals(BorderStyle.THIN, rowAlice.getCell(1).getCellStyle().getBorderBottom());
            assertEquals(BorderStyle.THIN, rowAlice.getCell(2).getCellStyle().getBorderLeft());
            assertEquals(BorderStyle.THIN, rowAlice.getCell(3).getCellStyle().getBorderRight());

            // Verify alignment
            assertEquals(HorizontalAlignment.LEFT, rowAlice.getCell(1).getCellStyle().getAlignment());
            assertEquals(HorizontalAlignment.CENTER, rowAlice.getCell(2).getCellStyle().getAlignment());
            assertEquals(HorizontalAlignment.CENTER, rowAlice.getCell(3).getCellStyle().getAlignment());
        }
    }

    @Test
    @DisplayName("Verify dynamic expansion for 8 sessions in Excel")
    void testExportAttendanceRegisterExcel_Dynamic8Sessions() throws Exception {
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

        byte[] result = excelExportService.exportAttendanceRegisterExcel("class-1", "subject-1");
        try (Workbook wb = parseWorkbook(result)) {
            Sheet sheet = wb.getSheetAt(0);

            // Session 1 is in Col 3, Session 8 is in Col 10
            Row rowDate = sheet.getRow(5);
            assertEquals("01/09/2026", rowDate.getCell(3).getStringCellValue());
            assertEquals("08/09/2026", rowDate.getCell(10).getStringCellValue());

            // Percentage is in Col 11 (Col 3 + 8)
            Row rowHeader = sheet.getRow(4);
            assertEquals("Attendance Percentage (%)", rowHeader.getCell(11).getStringCellValue());

            // Student row has 8 'A' cells and 0%
            Row studentRow = sheet.getRow(8);
            for (int i = 0; i < 8; i++) {
                assertEquals("A", studentRow.getCell(3 + i).getStringCellValue());
            }
            assertEquals("0%", studentRow.getCell(11).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Verify zero conducted sessions in Excel")
    void testExportAttendanceRegisterExcel_ZeroSessions() throws Exception {
        when(classRepository.findById("class-1")).thenReturn(Optional.of(testClass));
        when(subjectRepository.findById("subject-1")).thenReturn(Optional.of(testSubject));
        when(studentRepository.findByCollegeClassId("class-1")).thenReturn(new ArrayList<>(testStudents));
        when(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc("class-1", "subject-1"))
                .thenReturn(Collections.emptyList());

        byte[] result = excelExportService.exportAttendanceRegisterExcel("class-1", "subject-1");
        try (Workbook wb = parseWorkbook(result)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(4);
            assertEquals("S.No", headerRow.getCell(0).getStringCellValue());
            assertEquals("Student Name", headerRow.getCell(1).getStringCellValue());
            assertEquals("USN", headerRow.getCell(2).getStringCellValue());
            assertEquals("Attendance Percentage (%)", headerRow.getCell(3).getStringCellValue());

            Row studentRow = sheet.getRow(5);
            assertEquals("Alice Smith", studentRow.getCell(1).getStringCellValue());
            assertEquals("0%", studentRow.getCell(3).getStringCellValue());
        }
    }
}
