package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.ClassDto;
import com.renaissance.dto.DepartmentDto;
import com.renaissance.dto.StudentDto;
import com.renaissance.dto.SubjectDto;
import com.renaissance.dto.TeacherDto;
import com.renaissance.entity.Teacher;
import com.renaissance.service.AuthService;
import com.renaissance.service.StudentService;
import com.renaissance.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final AuthService authService;
    private final TeacherService teacherService;
    private final StudentService studentService;

    public TeacherController(AuthService authService, TeacherService teacherService, StudentService studentService) {
        this.authService = authService;
        this.teacherService = teacherService;
        this.studentService = studentService;
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
        Teacher teacher = authService.getCurrentTeacher();
        List<ClassDto> classes = teacherService.getClassesForTeacher(teacher, departmentId);
        return ResponseEntity.ok(ApiResponse.ok(classes));
    }

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getSubjects(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) Integer semester) {
        Teacher teacher = authService.getCurrentTeacher();
        List<SubjectDto> subjects = teacherService.getSubjectsForTeacher(teacher, departmentId, semester);
        return ResponseEntity.ok(ApiResponse.ok(subjects));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getAssignedStudents(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String keyword) {
        Teacher teacher = authService.getCurrentTeacher();
        List<StudentDto> students = studentService.getStudentsForTeacher(teacher, classId, keyword);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }
}
