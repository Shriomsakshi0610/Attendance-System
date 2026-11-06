package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.ClassDto;
import com.renaissance.dto.DepartmentDto;
import com.renaissance.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ClassController {

    private final TeacherService teacherService;

    public ClassController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/api/classes")
    public ResponseEntity<ApiResponse<List<ClassDto>>> getClasses(@RequestParam(required = false) Long departmentId) {
        List<ClassDto> classes = teacherService.getClassesByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.ok(classes));
    }

    @GetMapping("/api/public/departments")
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getPublicDepartments() {
        List<DepartmentDto> departments = teacherService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.ok(departments));
    }

    @GetMapping("/api/public/classes")
    public ResponseEntity<ApiResponse<List<ClassDto>>> getPublicClasses(@RequestParam(required = false) Long departmentId) {
        List<ClassDto> classes = teacherService.getClassesByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.ok(classes));
    }
}
