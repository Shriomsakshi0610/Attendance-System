package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.LoginRequest;
import com.renaissance.dto.LoginResponse;
import com.renaissance.dto.RefreshTokenRequest;
import com.renaissance.dto.RefreshResponse;
import com.renaissance.dto.TeacherDto;
import com.renaissance.exception.UnauthorizedException;
import com.renaissance.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refreshSession(
            @RequestBody(required = false) RefreshTokenRequest request,
            @RequestHeader(value = "X-Refresh-Token", required = false) String headerToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String token = (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank())
                ? request.getRefreshToken()
                : headerToken;

        String deviceInfo = (request != null && request.getDeviceInfo() != null)
                ? request.getDeviceInfo()
                : httpRequest.getHeader("User-Agent");

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Refresh token is missing", null, "MISSING_REFRESH_TOKEN"));
        }

        try {
            RefreshResponse response = authService.silentRenew(token, deviceInfo, httpRequest, httpResponse);
            return ResponseEntity.ok(ApiResponse.ok("Session renewed successfully", response));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, e.getMessage(), null, "INVALID_REFRESH_TOKEN"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Unable to renew session", null, "RENEWAL_FAILED"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @RequestHeader(value = "X-Refresh-Token", required = false) String headerToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String token = (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank())
                ? request.getRefreshToken()
                : headerToken;

        authService.logout(httpRequest, token);

        jakarta.servlet.http.Cookie sessionCookie = new jakarta.servlet.http.Cookie("RENAISSANCE_SESSION", "");
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(0);
        sessionCookie.setHttpOnly(true);
        httpResponse.addCookie(sessionCookie);

        jakarta.servlet.http.Cookie jsessionCookie = new jakarta.servlet.http.Cookie("JSESSIONID", "");
        jsessionCookie.setPath("/");
        jsessionCookie.setMaxAge(0);
        jsessionCookie.setHttpOnly(true);
        httpResponse.addCookie(jsessionCookie);

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
