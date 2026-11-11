package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.AttendanceSummaryDto;
import com.renaissance.dto.DashboardStatsDto;
import com.renaissance.dto.DateWiseAttendanceRecordDto;
import com.renaissance.dto.StudentProfileDto;
import com.renaissance.service.CsvExportService;
import com.renaissance.service.ExcelExportService;
import com.renaissance.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final ExcelExportService excelExportService;

    public ReportController(ReportService reportService, CsvExportService csvExportService, ExcelExportService excelExportService) {
        this.reportService = reportService;
        this.csvExportService = csvExportService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        DashboardStatsDto stats = reportService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/attendance-summary")
    public ResponseEntity<ApiResponse<List<AttendanceSummaryDto>>> getAttendanceSummary(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceSummaryDto> summary = reportService.getAttendanceSummary(classId, subjectId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/student-profile/{id}")
    public ResponseEntity<ApiResponse<StudentProfileDto>> getStudentProfile(
            @PathVariable String id,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId) {
        StudentProfileDto profile = reportService.getStudentProfile(id, classId, subjectId);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @GetMapping("/datewise")
    public ResponseEntity<ApiResponse<List<DateWiseAttendanceRecordDto>>> getDateWiseAttendance(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DateWiseAttendanceRecordDto> records = reportService.getDateWiseAttendance(classId, subjectId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(records));
    }

    @GetMapping("/export/summary-csv")
    public ResponseEntity<byte[]> exportSummaryCsv(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] csvData = csvExportService.exportAttendanceSummaryCsv(classId, subjectId, startDate, endDate);
        String filename = "Attendance_Summary_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping({"/export/register-excel", "/export/summary-excel"})
    public ResponseEntity<byte[]> exportRegisterExcel(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] excelData = excelExportService.exportAttendanceRegisterExcel(classId, subjectId, startDate, endDate);
        String filename = "Theory_Attendance_Register_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/export/datewise-csv")
    public ResponseEntity<byte[]> exportDateWiseCsv(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] csvData = csvExportService.exportDateWiseAttendanceCsv(classId, subjectId, startDate, endDate);
        String filename = "Attendance_Datewise_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/export/student-csv/{id}")
    public ResponseEntity<byte[]> exportStudentCsv(
            @PathVariable String id,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId) {
        byte[] csvData = csvExportService.exportStudentHistoryCsv(id, classId, subjectId);
        String filename = "Attendance_Student_" + id + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
