package com.renaissance.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.renaissance.dto.HistoricalImportPreviewDto;
import com.renaissance.dto.HistoricalImportRowDto;
import com.renaissance.entity.*;
import com.renaissance.exception.ResourceNotFoundException;
import com.renaissance.repository.*;
import com.renaissance.util.AttendanceCodeGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class HistoricalAttendanceImportService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalAttendanceImportService.class);

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRepository classRepository;
    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final HistoricalAttendanceImportLogRepository importLogRepository;
    private final CacheManager cacheManager;

    // Temporary storage for staged preview data before confirmation (15-minute TTL)
    private final Cache<String, List<HistoricalImportRowDto>> previewCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(50)
            .build();

    public HistoricalAttendanceImportService(StudentRepository studentRepository,
                                           SubjectRepository subjectRepository,
                                           ClassRepository classRepository,
                                           DepartmentRepository departmentRepository,
                                           TeacherRepository teacherRepository,
                                           AttendanceSessionRepository sessionRepository,
                                           AttendanceRecordRepository recordRepository,
                                           HistoricalAttendanceImportLogRepository importLogRepository,
                                           @org.springframework.beans.factory.annotation.Autowired(required = false) CacheManager cacheManager) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.classRepository = classRepository;
        this.departmentRepository = departmentRepository;
        this.teacherRepository = teacherRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.importLogRepository = importLogRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Parses and validates uploaded CSV, returning a comprehensive preview.
     * Stashes valid rows in temporary memory under importToken for confirmation.
     */
    public HistoricalImportPreviewDto previewHistoricalAttendanceCsv(MultipartFile file, String adminEmail) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded CSV file is empty.");
        }

        List<HistoricalImportRowDto> allRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> csvDeduplicationKeys = new HashSet<>();

        int totalRows = 0;
        int validRows = 0;
        int invalidRows = 0;
        int duplicateRows = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : csvParser) {
                totalRows++;
                HistoricalImportRowDto row = new HistoricalImportRowDto();
                row.setRowNumber(totalRows);

                try {
                    String usn = getRecordField(record, "USN");
                    String subjectCode = getRecordField(record, "SubjectCode", "Subject_Code", "Subject Code", "Subject");
                    String subjectName = getRecordField(record, "SubjectName", "Subject_Name", "Subject Name", "CourseName");
                    String semesterStr = getRecordField(record, "Semester", "Sem");
                    String section = getRecordField(record, "Section", "Sec");
                    String dateStr = getRecordField(record, "Date", "AttendanceDate");
                    String sessionName = getRecordField(record, "Session", "SessionName", "Period");
                    String statusStr = getRecordField(record, "Status", "AttendanceStatus");
                    String teacherId = getRecordField(record, "TeacherID", "Teacher_ID", "Teacher", "TeacherEmail", "TeacherId");

                    row.setUsn(usn);
                    row.setSubjectCode(subjectCode);
                    row.setSubjectName(subjectName);
                    row.setSection(section);
                    row.setDate(dateStr);
                    row.setSessionName(sessionName != null && !sessionName.isBlank() ? sessionName : "Regular");
                    row.setStatus(statusStr != null ? statusStr.toUpperCase() : "PRESENT");
                    row.setTeacherIdentifier(teacherId);

                    if (semesterStr != null && !semesterStr.isBlank()) {
                        try {
                            row.setSemester(Integer.parseInt(semesterStr.trim()));
                        } catch (NumberFormatException nfe) {
                            row.setValid(false);
                            row.setValidationError("Invalid semester: " + semesterStr);
                            invalidRows++;
                            allRows.add(row);
                            continue;
                        }
                    }

                    // 1. Validate USN
                    if (usn == null || usn.isBlank()) {
                        row.setValid(false);
                        row.setValidationError("USN is required.");
                        invalidRows++;
                        allRows.add(row);
                        continue;
                    }

                    Student student = studentRepository.findByUsnIgnoreCase(usn.trim()).orElse(null);
                    if (student == null) {
                        row.setValid(false);
                        row.setValidationError("Student with USN '" + usn + "' is not registered in the system.");
                        invalidRows++;
                        allRows.add(row);
                        continue;
                    }
                    row.setStudentName(student.getName());
                    row.setStudentId(student.getId());

                    // 2. Validate Class
                    CollegeClass studentClass = student.getCollegeClass();
                    if (studentClass == null) {
                        row.setValid(false);
                        row.setValidationError("Student is not assigned to any class.");
                        invalidRows++;
                        allRows.add(row);
                        continue;
                    }
                    row.setClassId(studentClass.getId());

                    // Optional section/semester check if provided in CSV
                    if (row.getSemester() != null && !row.getSemester().equals(studentClass.getSemester())) {
                        row.setValid(false);
                        row.setValidationError("Semester mismatch: Student belongs to semester " + studentClass.getSemester() + ", but CSV specified " + row.getSemester());
                        invalidRows++;
                        allRows.add(row);
                        continue;
                    }
                    if (section != null && !section.isBlank() && !section.equalsIgnoreCase(studentClass.getSection())) {
                        row.setValid(false);
                        row.setValidationError("Section mismatch: Student belongs to section " + studentClass.getSection() + ", but CSV specified " + section);
                        invalidRows++;
                        allRows.add(row);
                        continue;
                    }

                    // 3. Validate Subject
                    Subject subject = null;
                    if (subjectCode != null && !subjectCode.isBlank()) {
                        subject = subjectRepository.findByCodeIgnoreCase(subjectCode.trim()).orElse(null);
                    }
                    if (subject == null && subjectName != null && !subjectName.isBlank()) {
                        subject = subjectRepository.findByNameIgnoreCase(subjectName.trim()).orElse(null);
                    }
                    if (subject == null) {
                        row.setValid(false);
                        row.setValidationError("Subject '" + (subjectCode != null ? subjectCode : subjectName) + "' not found.");
                        invalidRows++;
                        allRows.add(row);
                        continue;
                    }
                    row.setSubjectId(subject.getId());
                    row.setSubjectCode(subject.getCode());
                    row.setSubjectName(subject.getName());

                    // 4. Validate Date
                    LocalDate attendanceDate = parseDate(dateStr);
                    if (attendanceDate == null) {
                        row.setValid(false);
                        row.setValidationError("Invalid date format: '" + dateStr + "'. Expected YYYY-MM-DD or DD-MM-YYYY.");
                        invalidRows++;
                        allRows.add(row);
                        continue;
                    }
                    row.setDate(attendanceDate.toString());

                    // 5. Validate Status
                    String normStatus = row.getStatus();
                    if (!"PRESENT".equalsIgnoreCase(normStatus) && !"ABSENT".equalsIgnoreCase(normStatus)) {
                        row.setValid(false);
                        row.setValidationError("Invalid status '" + statusStr + "'. Must be PRESENT or ABSENT.");
                        invalidRows++;
                        allRows.add(row);
                        continue;
                    }
                    row.setStatus(normStatus.toUpperCase());

                    // 6. Resolve Teacher
                    Teacher teacher = null;
                    if (teacherId != null && !teacherId.isBlank()) {
                        teacher = teacherRepository.findById(teacherId.trim()).orElse(null);
                        if (teacher == null) {
                            teacher = teacherRepository.findByEmailIgnoreCase(teacherId.trim()).orElse(null);
                        }
                        if (teacher == null) {
                            teacher = teacherRepository.findByEmployeeIdIgnoreCase(teacherId.trim()).orElse(null);
                        }
                    }
                    if (teacher == null) {
                        teacher = subject.getAssignedTeacher();
                    }
                    if (teacher == null) {
                        teacher = studentClass.getClassTeacher();
                    }
                    if (teacher == null && adminEmail != null) {
                        teacher = teacherRepository.findByEmailIgnoreCase(adminEmail).orElse(null);
                    }
                    if (teacher == null) {
                        row.setValid(false);
                        row.setValidationError("Unable to resolve faculty member for this attendance record.");
                        invalidRows++;
                        allRows.add(row);
                        continue;
                    }
                    row.setTeacherId(teacher.getId());

                    // 7. Check In-CSV Duplicates
                    String dedupeKey = usn.toUpperCase() + "_" + subject.getId() + "_" + attendanceDate + "_" + row.getSessionName().toUpperCase();
                    if (!csvDeduplicationKeys.add(dedupeKey)) {
                        row.setValid(false);
                        row.setValidationError("Duplicate record inside CSV for student " + usn + " on " + attendanceDate + " (" + row.getSessionName() + ")");
                        invalidRows++;
                        duplicateRows++;
                        allRows.add(row);
                        continue;
                    }

                    // 8. Check Database Duplicates
                    // We check if an AttendanceRecord already exists for this student for a session on that date and subject
                    LocalDateTime dayStart = attendanceDate.atStartOfDay();
                    LocalDateTime dayEnd = attendanceDate.atTime(LocalTime.MAX);
                    List<AttendanceSession> existingSessions = sessionRepository.findByCollegeClass_IdAndSubject_IdOrderByStartTimeAsc(studentClass.getId(), subject.getId());
                    boolean dbDuplicateFound = false;
                    for (AttendanceSession s : existingSessions) {
                        if (s.getStartTime() != null && !s.getStartTime().isBefore(dayStart) && !s.getStartTime().isAfter(dayEnd)) {
                            if (recordRepository.existsBySessionIdAndStudentId(s.getId(), student.getId())) {
                                dbDuplicateFound = true;
                                break;
                            }
                        }
                    }
                    if (dbDuplicateFound) {
                        row.setValid(false);
                        row.setValidationError("Attendance record already exists in database for " + usn + " on " + attendanceDate);
                        invalidRows++;
                        duplicateRows++;
                        allRows.add(row);
                        continue;
                    }

                    row.setValid(true);
                    validRows++;
                    allRows.add(row);

                } catch (Exception ex) {
                    row.setValid(false);
                    row.setValidationError("Row error: " + ex.getMessage());
                    invalidRows++;
                    allRows.add(row);
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage(), e);
        }

        String importToken = UUID.randomUUID().toString();
        // Stash only valid rows for execution
        List<HistoricalImportRowDto> validOnly = allRows.stream().filter(HistoricalImportRowDto::isValid).collect(Collectors.toList());
        previewCache.put(importToken, validOnly);

        HistoricalImportPreviewDto preview = new HistoricalImportPreviewDto();
        preview.setImportToken(importToken);
        preview.setFileName(file.getOriginalFilename());
        preview.setTotalRows(totalRows);
        preview.setValidRows(validRows);
        preview.setInvalidRows(invalidRows);
        preview.setDuplicateRows(duplicateRows);
        // Display first 50 rows in UI preview
        preview.setPreviewRows(allRows.stream().limit(50).collect(Collectors.toList()));

        for (HistoricalImportRowDto r : allRows) {
            if (!r.isValid()) {
                errors.add("Row " + r.getRowNumber() + " (" + (r.getUsn() != null ? r.getUsn() : "N/A") + "): " + r.getValidationError());
            }
        }
        preview.setErrors(errors);

        return preview;
    }

    /**
     * Executes validated staged historical attendance records in safe bulk operations.
     */
    public Map<String, Object> confirmHistoricalImport(String importToken, String fileName, String adminEmail) {
        List<HistoricalImportRowDto> validRows = previewCache.getIfPresent(importToken);
        if (validRows == null || validRows.isEmpty()) {
            throw new IllegalStateException("Import session has expired or no valid rows found. Please upload the CSV again.");
        }

        // Group rows by: classId, subjectId, teacherId, date, sessionName
        Map<String, List<HistoricalImportRowDto>> sessionGroups = validRows.stream()
                .collect(Collectors.groupingBy(r -> r.getClassId() + "___" + r.getSubjectId() + "___" + r.getTeacherId() + "___" + r.getDate() + "___" + r.getSessionName()));

        int importedCount = 0;
        Set<String> affectedTeacherIds = new HashSet<>();

        for (Map.Entry<String, List<HistoricalImportRowDto>> entry : sessionGroups.entrySet()) {
            List<HistoricalImportRowDto> groupRows = entry.getValue();
            if (groupRows.isEmpty()) continue;

            HistoricalImportRowDto sample = groupRows.get(0);
            LocalDate date = LocalDate.parse(sample.getDate());
            LocalDateTime startTime = date.atTime(9, 0);
            LocalDateTime endTime = date.atTime(9, 50);

            CollegeClass collegeClass = classRepository.findById(sample.getClassId()).orElse(null);
            Subject subject = subjectRepository.findById(sample.getSubjectId()).orElse(null);
            Teacher teacher = teacherRepository.findById(sample.getTeacherId()).orElse(null);

            if (collegeClass == null || subject == null || teacher == null) {
                continue;
            }

            affectedTeacherIds.add(teacher.getId());

            // Create or reuse historical attendance session marked COMPLETED
            String code = AttendanceCodeGenerator.generateCode();
            AttendanceSession histSession = new AttendanceSession(
                    code,
                    teacher,
                    collegeClass,
                    subject,
                    0.0,
                    0.0,
                    0.0,
                    40.0,
                    startTime,
                    endTime
            );
            histSession.setStatus(AttendanceSession.SessionStatus.COMPLETED);
            AttendanceSession savedSession = sessionRepository.save(histSession);

            List<AttendanceRecord> recordsToSave = new ArrayList<>();
            for (HistoricalImportRowDto row : groupRows) {
                Student student = studentRepository.findById(row.getStudentId()).orElse(null);
                if (student == null) continue;

                AttendanceRecord.AttendanceStatus status = "ABSENT".equalsIgnoreCase(row.getStatus()) ?
                        AttendanceRecord.AttendanceStatus.ABSENT : AttendanceRecord.AttendanceStatus.PRESENT;

                AttendanceRecord rec = new AttendanceRecord(
                        savedSession,
                        student,
                        status,
                        0.0,
                        0.0,
                        0.0,
                        0.0
                );
                rec.setMarkedAt(startTime);
                recordsToSave.add(rec);
            }

            if (!recordsToSave.isEmpty()) {
                recordRepository.saveAll(recordsToSave);
                importedCount += recordsToSave.size();
            }
        }

        // Invalidate cached dashboard stats for affected teachers
        if (cacheManager != null) {
            org.springframework.cache.Cache dashCache = cacheManager.getCache(com.renaissance.config.CacheConfig.CACHE_DASHBOARD_STATS);
            if (dashCache != null) {
                for (String tId : affectedTeacherIds) {
                    dashCache.evict("TEACHER_" + tId);
                }
            }
        }

        // Log audit record
        HistoricalAttendanceImportLog logEntry = new HistoricalAttendanceImportLog(
                adminEmail,
                adminEmail,
                fileName != null ? fileName : "historical_attendance.csv",
                validRows.size(),
                importedCount,
                0,
                0,
                Collections.emptyList(),
                "COMPLETED"
        );
        importLogRepository.save(logEntry);

        // Invalidate preview token
        previewCache.invalidate(importToken);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("importedRecords", importedCount);
        result.put("sessionsCreated", sessionGroups.size());
        result.put("importLogId", logEntry.getId());
        result.put("message", "Successfully imported " + importedCount + " historical attendance records across " + sessionGroups.size() + " class sessions.");
        return result;
    }

    /**
     * Generates a downloadable CSV containing any rejected or duplicate rows with specific failure reasons.
     */
    public byte[] generateErrorReportCsv(List<String> errors) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(osw, CSVFormat.DEFAULT.builder()
                     .setHeader("Error_Number", "Failure_Reason")
                     .build())) {

            int num = 1;
            if (errors != null) {
                for (String err : errors) {
                    csvPrinter.printRecord(num++, err);
                }
            }
            csvPrinter.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private String getRecordField(CSVRecord record, String... possibleNames) {
        for (String name : possibleNames) {
            if (record.isMapped(name)) {
                String val = record.get(name);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
        }
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String trimmed = dateStr.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE, // YYYY-MM-DD
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy")
        );
        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(trimmed, dtf);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
