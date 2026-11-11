package com.renaissance.service;

import com.renaissance.dto.DateWiseAttendanceRecordDto;
import com.renaissance.dto.StudentProfileDto;
import com.renaissance.entity.*;
import com.renaissance.exception.UnauthorizedException;
import com.renaissance.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CsvExportService {

    private final ReportService reportService;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AuthService authService;

    public CsvExportService(ReportService reportService,
                            ClassRepository classRepository,
                            SubjectRepository subjectRepository,
                            StudentRepository studentRepository,
                            AttendanceSessionRepository sessionRepository,
                            AttendanceRecordRepository recordRepository,
                            AuthService authService) {
        this.reportService = reportService;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.authService = authService;
    }

    /**
     * Generates a downloadable CSV formatted as a traditional college Theory Attendance Register (Form No. 1).
     */
    public byte[] exportAttendanceSummaryCsv(String classId, String subjectId) {
        return exportAttendanceSummaryCsv(classId, subjectId, null, null);
    }

    /**
     * Generates a downloadable CSV formatted as a traditional college Theory Attendance Register (Form No. 1)
     * with dynamic session date columns, student P/A presence cells, and final attendance percentage.
     */
    public byte[] exportAttendanceSummaryCsv(String classId, String subjectId, LocalDate startDate, LocalDate endDate) {
        Teacher teacher = getAuthorizedTeacherOrNull();
        Set<String> assignedClassIds = (teacher != null && !teacher.isAdmin() && teacher.getAssignedClasses() != null && !teacher.getAssignedClasses().isEmpty())
                ? teacher.getAssignedClasses().stream().map(CollegeClass::getId).collect(Collectors.toSet())
                : null;

        if (assignedClassIds != null && classId != null && !classId.isBlank()) {
            if (!assignedClassIds.contains(classId)) {
                throw new UnauthorizedException("Access Denied: You are not assigned to class " + classId);
            }
        }

        // 1. Fetch Class and Subject metadata
        CollegeClass collegeClass = (classId != null && !classId.isBlank())
                ? classRepository.findById(classId).orElse(null)
                : null;

        Subject subject = (subjectId != null && !subjectId.isBlank())
                ? subjectRepository.findById(subjectId).orElse(null)
                : null;

        // 2. Fetch Students for the selected class/division
        List<Student> students;
        if (classId != null && !classId.isBlank()) {
            students = studentRepository.findByCollegeClassId(classId);
        } else if (assignedClassIds != null && !assignedClassIds.isEmpty()) {
            students = studentRepository.findByCollegeClassIdIn(assignedClassIds);
        } else {
            students = studentRepository.findAll();
        }

        // Sort students: by USN ascending, then by name
        students.sort((a, b) -> {
            String usn1 = a.getUsn() != null ? a.getUsn().trim() : "";
            String usn2 = b.getUsn() != null ? b.getUsn().trim() : "";
            int cmp = usn1.compareToIgnoreCase(usn2);
            if (cmp != 0) return cmp;
            String name1 = a.getName() != null ? a.getName().trim() : "";
            String name2 = b.getName() != null ? b.getName().trim() : "";
            return name1.compareToIgnoreCase(name2);
        });

        // 3. Fetch conducted sessions for the class and subject in chronological order
        List<AttendanceSession> conductedSessions;
        if (classId != null && !classId.isBlank() && subjectId != null && !subjectId.isBlank()) {
            conductedSessions = new ArrayList<>(sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc(classId, subjectId));
        } else if (classId != null && !classId.isBlank()) {
            conductedSessions = new ArrayList<>(sessionRepository.findByCollegeClassIdOrderByStartTimeDesc(classId));
        } else if (assignedClassIds != null && !assignedClassIds.isEmpty()) {
            conductedSessions = new ArrayList<>(sessionRepository.findByCollegeClassIdIn(assignedClassIds));
            if (subjectId != null && !subjectId.isBlank()) {
                conductedSessions = conductedSessions.stream()
                        .filter(s -> s.getSubject() != null && subjectId.equals(s.getSubject().getId()))
                        .collect(Collectors.toList());
            }
        } else {
            conductedSessions = new ArrayList<>(sessionRepository.findAll());
            if (subjectId != null && !subjectId.isBlank()) {
                conductedSessions = conductedSessions.stream()
                        .filter(s -> s.getSubject() != null && subjectId.equals(s.getSubject().getId()))
                        .collect(Collectors.toList());
            }
        }

        // Guarantee chronological sorting
        conductedSessions.sort(Comparator.comparing(
                AttendanceSession::getStartTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        // Filter by date range if provided
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        if (startDt != null || endDt != null) {
            conductedSessions = conductedSessions.stream().filter(s -> {
                if (s.getStartTime() == null) return false;
                if (startDt != null && s.getStartTime().isBefore(startDt)) return false;
                if (endDt != null && s.getStartTime().isAfter(endDt)) return false;
                return true;
            }).collect(Collectors.toList());
        }

        // Only COMPLETED or ACTIVE sessions that have already started (no future sessions without records)
        LocalDateTime now = LocalDateTime.now();
        List<AttendanceSession> validSessions = conductedSessions.stream()
                .filter(s -> s.getStatus() == AttendanceSession.SessionStatus.COMPLETED || s.getStatus() == AttendanceSession.SessionStatus.ACTIVE)
                .filter(s -> s.getStartTime() != null && !s.getStartTime().isAfter(now))
                .collect(Collectors.toList());

        // 4. Preload attendance records for all valid sessions
        Map<String, Set<String>> sessionPresentStudentKeys = new HashMap<>();
        for (AttendanceSession s : validSessions) {
            Set<String> presentKeys = new HashSet<>();
            List<AttendanceRecord> recs = recordRepository.findBySessionIdOrderByMarkedAtDesc(s.getId());
            for (AttendanceRecord r : recs) {
                if (r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT && r.getStudent() != null) {
                    if (r.getStudent().getId() != null) {
                        presentKeys.add("ID:" + r.getStudent().getId());
                    }
                    if (r.getStudent().getUsn() != null && !r.getStudent().getUsn().isBlank()) {
                        presentKeys.add("USN:" + r.getStudent().getUsn().trim().toUpperCase(Locale.US));
                    }
                }
            }
            sessionPresentStudentKeys.put(s.getId(), presentKeys);
        }

        // 5. Resolve Header Fields
        String academicYear = (collegeClass != null && collegeClass.getAcademicYear() != null && !collegeClass.getAcademicYear().isBlank())
                ? collegeClass.getAcademicYear()
                : "2026-2027";

        String semesterDisplay = formatSemester(
                collegeClass != null ? collegeClass.getSemester() : (subject != null ? subject.getSemester() : null),
                collegeClass != null ? collegeClass.getSection() : null
        );

        String facultyName = resolveFacultyName(subject, collegeClass, validSessions, teacher);

        String courseName = (subject != null && subject.getName() != null && !subject.getName().isBlank())
                ? subject.getName()
                : (!validSessions.isEmpty() && validSessions.get(0).getSubject() != null && validSessions.get(0).getSubject().getName() != null
                ? validSessions.get(0).getSubject().getName()
                : "All Subjects");

        String courseCode = (subject != null && subject.getCode() != null && !subject.getCode().isBlank())
                ? subject.getCode()
                : (!validSessions.isEmpty() && validSessions.get(0).getSubject() != null && validSessions.get(0).getSubject().getCode() != null
                ? validSessions.get(0).getSubject().getCode()
                : "N/A");

        // 6. Generate CSV Output
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Write UTF-8 BOM so Microsoft Excel recognizes UTF-8 encoding cleanly
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                // Header Section
                csvPrinter.printRecord("Form No. 1");
                csvPrinter.printRecord("Theory Attendance Record");
                csvPrinter.printRecord("Academic Year", academicYear);
                csvPrinter.printRecord("Semester", semesterDisplay);
                csvPrinter.printRecord("Faculty Name", facultyName);
                csvPrinter.printRecord("Course / Subject Name", courseName);
                csvPrinter.printRecord("Course Code", courseCode);
                csvPrinter.printRecord("Attendance Record", "Theory");
                csvPrinter.println(); // Blank row separating header metadata from tabular attendance data

                // Tabular Column Headers
                List<String> tableHeaders = new ArrayList<>();
                tableHeaders.add("S.No");
                tableHeaders.add("Student Name");
                tableHeaders.add("USN");

                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (AttendanceSession s : validSessions) {
                    String dateStr = s.getStartTime() != null ? s.getStartTime().format(dateFmt) : "-";
                    tableHeaders.add(dateStr);
                }
                tableHeaders.add("Attendance Percentage (%)");
                csvPrinter.printRecord(tableHeaders);

                // Student Attendance Rows
                int totalConducted = validSessions.size();
                int serialNo = 1;
                for (Student student : students) {
                    List<Object> row = new ArrayList<>();
                    row.add(serialNo++);
                    row.add(student.getName() != null ? student.getName() : "");
                    row.add(student.getUsn() != null ? student.getUsn() : "");

                    int presentCount = 0;
                    for (AttendanceSession s : validSessions) {
                        Set<String> presentKeys = sessionPresentStudentKeys.get(s.getId());
                        boolean present = isStudentPresent(student, presentKeys);
                        if (present) {
                            presentCount++;
                            row.add("P");
                        } else {
                            row.add("A");
                        }
                    }

                    double pct = totalConducted > 0 ? ((double) presentCount * 100.0) / totalConducted : 0.0;
                    String pctStr = (pct == Math.floor(pct))
                            ? String.format(Locale.US, "%.0f%%", pct)
                            : String.format(Locale.US, "%.1f%%", pct);
                    row.add(pctStr);

                    csvPrinter.printRecord(row);
                }

                csvPrinter.flush();
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate college theory attendance register CSV: " + e.getMessage(), e);
        }
    }

    private boolean isStudentPresent(Student student, Set<String> presentKeys) {
        if (student == null || presentKeys == null || presentKeys.isEmpty()) {
            return false;
        }
        if (student.getId() != null && presentKeys.contains("ID:" + student.getId())) {
            return true;
        }
        if (student.getUsn() != null && presentKeys.contains("USN:" + student.getUsn().trim().toUpperCase(Locale.US))) {
            return true;
        }
        return false;
    }

    private String formatSemester(Integer semester, String section) {
        if (semester == null) {
            return (section != null && !section.isBlank() && !"-".equals(section)) ? "Section " + section : "N/A";
        }
        String suffix = switch (semester) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> semester + "th";
        };
        String semStr = suffix + " Semester B.E.";
        if (section != null && !section.isBlank() && !"-".equals(section)) {
            semStr += " (Section " + section + ")";
        }
        return semStr;
    }

    private String resolveFacultyName(Subject subject, CollegeClass collegeClass, List<AttendanceSession> sessions, Teacher currentTeacher) {
        if (subject != null) {
            try {
                if (subject.getAssignedTeacher() != null && subject.getAssignedTeacher().getName() != null && !subject.getAssignedTeacher().getName().isBlank()) {
                    return subject.getAssignedTeacher().getName();
                }
            } catch (Exception ignored) {}
        }
        if (currentTeacher != null && currentTeacher.getName() != null && !currentTeacher.getName().isBlank()) {
            return currentTeacher.getName();
        }
        if (sessions != null && !sessions.isEmpty()) {
            try {
                AttendanceSession firstSession = sessions.get(0);
                if (firstSession.getTeacher() != null && firstSession.getTeacher().getName() != null && !firstSession.getTeacher().getName().isBlank()) {
                    return firstSession.getTeacher().getName();
                }
            } catch (Exception ignored) {}
        }
        if (collegeClass != null) {
            try {
                if (collegeClass.getClassTeacher() != null && collegeClass.getClassTeacher().getName() != null && !collegeClass.getClassTeacher().getName().isBlank()) {
                    return collegeClass.getClassTeacher().getName();
                }
            } catch (Exception ignored) {}
        }
        return "Faculty";
    }

    private Teacher getAuthorizedTeacherOrNull() {
        try {
            return authService.getCurrentTeacher();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates a downloadable CSV of date-wise attendance records.
     */
    public byte[] exportDateWiseAttendanceCsv(String classId, String subjectId, LocalDate startDate, LocalDate endDate) {
        List<DateWiseAttendanceRecordDto> records = reportService.getDateWiseAttendance(classId, subjectId, startDate, endDate);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(
                             "Date",
                             "Student Name",
                             "Student ID / USN",
                             "Class",
                             "Section",
                             "Subject",
                             "Attendance Status"
                     ).build())) {

            // Write UTF-8 BOM
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            for (DateWiseAttendanceRecordDto rec : records) {
                String subjectDisplay = rec.getSubjectCode() != null && !"-".equals(rec.getSubjectCode())
                        ? rec.getSubjectCode() + " - " + rec.getSubjectName()
                        : rec.getSubjectName();

                csvPrinter.printRecord(
                        rec.getDate(),
                        rec.getStudentName(),
                        rec.getStudentUsn(),
                        rec.getClassName(),
                        rec.getSection(),
                        subjectDisplay,
                        rec.getStatus()
                );
            }

            csvPrinter.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate date-wise attendance CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a downloadable CSV of an individual student's complete attendance history.
     */
    public byte[] exportStudentHistoryCsv(String studentId) {
        return exportStudentHistoryCsv(studentId, null, null);
    }

    /**
     * Generates a downloadable CSV of an individual student's attendance history filtered by class and subject.
     */
    public byte[] exportStudentHistoryCsv(String studentId, String classId, String subjectId) {
        StudentProfileDto profile = reportService.getStudentProfile(studentId, classId, subjectId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(
                             "Session Code",
                             "Subject Code",
                             "Subject Name",
                             "Date",
                             "Session Time",
                             "Attendance Status",
                             "Marked Time",
                             "Distance From Teacher"
                     ).build())) {

            // Write UTF-8 BOM
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            for (StudentProfileDto.HistoryRecordDto rec : profile.getAttendanceHistory()) {
                csvPrinter.printRecord(
                        rec.getSessionCode(),
                        rec.getSubjectCode(),
                        rec.getSubjectName(),
                        rec.getSessionDate(),
                        rec.getSessionTime(),
                        rec.getStatus(),
                        rec.getMarkedTime(),
                        rec.getDistanceFromTeacher()
                );
            }

            csvPrinter.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate student history CSV: " + e.getMessage(), e);
        }
    }
}
