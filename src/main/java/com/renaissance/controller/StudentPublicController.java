package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.DeviceRegistrationRequest;
import com.renaissance.dto.DeviceStatusResponse;
import com.renaissance.dto.StudentDetectionResponse;
import com.renaissance.service.DeviceRegistrationService;
import com.renaissance.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class StudentPublicController {

    private final StudentService studentService;
    private final DeviceRegistrationService deviceRegistrationService;

    public StudentPublicController(StudentService studentService,
                                  DeviceRegistrationService deviceRegistrationService) {
        this.studentService = studentService;
        this.deviceRegistrationService = deviceRegistrationService;
    }

    /**
     * Module 1: Smart USN Detection & Auto-fill.
     * Resolves the student's full institutional USN using the selected class and last 3 digits.
     */
    @GetMapping("/student/detect-usn")
    public ResponseEntity<ApiResponse<StudentDetectionResponse>> detectUsn(
            @RequestParam String classId,
            @RequestParam String digits) {
        StudentDetectionResponse response = studentService.detectStudentByLastThreeDigits(classId, digits);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/student/detect-usn")
    public ResponseEntity<ApiResponse<StudentDetectionResponse>> detectUsnPost(
            @RequestBody Map<String, String> body) {
        String classId = body.get("classId");
        String digits = body.get("digits");
        StudentDetectionResponse response = studentService.detectStudentByLastThreeDigits(classId, digits);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Module 4: Check if current student device is registered and approved.
     */
    @PostMapping("/device/check-status")
    public ResponseEntity<ApiResponse<DeviceStatusResponse>> checkDeviceStatus(
            @RequestBody Map<String, String> body) {
        String usn = body.get("usn");
        String deviceToken = body.get("deviceToken");
        DeviceStatusResponse response = deviceRegistrationService.checkDeviceStatus(usn, deviceToken);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Module 4: Submit a device registration request from student device.
     */
    @PostMapping("/device/register")
    public ResponseEntity<ApiResponse<DeviceStatusResponse>> requestDeviceRegistration(
            @Valid @RequestBody DeviceRegistrationRequest request,
            HttpServletRequest httpRequest) {
        DeviceStatusResponse response = deviceRegistrationService.requestRegistration(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Device registration request submitted successfully", response));
    }
}
