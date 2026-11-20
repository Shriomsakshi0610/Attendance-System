package com.renaissance.controller;

import com.renaissance.dto.*;
import com.renaissance.service.AdminService;
import com.renaissance.service.DeviceRegistrationService;
import com.renaissance.service.HistoricalAttendanceImportService;
import com.renaissance.service.TimetableService;
import jakarta.validation.Valid;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final TimetableService timetableService;
    private final com.renaissance.service.AuthService authService;
    private final DeviceRegistrationService deviceRegistrationService;
    private final HistoricalAttendanceImportService historicalAttendanceImportService;
    private final CacheManager cacheManager;

    public AdminController(AdminService adminService,
                           TimetableService timetableService,
                           com.renaissance.service.AuthService authService,
                           DeviceRegistrationService deviceRegistrationService,
                           HistoricalAttendanceImportService historicalAttendanceImportService,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) CacheManager cacheManager) {
        this.adminService = adminService;
        this.timetableService = timetableService;
        this.authService = authService;
        this.deviceRegistrationService = deviceRegistrationService;
        this.historicalAttendanceImportService = historicalAttendanceImportService;
        this.cacheManager = cacheManager;
    }

    // ----------------------------------------------------
    // ADMIN PROFILE & PASSWORD MANAGEMENT
    // ----------------------------------------------------

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changeAdminPassword(@Valid @RequestBody ResetPasswordRequest req) {
        com.renaissance.entity.Teacher current = authService.getCurrentTeacher();
        adminService.resetTeacherPassword(current.getId(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Administrator password changed successfully", null));
    }

    // ----------------------------------------------------
    // DASHBOARD OVERVIEW STATS
    // ----------------------------------------------------

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsDto>> getDashboardStats() {
        AdminDashboardStatsDto stats = adminService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok("Admin dashboard stats retrieved", stats));
    }

    // ----------------------------------------------------
    // DEPARTMENT ENDPOINTS
    // ----------------------------------------------------

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getAllDepartments() {
        List<DepartmentDto> list = adminService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<DepartmentDto>> getDepartmentById(@PathVariable String id) {
        DepartmentDto dept = adminService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.ok(dept));
    }

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<DepartmentDto>> createDepartment(@Valid @RequestBody CreateDepartmentRequest req) {
        DepartmentDto created = adminService.createDepartment(req);
        return ResponseEntity.ok(ApiResponse.ok("Department created successfully", created));
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<DepartmentDto>> updateDepartment(@PathVariable String id,
                                                                      @Valid @RequestBody CreateDepartmentRequest req) {
        DepartmentDto updated = adminService.updateDepartment(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully", updated));
    }

    @PatchMapping("/departments/{id}/toggle-status")
    public ResponseEntity<ApiResponse<DepartmentDto>> toggleDepartmentStatus(@PathVariable String id) {
        DepartmentDto updated = adminService.toggleDepartmentStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Department status toggled successfully", updated));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable String id) {
        adminService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully", null));
    }

    // ----------------------------------------------------
    // CLASS & SECTION ENDPOINTS
    // ----------------------------------------------------

    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<List<ClassDto>>> getAllClasses(@RequestParam(required = false) String departmentId) {
        List<ClassDto> list = adminService.getAllClasses(departmentId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/classes/{id}")
    public ResponseEntity<ApiResponse<ClassDto>> getClassById(@PathVariable String id) {
        ClassDto c = adminService.getClassById(id);
        return ResponseEntity.ok(ApiResponse.ok(c));
    }

    @PostMapping("/classes")
    public ResponseEntity<ApiResponse<ClassDto>> createClass(@Valid @RequestBody CreateClassRequest req) {
        ClassDto created = adminService.createClass(req);
        return ResponseEntity.ok(ApiResponse.ok("Class created successfully", created));
    }

    @PutMapping("/classes/{id}")
    public ResponseEntity<ApiResponse<ClassDto>> updateClass(@PathVariable String id,
                                                             @Valid @RequestBody CreateClassRequest req) {
        ClassDto updated = adminService.updateClass(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Class updated successfully", updated));
    }

    @PatchMapping("/classes/{id}/toggle-status")
    public ResponseEntity<ApiResponse<ClassDto>> toggleClassStatus(@PathVariable String id) {
        ClassDto updated = adminService.toggleClassStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Class status toggled successfully", updated));
    }

    @DeleteMapping("/classes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable String id) {
        adminService.deleteClass(id);
        return ResponseEntity.ok(ApiResponse.ok("Class deleted successfully", null));
    }

    @GetMapping("/classes/{id}/students")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getClassStudents(@PathVariable String id) {
        List<StudentDto> students = adminService.getClassStudents(id);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    // ----------------------------------------------------
    // SUBJECT ENDPOINTS
    // ----------------------------------------------------

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getAllSubjects(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) Integer semester) {
        List<SubjectDto> list = adminService.getAllSubjects(departmentId, semester);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<ApiResponse<SubjectDto>> getSubjectById(@PathVariable String id) {
        SubjectDto subject = adminService.getSubjectById(id);
        return ResponseEntity.ok(ApiResponse.ok(subject));
    }

    @PostMapping("/subjects")
    public ResponseEntity<ApiResponse<SubjectDto>> createSubject(@Valid @RequestBody CreateSubjectRequest req) {
        SubjectDto created = adminService.createSubject(req);
        return ResponseEntity.ok(ApiResponse.ok("Subject created successfully", created));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<ApiResponse<SubjectDto>> updateSubject(@PathVariable String id,
                                                                 @Valid @RequestBody CreateSubjectRequest req) {
        SubjectDto updated = adminService.updateSubject(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Subject updated successfully", updated));
    }

    @PatchMapping("/subjects/{id}/toggle-status")
    public ResponseEntity<ApiResponse<SubjectDto>> toggleSubjectStatus(@PathVariable String id) {
        SubjectDto updated = adminService.toggleSubjectStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Subject status toggled successfully", updated));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable String id) {
        adminService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.ok("Subject deleted successfully", null));
    }

    // ----------------------------------------------------
    // FACULTY / TEACHER ENDPOINTS
    // ----------------------------------------------------

    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse<List<TeacherDto>>> getAllTeachers() {
        List<TeacherDto> list = adminService.getAllTeachers();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<TeacherDto>> getTeacherById(@PathVariable String id) {
        TeacherDto teacher = adminService.getTeacherById(id);
        return ResponseEntity.ok(ApiResponse.ok(teacher));
    }

    @PostMapping("/teachers")
    public ResponseEntity<ApiResponse<TeacherDto>> createTeacher(@Valid @RequestBody CreateTeacherRequest req) {
        TeacherDto created = adminService.createTeacher(req);
        return ResponseEntity.ok(ApiResponse.ok("Teacher created successfully", created));
    }

    @PutMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<TeacherDto>> updateTeacher(@PathVariable String id,
                                                                @Valid @RequestBody CreateTeacherRequest req) {
        TeacherDto updated = adminService.updateTeacher(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Teacher updated successfully", updated));
    }

    @PatchMapping("/teachers/{id}/toggle-status")
    public ResponseEntity<ApiResponse<TeacherDto>> toggleTeacherStatus(@PathVariable String id) {
        TeacherDto updated = adminService.toggleTeacherStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Teacher status toggled successfully", updated));
    }

    @PostMapping("/teachers/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetTeacherPassword(@PathVariable String id,
                                                                 @Valid @RequestBody ResetPasswordRequest req) {
        adminService.resetTeacherPassword(id, req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Teacher password reset successfully", null));
    }

    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(@PathVariable String id) {
        adminService.deleteTeacher(id);
        return ResponseEntity.ok(ApiResponse.ok("Teacher deleted successfully", null));
    }

    // ----------------------------------------------------
    // STUDENT ENDPOINTS
    // ----------------------------------------------------

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getAllStudents(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String keyword) {
        List<StudentDto> students = adminService.getAllStudents(departmentId, classId, keyword);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<StudentDto>> getStudentById(@PathVariable String id) {
        StudentDto student = adminService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.ok(student));
    }

    @PostMapping("/students")
    public ResponseEntity<ApiResponse<StudentDto>> createStudent(@Valid @RequestBody StudentDto dto) {
        StudentDto created = adminService.createStudent(dto);
        return ResponseEntity.ok(ApiResponse.ok("Student created successfully", created));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<ApiResponse<StudentDto>> updateStudent(@PathVariable String id,
                                                                 @Valid @RequestBody StudentDto dto) {
        StudentDto updated = adminService.updateStudent(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Student updated successfully", updated));
    }

    @PatchMapping("/students/{id}/toggle-status")
    public ResponseEntity<ApiResponse<StudentDto>> toggleStudentStatus(@PathVariable String id) {
        StudentDto updated = adminService.toggleStudentStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Student status toggled successfully", updated));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable String id) {
        adminService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.ok("Student deleted successfully", null));
    }

    @PostMapping("/students/import")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importStudents(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = adminService.importStudentsFromCsv(file);
        return ResponseEntity.ok(ApiResponse.ok("CSV import process completed", result));
    }

    // ----------------------------------------------------
    // TIMETABLE ENDPOINTS
    // ----------------------------------------------------

    @GetMapping("/timetable")
    public ResponseEntity<ApiResponse<List<TimetableScheduleDto>>> getTimetable(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) String departmentId) {

        List<TimetableScheduleDto> list;
        if (classId != null && !classId.isBlank()) {
            list = timetableService.getSchedulesByClass(classId);
        } else if (teacherId != null && !teacherId.isBlank()) {
            list = timetableService.getSchedulesByTeacher(teacherId);
        } else if (departmentId != null && !departmentId.isBlank()) {
            list = timetableService.getSchedulesByDepartment(departmentId);
        } else {
            list = timetableService.getAllSchedules();
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/timetable")
    public ResponseEntity<ApiResponse<TimetableScheduleDto>> createSchedule(
            @Valid @RequestBody CreateTimetableScheduleRequest req) {
        TimetableScheduleDto created = timetableService.createSchedule(req);
        return ResponseEntity.ok(ApiResponse.ok("Timetable schedule created successfully", created));
    }

    @PutMapping("/timetable/{id}")
    public ResponseEntity<ApiResponse<TimetableScheduleDto>> updateSchedule(
            @PathVariable String id,
            @Valid @RequestBody CreateTimetableScheduleRequest req) {
        TimetableScheduleDto updated = timetableService.updateSchedule(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Timetable schedule updated successfully", updated));
    }

    @DeleteMapping("/timetable/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable String id) {
        timetableService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.ok("Timetable schedule deleted successfully", null));
    }

    // ----------------------------------------------------
    // ACTIVE SESSIONS & CAMPUS-WIDE MONITORING
    // ----------------------------------------------------

    @GetMapping("/sessions/active")
    public ResponseEntity<ApiResponse<List<SessionDto>>> getActiveSessions() {
        List<SessionDto> sessions = adminService.getActiveSessions();
        return ResponseEntity.ok(ApiResponse.ok(sessions));
    }

    @PostMapping("/sessions/{id}/terminate")
    public ResponseEntity<ApiResponse<SessionDto>> terminateSession(@PathVariable String id) {
        SessionDto terminated = adminService.terminateSession(id);
        return ResponseEntity.ok(ApiResponse.ok("Attendance session terminated successfully by administrator", terminated));
    }

    // ----------------------------------------------------
    // MODULE 4: STUDENT DEVICE REGISTRATION & MANAGEMENT
    // ----------------------------------------------------

    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<DeviceDto>>> getAllDevices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        List<DeviceDto> devices = deviceRegistrationService.getAllDevices(status, keyword);
        return ResponseEntity.ok(ApiResponse.ok(devices));
    }

    @PostMapping("/devices/{id}/approve")
    public ResponseEntity<ApiResponse<DeviceDto>> approveDevice(@PathVariable String id) {
        com.renaissance.entity.Teacher current = authService.getCurrentTeacher();
        DeviceDto approved = deviceRegistrationService.approveDevice(id, current.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Student device approved successfully", approved));
    }

    @PostMapping("/devices/{id}/reject")
    public ResponseEntity<ApiResponse<DeviceDto>> rejectDevice(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        com.renaissance.entity.Teacher current = authService.getCurrentTeacher();
        String reason = body != null ? body.get("reason") : "Rejected by administrator";
        DeviceDto rejected = deviceRegistrationService.rejectDevice(id, reason, current.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Student device rejected", rejected));
    }

    @PostMapping("/devices/{id}/revoke")
    public ResponseEntity<ApiResponse<DeviceDto>> revokeDevice(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        com.renaissance.entity.Teacher current = authService.getCurrentTeacher();
        String reason = body != null ? body.get("reason") : "Revoked by administrator";
        DeviceDto revoked = deviceRegistrationService.revokeDevice(id, reason, current.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Student device authorization revoked", revoked));
    }

    @GetMapping("/devices/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getDeviceStats() {
        Map<String, Long> stats = deviceRegistrationService.getDeviceStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    // ----------------------------------------------------
    // MODULE 2: HISTORICAL ATTENDANCE CSV IMPORT
    // ----------------------------------------------------

    @PostMapping("/attendance/import/preview")
    public ResponseEntity<ApiResponse<HistoricalImportPreviewDto>> previewHistoricalAttendance(
            @RequestParam("file") MultipartFile file) {
        com.renaissance.entity.Teacher current = authService.getCurrentTeacher();
        HistoricalImportPreviewDto preview = historicalAttendanceImportService.previewHistoricalAttendanceCsv(file, current.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Historical attendance CSV parsed successfully", preview));
    }

    @PostMapping("/attendance/import/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmHistoricalImport(
            @RequestBody Map<String, String> body) {
        String importToken = body.get("importToken");
        String fileName = body.get("fileName");
        com.renaissance.entity.Teacher current = authService.getCurrentTeacher();
        Map<String, Object> result = historicalAttendanceImportService.confirmHistoricalImport(importToken, fileName, current.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Historical attendance imported successfully", result));
    }

    @PostMapping("/attendance/import/error-csv")
    public ResponseEntity<byte[]> downloadErrorReportCsv(@RequestBody Map<String, List<String>> body) {
        List<String> errors = body.get("errors");
        byte[] csv = historicalAttendanceImportService.generateErrorReportCsv(errors);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historical_attendance_import_errors.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    // ----------------------------------------------------
    // MODULE 5: CAFFEINE CACHE METRICS & MONITORING
    // ----------------------------------------------------

    @GetMapping("/system/cache-metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheMetrics() {
        Map<String, Object> result = new LinkedHashMap<>();

        if (cacheManager instanceof CaffeineCacheManager ccm) {
            Map<String, Object> cachesStats = new LinkedHashMap<>();
            long totalHits = 0;
            long totalMisses = 0;
            long totalEvictions = 0;

            for (String cacheName : ccm.getCacheNames()) {
                org.springframework.cache.Cache springCache = ccm.getCache(cacheName);
                if (springCache != null && springCache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache) {
                    com.github.benmanes.caffeine.cache.stats.CacheStats stats = nativeCache.stats();

                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("hitCount", stats.hitCount());
                    s.put("missCount", stats.missCount());
                    s.put("hitRate", Math.round(stats.hitRate() * 1000.0) / 10.0 + "%");
                    s.put("missRate", Math.round(stats.missRate() * 1000.0) / 10.0 + "%");
                    s.put("evictionCount", stats.evictionCount());
                    s.put("estimatedSize", nativeCache.estimatedSize());

                    totalHits += stats.hitCount();
                    totalMisses += stats.missCount();
                    totalEvictions += stats.evictionCount();

                    cachesStats.put(cacheName, s);
                }
            }

            long totalRequests = totalHits + totalMisses;
            double overallHitRate = totalRequests > 0 ? ((double) totalHits / totalRequests) * 100.0 : 0.0;

            result.put("cacheType", "Caffeine (In-Process L1 RAM)");
            result.put("totalCaches", cachesStats.size());
            result.put("overallHitRate", Math.round(overallHitRate * 10.0) / 10.0 + "%");
            result.put("totalHits", totalHits);
            result.put("totalMisses", totalMisses);
            result.put("totalEvictions", totalEvictions);
            result.put("caches", cachesStats);
        } else {
            result.put("cacheType", cacheManager != null ? cacheManager.getClass().getSimpleName() : "None");
            result.put("message", "Cache statistics not available for current cache manager");
        }

        return ResponseEntity.ok(ApiResponse.ok("System cache performance metrics", result));
    }
}
