package com.renaissance.service;

import com.renaissance.dto.AttendanceSummaryDto;
import com.renaissance.dto.DateWiseAttendanceRecordDto;
import com.renaissance.dto.StudentProfileDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class CsvExportService {

    private final ReportService reportService;

    public CsvExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Generates a downloadable CSV of attendance summary records with UTF-8 encoding.
     */
    public byte[] exportAttendanceSummaryCsv(String classId, String subjectId) {
        return exportAttendanceSummaryCsv(classId, subjectId, null, null);
    }

    /**
     * Generates a downloadable CSV of attendance summary records with optional date filtering.
     */
    public byte[] exportAttendanceSummaryCsv(String classId, String subjectId, LocalDate startDate, LocalDate endDate) {
        List<AttendanceSummaryDto> records = reportService.getAttendanceSummary(classId, subjectId, startDate, endDate);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(
                             "Student Name",
                             "USN / Registration Number",
                             "Class",
                             "Section",
                             "Subject",
                             "Total Classes",
                             "Classes Attended",
                             "Classes Absent",
                             "Attendance Percentage"
                     ).build())) {

            // Write UTF-8 BOM so Excel opens it with correct encoding
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            for (AttendanceSummaryDto rec : records) {
                String classDisplay = (rec.getDepartment() != null ? rec.getDepartment() : "") +
                        " Sem " + rec.getSemester();
                csvPrinter.printRecord(
                        rec.getStudentName(),
                        rec.getUsn(),
                        classDisplay,
                        rec.getSection(),
                        rec.getSubject(),
                        rec.getTotalClasses(),
                        rec.getAttended(),
                        rec.getAbsent(),
                        String.format(Locale.US, "%.1f%%", rec.getPercentage())
                );
            }

            csvPrinter.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate attendance summary CSV: " + e.getMessage(), e);
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
        StudentProfileDto profile = reportService.getStudentProfile(studentId);

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
