package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.ClassDto;
import com.renaissance.dto.DepartmentDto;
import com.renaissance.dto.SubjectDto;
import com.renaissance.dto.TeacherDto;
import com.renaissance.service.AuthService;
import com.renaissance.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final AuthService authService;
    private final TeacherService teacherService;

    public TeacherController(AuthService authService, TeacherService teacherService) {
        this.authService = authService;
        this.teacherService = teacherService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<TeacherDto>> getProfile() {
        TeacherDto teacher = authService.getCurrentTeacherDto();
        return ResponseEntity.ok(ApiResponse.ok(teacher));
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getDepartments() {
        List<DepartmentDto> departments = teacherService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.ok(departments));
    }

    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<List<ClassDto>>> getClasses(@RequestParam(required = false) String departmentId) {
        List<ClassDto> classes = teacherService.getClassesByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.ok(classes));
    }

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getSubjects(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) Integer semester) {
        List<SubjectDto> subjects = teacherService.getSubjects(departmentId, semester);
        return ResponseEntity.ok(ApiResponse.ok(subjects));
    }
}
