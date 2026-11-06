package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.AttendanceSummaryDto;
import com.renaissance.dto.DashboardStatsDto;
import com.renaissance.dto.StudentProfileDto;
import com.renaissance.service.CsvExportService;
import com.renaissance.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CsvExportService csvExportService;

    public ReportController(ReportService reportService, CsvExportService csvExportService) {
        this.reportService = reportService;
        this.csvExportService = csvExportService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        DashboardStatsDto stats = reportService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/attendance-summary")
    public ResponseEntity<ApiResponse<List<AttendanceSummaryDto>>> getAttendanceSummary(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long subjectId) {
        List<AttendanceSummaryDto> summary = reportService.getAttendanceSummary(classId, subjectId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/student-profile/{id}")
    public ResponseEntity<ApiResponse<StudentProfileDto>> getStudentProfile(@PathVariable Long id) {
        StudentProfileDto profile = reportService.getStudentProfile(id);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @GetMapping("/export/summary-csv")
    public ResponseEntity<byte[]> exportSummaryCsv(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long subjectId) {
        byte[] csvData = csvExportService.exportAttendanceSummaryCsv(classId, subjectId);
        String filename = "renaissance_attendance_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/export/student-csv/{id}")
    public ResponseEntity<byte[]> exportStudentCsv(@PathVariable Long id) {
        byte[] csvData = csvExportService.exportStudentHistoryCsv(id);
        String filename = "renaissance_student_" + id + "_history.csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
