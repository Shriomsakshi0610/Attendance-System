package com.renaissance.service;

import com.renaissance.dto.AttendanceSummaryDto;
import com.renaissance.dto.StudentProfileDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CsvExportService {

    private final ReportService reportService;

    public CsvExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Generates a downloadable CSV of attendance summary records with UTF-8 encoding.
     */
    public byte[] exportAttendanceSummaryCsv(Long classId, Long subjectId) {
        List<AttendanceSummaryDto> records = reportService.getAttendanceSummary(classId, subjectId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(
                             "USN",
                             "Student Name",
                             "Department",
                             "Semester",
                             "Section",
                             "Subject",
                             "Total Classes",
                             "Present",
                             "Absent",
                             "Attendance Percentage"
                     ).build())) {

            // Write UTF-8 BOM so Excel opens it with correct encoding
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            for (AttendanceSummaryDto rec : records) {
                csvPrinter.printRecord(
                        rec.getUsn(),
                        rec.getStudentName(),
                        rec.getDepartment(),
                        rec.getSemester(),
                        rec.getSection(),
                        rec.getSubject(),
                        rec.getTotalClasses(),
                        rec.getAttended(),
                        rec.getAbsent(),
                        String.format("%.2f%%", rec.getPercentage())
                );
            }

            csvPrinter.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate attendance summary CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a downloadable CSV of an individual student's complete attendance history.
     */
    public byte[] exportStudentHistoryCsv(Long studentId) {
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
