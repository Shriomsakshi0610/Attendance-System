package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.SubjectDto;
import com.renaissance.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final TeacherService teacherService;

    public SubjectController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getSubjects(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer semester) {
        List<SubjectDto> subjects = teacherService.getSubjects(departmentId, semester);
        return ResponseEntity.ok(ApiResponse.ok(subjects));
    }
}
