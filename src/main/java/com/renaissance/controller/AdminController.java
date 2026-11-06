package com.renaissance.controller;

import com.renaissance.dto.*;
import com.renaissance.service.AdminService;
import com.renaissance.service.TimetableService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final TimetableService timetableService;

    public AdminController(AdminService adminService, TimetableService timetableService) {
        this.adminService = adminService;
        this.timetableService = timetableService;
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
}
