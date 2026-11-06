package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.LoginRequest;
import com.renaissance.dto.LoginResponse;
import com.renaissance.dto.TeacherDto;
import com.renaissance.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest,
                                                           HttpServletResponse httpResponse) {
        LoginResponse response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<TeacherDto>> getAuthStatus() {
        try {
            TeacherDto teacher = authService.getCurrentTeacherDto();
            return ResponseEntity.ok(ApiResponse.ok(teacher));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(false, "Not authenticated", null, "UNAUTHENTICATED"));
        }
    }
}
