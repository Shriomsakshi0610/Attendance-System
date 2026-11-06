package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.StudentDto;
import com.renaissance.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentDto>>> getAllStudents(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String keyword) {
        List<StudentDto> students = studentService.getAllStudents(departmentId, classId, keyword);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDto>> getStudentById(@PathVariable String id) {
        StudentDto student = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.ok(student));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentDto>> createStudent(@Valid @RequestBody StudentDto dto) {
        StudentDto created = studentService.createStudent(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Student created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDto>> updateStudent(@PathVariable String id, @Valid @RequestBody StudentDto dto) {
        StudentDto updated = studentService.updateStudent(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Student updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable String id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.ok("Student deleted successfully", null));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importStudentsFromCsv(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = studentService.importStudentsFromCsv(file);
        return ResponseEntity.ok(ApiResponse.ok("CSV processed successfully", result));
    }
}
