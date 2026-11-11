package com.renaissance.service;

import com.renaissance.entity.*;
import com.renaissance.exception.UnauthorizedException;
import com.renaissance.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AuthService authService;

    public ExcelExportService(ClassRepository classRepository,
                              SubjectRepository subjectRepository,
                              StudentRepository studentRepository,
                              AttendanceSessionRepository sessionRepository,
                              AttendanceRecordRepository recordRepository,
                              AuthService authService) {
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.authService = authService;
    }

    public byte[] exportAttendanceRegisterExcel(String classId, String subjectId) {
        return exportAttendanceRegisterExcel(classId, subjectId, null, null);
    }

    /**
     * Generates a fully-styled, pixel-accurate Microsoft Excel (.xlsx) Theory Attendance Register
     * matching the college reference format with:
     * - Top metadata header: Faculty Name (top-left), Course Title (top), Semester (top-right), Course Code (below Course Title).
     * - Merged "Journal Marks" section heading above session columns.
     * - Dedicated "DATE:" row directly above session columns.
     * - Dedicated "Course Session No." row with sequential centered session numbers: 1, 2, 3, 4...
     * - Bold centered student headers: S.No, Student Name, USN.
     * - Individual student presence: P/A cells under corresponding sessions.
     * - Final Attendance Percentage (%) column immediately after the last session.
     * - Solid thin borders, underlines, custom row heights, and column widths.
     */
    public byte[] exportAttendanceRegisterExcel(String classId, String subjectId, LocalDate startDate, LocalDate endDate) {
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

        // 2. Fetch Students for the selected division
        List<Student> students;
        if (classId != null && !classId.isBlank()) {
            students = studentRepository.findByCollegeClassId(classId);
        } else if (assignedClassIds != null && !assignedClassIds.isEmpty()) {
            students = studentRepository.findByCollegeClassIdIn(assignedClassIds);
        } else {
            students = studentRepository.findAll();
        }

        students.sort((a, b) -> {
            String usn1 = a.getUsn() != null ? a.getUsn().trim() : "";
            String usn2 = b.getUsn() != null ? b.getUsn().trim() : "";
            int cmp = usn1.compareToIgnoreCase(usn2);
            if (cmp != 0) return cmp;
            String name1 = a.getName() != null ? a.getName().trim() : "";
            String name2 = b.getName() != null ? b.getName().trim() : "";
            return name1.compareToIgnoreCase(name2);
        });

        // 3. Fetch conducted sessions chronologically
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

        conductedSessions.sort(Comparator.comparing(
                AttendanceSession::getStartTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        // Date range filtering
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

        // Only COMPLETED or ACTIVE non-future sessions
        LocalDateTime now = LocalDateTime.now();
        List<AttendanceSession> validSessions = conductedSessions.stream()
                .filter(s -> s.getStatus() == AttendanceSession.SessionStatus.COMPLETED || s.getStatus() == AttendanceSession.SessionStatus.ACTIVE)
                .filter(s -> s.getStartTime() != null && !s.getStartTime().isAfter(now))
                .collect(Collectors.toList());

        // 4. Preload attendance records per session
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

        // 5. Metadata field resolution
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
        String semesterDisplay = formatSemester(
                collegeClass != null ? collegeClass.getSemester() : (subject != null ? subject.getSemester() : null),
                collegeClass != null ? collegeClass.getSection() : null
        );

        int totalConducted = validSessions.size();
        int totalCols = Math.max(4, 3 + totalConducted + 1);
        int lastColIndex = totalCols - 1;

        // 6. Generate Workbook
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Theory Attendance Register");
            sheet.setDisplayGridlines(true);

            // Fonts
            Font regularFont = workbook.createFont();
            regularFont.setFontName("Calibri");
            regularFont.setFontHeightInPoints((short) 10);

            Font boldFont = workbook.createFont();
            boldFont.setFontName("Calibri");
            boldFont.setFontHeightInPoints((short) 10);
            boldFont.setBold(true);

            Font boldUnderlineFont = workbook.createFont();
            boldUnderlineFont.setFontName("Calibri");
            boldUnderlineFont.setFontHeightInPoints((short) 10);
            boldUnderlineFont.setBold(true);
            boldUnderlineFont.setUnderline(Font.U_SINGLE);

            Font headerSectionFont = workbook.createFont();
            headerSectionFont.setFontName("Calibri");
            headerSectionFont.setFontHeightInPoints((short) 11);
            headerSectionFont.setBold(true);

            Font headerSectionUnderlineFont = workbook.createFont();
            headerSectionUnderlineFont.setFontName("Calibri");
            headerSectionUnderlineFont.setFontHeightInPoints((short) 11);
            headerSectionUnderlineFont.setBold(true);
            headerSectionUnderlineFont.setUnderline(Font.U_SINGLE);

            Font journalMarksFont = workbook.createFont();
            journalMarksFont.setFontName("Calibri");
            journalMarksFont.setFontHeightInPoints((short) 12);
            journalMarksFont.setBold(true);

            // Cell Styles
            CellStyle metaLabelStyle = workbook.createCellStyle();
            metaLabelStyle.setFont(headerSectionUnderlineFont);

            CellStyle metaValueStyle = workbook.createCellStyle();
            metaValueStyle.setFont(headerSectionFont);

            CellStyle journalMarksStyle = workbook.createCellStyle();
            journalMarksStyle.setFont(journalMarksFont);
            journalMarksStyle.setAlignment(HorizontalAlignment.CENTER);
            journalMarksStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(journalMarksStyle);

            CellStyle dateHeaderStyle = workbook.createCellStyle();
            dateHeaderStyle.setFont(boldFont);
            dateHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            dateHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(dateHeaderStyle);

            CellStyle dateLabelStyle = workbook.createCellStyle();
            dateLabelStyle.setFont(boldUnderlineFont);
            dateLabelStyle.setAlignment(HorizontalAlignment.CENTER);
            dateLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(dateLabelStyle);

            CellStyle sessionNoLabelStyle = workbook.createCellStyle();
            sessionNoLabelStyle.setFont(boldUnderlineFont);
            sessionNoLabelStyle.setAlignment(HorizontalAlignment.CENTER);
            sessionNoLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(sessionNoLabelStyle);

            CellStyle sessionNoCellStyle = workbook.createCellStyle();
            sessionNoCellStyle.setFont(boldFont);
            sessionNoCellStyle.setAlignment(HorizontalAlignment.CENTER);
            sessionNoCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(sessionNoCellStyle);

            CellStyle tableColHeaderStyle = workbook.createCellStyle();
            tableColHeaderStyle.setFont(headerSectionFont);
            tableColHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            tableColHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(tableColHeaderStyle);

            CellStyle percentageHeaderStyle = workbook.createCellStyle();
            percentageHeaderStyle.setFont(headerSectionFont);
            percentageHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            percentageHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            percentageHeaderStyle.setWrapText(true);
            setBorder(percentageHeaderStyle);

            CellStyle snoDataStyle = workbook.createCellStyle();
            snoDataStyle.setFont(regularFont);
            snoDataStyle.setAlignment(HorizontalAlignment.CENTER);
            snoDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(snoDataStyle);

            CellStyle studentNameDataStyle = workbook.createCellStyle();
            studentNameDataStyle.setFont(regularFont);
            studentNameDataStyle.setAlignment(HorizontalAlignment.LEFT);
            studentNameDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(studentNameDataStyle);

            CellStyle usnDataStyle = workbook.createCellStyle();
            usnDataStyle.setFont(regularFont);
            usnDataStyle.setAlignment(HorizontalAlignment.CENTER);
            usnDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(usnDataStyle);

            CellStyle statusPresentStyle = workbook.createCellStyle();
            statusPresentStyle.setFont(boldFont);
            statusPresentStyle.setAlignment(HorizontalAlignment.CENTER);
            statusPresentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(statusPresentStyle);

            CellStyle statusAbsentStyle = workbook.createCellStyle();
            statusAbsentStyle.setFont(boldFont);
            statusAbsentStyle.setAlignment(HorizontalAlignment.CENTER);
            statusAbsentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(statusAbsentStyle);

            CellStyle percentageDataStyle = workbook.createCellStyle();
            percentageDataStyle.setFont(boldFont);
            percentageDataStyle.setAlignment(HorizontalAlignment.CENTER);
            percentageDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(percentageDataStyle);

            // --- Row 1: Header Section ---
            // Faculty Name: top-left | Course Title: top section | Semester: top-right
            Row row1 = sheet.createRow(1);
            row1.setHeightInPoints(22);

            // Faculty Name (top-left: Col 0)
            Cell cellFac = row1.createCell(0);
            XSSFRichTextString richFac = new XSSFRichTextString("Faculty Name: " + facultyName);
            richFac.applyFont(0, "Faculty Name:".length(), headerSectionUnderlineFont);
            richFac.applyFont("Faculty Name:".length(), richFac.length(), headerSectionFont);
            cellFac.setCellValue(richFac);

            // Course Title (top section: Col 2)
            Cell cellTitle = row1.createCell(2);
            XSSFRichTextString richTitle = new XSSFRichTextString("Course Title: " + courseName);
            richTitle.applyFont(0, "Course Title:".length(), headerSectionUnderlineFont);
            richTitle.applyFont("Course Title:".length(), richTitle.length(), headerSectionFont);
            cellTitle.setCellValue(richTitle);

            // Semester (top-right: lastColIndex)
            int semCol = Math.max(3, lastColIndex);
            Cell cellSem = row1.createCell(semCol);
            XSSFRichTextString richSem = new XSSFRichTextString("Semester: " + semesterDisplay);
            richSem.applyFont(0, "Semester:".length(), headerSectionUnderlineFont);
            richSem.applyFont("Semester:".length(), richSem.length(), headerSectionFont);
            cellSem.setCellValue(richSem);

            // --- Row 2: Course Code below Course Title ---
            Row row2 = sheet.createRow(2);
            row2.setHeightInPoints(20);

            Cell cellCode = row2.createCell(2);
            XSSFRichTextString richCode = new XSSFRichTextString("Course Code: " + courseCode);
            richCode.applyFont(0, "Course Code:".length(), headerSectionUnderlineFont);
            richCode.applyFont("Course Code:".length(), richCode.length(), headerSectionFont);
            cellCode.setCellValue(richCode);

            // Row 3: Spacing
            Row row3 = sheet.createRow(3);
            row3.setHeightInPoints(12);

            // --- Table Header Generation ---
            int dataStartRow;

            if (totalConducted > 0) {
                // Row 4: Merged "Journal Marks" heading above session columns
                Row row4 = sheet.createRow(4);
                row4.setHeightInPoints(25);

                Cell cellJm = row4.createCell(3);
                cellJm.setCellValue("Journal Marks");
                cellJm.setCellStyle(journalMarksStyle);

                // Set styles for all cells in the Journal Marks range before merge
                for (int c = 3; c < 3 + totalConducted; c++) {
                    Cell cCell = row4.getCell(c);
                    if (cCell == null) cCell = row4.createCell(c);
                    cCell.setCellStyle(journalMarksStyle);
                }

                CellRangeAddress jmRegion = new CellRangeAddress(4, 4, 3, 3 + totalConducted - 1);
                sheet.addMergedRegion(jmRegion);
                applyRegionBorder(jmRegion, sheet);

                // Attendance Percentage (%) header (Col 3 + totalConducted, merged Rows 4 to 7)
                int pctCol = 3 + totalConducted;
                Row row5 = sheet.createRow(5);
                Row row6 = sheet.createRow(6);
                Row row7 = sheet.createRow(7);
                row5.setHeightInPoints(24);
                row6.setHeightInPoints(22);
                row7.setHeightInPoints(22);

                for (int r = 4; r <= 7; r++) {
                    Row rRow = sheet.getRow(r);
                    if (rRow == null) rRow = sheet.createRow(r);
                    Cell pCell = rRow.createCell(pctCol);
                    pCell.setCellStyle(percentageHeaderStyle);
                }
                Cell pctHeaderCell = row4.getCell(pctCol);
                if (pctHeaderCell == null) pctHeaderCell = row4.createCell(pctCol);
                pctHeaderCell.setCellValue("Attendance Percentage (%)");
                pctHeaderCell.setCellStyle(percentageHeaderStyle);

                CellRangeAddress pctRegion = new CellRangeAddress(4, 7, pctCol, pctCol);
                sheet.addMergedRegion(pctRegion);
                applyRegionBorder(pctRegion, sheet);

                // Col 0: S.No (merged Rows 5 to 7)
                for (int r = 5; r <= 7; r++) {
                    Cell snoCell = sheet.getRow(r).createCell(0);
                    snoCell.setCellStyle(tableColHeaderStyle);
                }
                Cell snoTopCell = row5.getCell(0);
                snoTopCell.setCellValue("S.No");
                CellRangeAddress snoRegion = new CellRangeAddress(5, 7, 0, 0);
                sheet.addMergedRegion(snoRegion);
                applyRegionBorder(snoRegion, sheet);

                // Col 1: Student Name (merged Rows 5 to 7)
                for (int r = 5; r <= 7; r++) {
                    Cell nameCell = sheet.getRow(r).createCell(1);
                    nameCell.setCellStyle(tableColHeaderStyle);
                }
                Cell nameTopCell = row5.getCell(1);
                nameTopCell.setCellValue("Student Name");
                CellRangeAddress nameRegion = new CellRangeAddress(5, 7, 1, 1);
                sheet.addMergedRegion(nameRegion);
                applyRegionBorder(nameRegion, sheet);

                // Col 2 in Row 5: DATE:
                Cell dateLabelCell = row5.createCell(2);
                dateLabelCell.setCellValue("DATE:");
                dateLabelCell.setCellStyle(dateLabelStyle);

                // Col 3 to Col (3+N-1) in Row 5: Session Dates
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (int i = 0; i < totalConducted; i++) {
                    AttendanceSession s = validSessions.get(i);
                    String dateStr = s.getStartTime() != null ? s.getStartTime().format(dateFmt) : "-";
                    Cell dCell = row5.createCell(3 + i);
                    dCell.setCellValue(dateStr);
                    dCell.setCellStyle(dateHeaderStyle);
                }

                // Col 2 in Row 6: Course Session No.
                Cell sessionNoLabelCell = row6.createCell(2);
                sessionNoLabelCell.setCellValue("Course Session No.");
                sessionNoLabelCell.setCellStyle(sessionNoLabelStyle);

                // Col 3 to Col (3+N-1) in Row 6 & 7: Centered sequential session numbers (1, 2, 3...)
                for (int i = 0; i < totalConducted; i++) {
                    Cell sessCell6 = row6.createCell(3 + i);
                    sessCell6.setCellValue(i + 1);
                    sessCell6.setCellStyle(sessionNoCellStyle);

                    Cell sessCell7 = row7.createCell(3 + i);
                    sessCell7.setCellStyle(sessionNoCellStyle);

                    CellRangeAddress sessRegion = new CellRangeAddress(6, 7, 3 + i, 3 + i);
                    sheet.addMergedRegion(sessRegion);
                    applyRegionBorder(sessRegion, sheet);
                }

                // Col 2 in Row 7: USN
                Cell usnLabelCell = row7.createCell(2);
                usnLabelCell.setCellValue("USN");
                usnLabelCell.setCellStyle(tableColHeaderStyle);

                dataStartRow = 8;
            } else {
                // Zero conducted sessions fallback table header
                Row row4 = sheet.createRow(4);
                row4.setHeightInPoints(24);

                String[] cols = {"S.No", "Student Name", "USN", "Attendance Percentage (%)"};
                for (int c = 0; c < cols.length; c++) {
                    Cell cell = row4.createCell(c);
                    cell.setCellValue(cols[c]);
                    cell.setCellStyle(tableColHeaderStyle);
                }
                dataStartRow = 5;
            }

            // --- Student Attendance Data Rows ---
            int currentRow = dataStartRow;
            int serialNo = 1;

            for (Student student : students) {
                Row row = sheet.createRow(currentRow++);
                row.setHeightInPoints(20);

                // S.No
                Cell cSno = row.createCell(0);
                cSno.setCellValue(serialNo++);
                cSno.setCellStyle(snoDataStyle);

                // Student Name
                Cell cName = row.createCell(1);
                cName.setCellValue(student.getName() != null ? student.getName() : "");
                cName.setCellStyle(studentNameDataStyle);

                // USN
                Cell cUsn = row.createCell(2);
                cUsn.setCellValue(student.getUsn() != null ? student.getUsn() : "");
                cUsn.setCellStyle(usnDataStyle);

                // Attendance presence cells
                int presentCount = 0;
                if (totalConducted > 0) {
                    for (int i = 0; i < totalConducted; i++) {
                        AttendanceSession s = validSessions.get(i);
                        Set<String> presentKeys = sessionPresentStudentKeys.get(s.getId());
                        boolean present = isStudentPresent(student, presentKeys);

                        Cell cStatus = row.createCell(3 + i);
                        if (present) {
                            presentCount++;
                            cStatus.setCellValue("P");
                            cStatus.setCellStyle(statusPresentStyle);
                        } else {
                            cStatus.setCellValue("A");
                            cStatus.setCellStyle(statusAbsentStyle);
                        }
                    }

                    // Attendance Percentage (%)
                    double pct = (presentCount * 100.0) / totalConducted;
                    String pctStr = (pct == Math.floor(pct))
                            ? String.format(Locale.US, "%.0f%%", pct)
                            : String.format(Locale.US, "%.1f%%", pct);

                    Cell cPct = row.createCell(3 + totalConducted);
                    cPct.setCellValue(pctStr);
                    cPct.setCellStyle(percentageDataStyle);
                } else {
                    Cell cPct = row.createCell(3);
                    cPct.setCellValue("0%");
                    cPct.setCellStyle(percentageDataStyle);
                }
            }

            // Set column widths
            sheet.setColumnWidth(0, 9 * 256);   // S.No
            sheet.setColumnWidth(1, 28 * 256);  // Student Name
            sheet.setColumnWidth(2, 22 * 256);  // USN / Labels (fits Course Session No.)
            if (totalConducted > 0) {
                for (int i = 0; i < totalConducted; i++) {
                    sheet.setColumnWidth(3 + i, 14 * 256); // Date & session columns
                }
                sheet.setColumnWidth(3 + totalConducted, 18 * 256); // Percentage
            } else {
                sheet.setColumnWidth(3, 18 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate college theory attendance register Excel file: " + e.getMessage(), e);
        }
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void applyRegionBorder(CellRangeAddress region, Sheet sheet) {
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
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
}
