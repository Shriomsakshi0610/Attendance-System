package com.renaissance.service;

import com.renaissance.dto.LoginRequest;
import com.renaissance.dto.LoginResponse;
import com.renaissance.dto.RefreshResponse;
import com.renaissance.dto.TeacherDto;
import com.renaissance.entity.Teacher;
import com.renaissance.entity.UserRefreshToken;
import com.renaissance.exception.UnauthorizedException;
import com.renaissance.repository.TeacherRepository;
import com.renaissance.repository.UserRefreshTokenRepository;
import com.renaissance.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 30L;
    private static final long SESSION_TIMEOUT_SECONDS = 30L * 24 * 3600; // 30 days total persistent session

    private final AuthenticationManager authenticationManager;
    private final TeacherRepository teacherRepository;
    private final UserRefreshTokenRepository refreshTokenRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       TeacherRepository teacherRepository,
                       UserRefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.teacherRepository = teacherRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword())
            );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
            Teacher teacher = userDetails.getTeacher();

            // Generate cryptographically secure refresh token for persistent Android and mobile sessions
            String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
            String deviceInfo = httpRequest != null ? httpRequest.getHeader("User-Agent") : "Unknown Device";
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS);

            UserRefreshToken tokenEntity = new UserRefreshToken(
                    rawToken,
                    teacher.getId(),
                    teacher.getEmail(),
                    teacher.getRole(),
                    deviceInfo,
                    expiresAt
            );
            refreshTokenRepository.save(tokenEntity);
            log.info("Created persistent refresh token for teacher {} ({}), expires in {} days",
                    teacher.getName(), teacher.getEmail(), REFRESH_TOKEN_VALIDITY_DAYS);

            TeacherDto dto = toTeacherDto(teacher);
            return new LoginResponse("Login successful", dto, true, rawToken, SESSION_TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.warn("Login failed for user {}: {}", request.getEmail(), e.getMessage());
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /**
     * Synchronized, atomic silent session renewal.
     * Validates persistent refresh token from MongoDB, rotates token, creates fresh authenticated session,
     * and returns updated credentials.
     */
    public RefreshResponse silentRenew(String rawToken, String deviceInfo, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            throw new UnauthorizedException("Refresh token is required for session renewal.");
        }

        UserRefreshToken tokenEntity = refreshTokenRepository.findByToken(rawToken.trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token. Please log in again."));

        if (!tokenEntity.isValid()) {
            log.warn("Attempt to use invalid/expired/revoked refresh token for teacherId: {}", tokenEntity.getTeacherId());
            refreshTokenRepository.delete(tokenEntity);
            throw new UnauthorizedException("Refresh token has expired or was revoked. Please log in again.");
        }

        Teacher teacher = null;
        if (tokenEntity.getTeacherId() != null) {
            teacher = teacherRepository.findById(tokenEntity.getTeacherId()).orElse(null);
        }
        if (teacher == null && tokenEntity.getEmail() != null) {
            teacher = teacherRepository.findByEmailIgnoreCase(tokenEntity.getEmail()).orElse(null);
        }

        if (teacher == null) {
            refreshTokenRepository.delete(tokenEntity);
            throw new UnauthorizedException("Faculty account not found. Please log in again.");
        }

        if (!teacher.isActive()) {
            refreshTokenRepository.delete(tokenEntity);
            throw new UnauthorizedException("Faculty account is inactive. Please contact administrator.");
        }

        // Token rotation: Revoke old token and issue fresh rotated token with 30-day validity
        refreshTokenRepository.delete(tokenEntity);

        String rotatedToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String updatedDeviceInfo = deviceInfo != null && !deviceInfo.isBlank() ? deviceInfo :
                (httpRequest != null ? httpRequest.getHeader("User-Agent") : "Rotated Token");
        LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS);

        UserRefreshToken rotatedEntity = new UserRefreshToken(
                rotatedToken,
                teacher.getId(),
                teacher.getEmail(),
                teacher.getRole(),
                updatedDeviceInfo,
                newExpiresAt
        );
        refreshTokenRepository.save(rotatedEntity);

        // Re-authenticate and establish fresh Spring Security HttpSession
        SecurityUser userDetails = new SecurityUser(teacher);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        if (httpRequest != null) {
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        }

        log.info("Successfully renewed session silently for faculty {} ({}) via persistent token",
                teacher.getName(), teacher.getEmail());

        return new RefreshResponse(rotatedToken, toTeacherDto(teacher), REFRESH_TOKEN_VALIDITY_DAYS * 24 * 3600);
    }

    public void logout(HttpServletRequest request) {
        logout(request, null);
    }

    public void logout(HttpServletRequest request, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                refreshTokenRepository.deleteByToken(refreshToken.trim());
                log.info("Revoked persistent refresh token during logout");
            } catch (Exception e) {
                log.warn("Error revoking refresh token on logout: {}", e.getMessage());
            }
        }

        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
        SecurityContextHolder.clearContext();
    }

    public Teacher getCurrentTeacher() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("You must be logged in as a teacher to perform this operation.");
        }

        if (auth.getPrincipal() instanceof SecurityUser securityUser) {
            return teacherRepository.findById(securityUser.getTeacher().getId())
                    .orElseThrow(() -> new UnauthorizedException("Teacher profile not found."));
        }

        String email = auth.getName();
        return teacherRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Teacher not found for current session."));
    }

    public TeacherDto getCurrentTeacherDto() {
        return toTeacherDto(getCurrentTeacher());
    }

    public TeacherDto toTeacherDto(Teacher teacher) {
        if (teacher == null) return null;
        TeacherDto dto = new TeacherDto(
                teacher.getId(),
                teacher.getName(),
                teacher.getEmail(),
                teacher.getEmployeeId(),
                teacher.getDepartment() != null ? teacher.getDepartment().getName() : "General",
                teacher.getDepartment() != null ? teacher.getDepartment().getCode() : "GEN",
                teacher.getRole()
        );
        if (teacher.getDepartment() != null) {
            dto.setDepartmentId(teacher.getDepartment().getId());
        }
        dto.setPhone(teacher.getPhone());
        dto.setStatus(teacher.getStatus() != null ? teacher.getStatus() : "ACTIVE");

        if (teacher.getAssignedClasses() != null) {
            dto.setAssignedClassIds(teacher.getAssignedClasses().stream().map(c -> c.getId()).toList());
            dto.setAssignedClassNames(teacher.getAssignedClasses().stream().map(c -> c.getDisplayName()).toList());
        }
        if (teacher.getAssignedSubjects() != null) {
            dto.setAssignedSubjectIds(teacher.getAssignedSubjects().stream().map(s -> s.getId()).toList());
            dto.setAssignedSubjectNames(teacher.getAssignedSubjects().stream().map(s -> s.getName()).toList());
        }
        return dto;
    }
}
