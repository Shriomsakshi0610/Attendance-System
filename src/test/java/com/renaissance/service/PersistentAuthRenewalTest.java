package com.renaissance.service;

import com.renaissance.dto.LoginRequest;
import com.renaissance.dto.LoginResponse;
import com.renaissance.dto.RefreshResponse;
import com.renaissance.entity.Department;
import com.renaissance.entity.Teacher;
import com.renaissance.entity.UserRefreshToken;
import com.renaissance.exception.UnauthorizedException;
import com.renaissance.repository.TeacherRepository;
import com.renaissance.repository.UserRefreshTokenRepository;
import com.renaissance.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersistentAuthRenewalTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private UserRefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @Mock
    private HttpSession httpSession;

    private AuthService authService;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, teacherRepository, refreshTokenRepository);
        SecurityContextHolder.clearContext();

        Department dept = new Department();
        dept.setId("dept-ise");
        dept.setName("Information Science and Engineering");
        dept.setCode("ISE");

        testTeacher = new Teacher("Dr. Alan Turing", "turing@renaissance.edu", "hashed_pwd", "EMP007", dept, "ROLE_TEACHER");
        testTeacher.setId("teacher-123");
        testTeacher.setActive(true);
    }

    @Test
    @DisplayName("Login creates and persists long-lived refresh token in MongoDB")
    void testLoginGeneratesAndPersistsRefreshToken() {
        LoginRequest request = new LoginRequest("turing@renaissance.edu", "Password@123");
        SecurityUser userDetails = new SecurityUser(testTeacher);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(httpRequest.getSession(true)).thenReturn(httpSession);
        when(httpRequest.getHeader("User-Agent")).thenReturn("RenaissanceAndroid/1.3.3");

        LoginResponse response = authService.login(request, httpRequest, httpResponse);

        assertNotNull(response);
        assertTrue(response.isAuthenticated());
        assertNotNull(response.getRefreshToken());
        assertFalse(response.getRefreshToken().isBlank());
        assertEquals(30L * 24 * 3600, response.getSessionTimeoutSeconds());

        verify(refreshTokenRepository, times(1)).save(argThat(token ->
                token.getTeacherId().equals("teacher-123") &&
                token.getEmail().equals("turing@renaissance.edu") &&
                token.getToken().equals(response.getRefreshToken()) &&
                token.getExpiresAt().isAfter(LocalDateTime.now().plusDays(28))
        ));

        verify(httpSession, times(1)).setAttribute(eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
    }

    @Test
    @DisplayName("Silent renewal successfully validates token, rotates token with grace period, and re-authenticates session")
    void testSilentRenewalSuccess() {
        String oldToken = "old-refresh-token-xyz-123";
        UserRefreshToken tokenEntity = new UserRefreshToken(
                oldToken,
                "teacher-123",
                "turing@renaissance.edu",
                "ROLE_TEACHER",
                "Android Pixel 8",
                LocalDateTime.now().plusDays(15)
        );

        when(refreshTokenRepository.findByToken(oldToken)).thenReturn(Optional.of(tokenEntity));
        when(teacherRepository.findById("teacher-123")).thenReturn(Optional.of(testTeacher));
        when(httpRequest.getSession(true)).thenReturn(httpSession);

        RefreshResponse refreshResponse = authService.silentRenew(oldToken, "Android Pixel 8", httpRequest, httpResponse);

        assertNotNull(refreshResponse);
        assertNotNull(refreshResponse.getRefreshToken());
        assertNotEquals(oldToken, refreshResponse.getRefreshToken(), "Token rotation must issue a new token");
        assertEquals("Dr. Alan Turing", refreshResponse.getTeacher().getName());

        // Verify old token was marked rotated with replacement reference rather than deleted immediately
        assertTrue(tokenEntity.isRotated());
        assertEquals(refreshResponse.getRefreshToken(), tokenEntity.getReplacedByToken());
        verify(refreshTokenRepository, times(1)).save(tokenEntity);

        // Verify new rotated token was saved
        verify(refreshTokenRepository, times(1)).save(argThat(saved ->
                saved.getToken().equals(refreshResponse.getRefreshToken()) &&
                saved.getTeacherId().equals("teacher-123") &&
                !saved.isRotated()
        ));

        // Verify fresh authenticated SecurityContext was stored in session
        verify(httpSession, times(1)).setAttribute(eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("turing@renaissance.edu", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("Silent renewal satisfies concurrent requests within 60-second grace window")
    void testSilentRenewalGracePeriodForConcurrentRequests() {
        String rotatedToken = "prior-token-rotated-abc";
        String activeReplacementToken = "active-replacement-token-xyz";

        UserRefreshToken rotatedEntity = new UserRefreshToken(
                rotatedToken,
                "teacher-123",
                "turing@renaissance.edu",
                "ROLE_TEACHER",
                "Android Pixel 8",
                LocalDateTime.now().plusDays(15)
        );
        rotatedEntity.setRotated(true);
        rotatedEntity.setRotatedAt(LocalDateTime.now().minusSeconds(5)); // Rotated 5 seconds ago
        rotatedEntity.setReplacedByToken(activeReplacementToken);

        UserRefreshToken activeEntity = new UserRefreshToken(
                activeReplacementToken,
                "teacher-123",
                "turing@renaissance.edu",
                "ROLE_TEACHER",
                "Android Pixel 8",
                LocalDateTime.now().plusDays(30)
        );

        when(refreshTokenRepository.findByToken(rotatedToken)).thenReturn(Optional.of(rotatedEntity));
        when(refreshTokenRepository.findByToken(activeReplacementToken)).thenReturn(Optional.of(activeEntity));
        when(teacherRepository.findById("teacher-123")).thenReturn(Optional.of(testTeacher));
        when(httpRequest.getSession(true)).thenReturn(httpSession);

        // Concurrent call sending the prior token within grace period
        RefreshResponse refreshResponse = authService.silentRenew(rotatedToken, "Android Pixel 8", httpRequest, httpResponse);

        assertNotNull(refreshResponse);
        assertEquals(activeReplacementToken, refreshResponse.getRefreshToken(), "Must return the active replacement token");
        assertEquals("Dr. Alan Turing", refreshResponse.getTeacher().getName());

        // Verify fresh authenticated SecurityContext was stored in session
        verify(httpSession, times(1)).setAttribute(eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Silent renewal rejects rotated token if grace window has expired")
    void testSilentRenewalRejectsRotatedTokenPastGracePeriod() {
        String staleToken = "stale-rotated-token-expired";
        UserRefreshToken staleEntity = new UserRefreshToken(
                staleToken,
                "teacher-123",
                "turing@renaissance.edu",
                "ROLE_TEACHER",
                "Android Pixel 8",
                LocalDateTime.now().plusDays(15)
        );
        staleEntity.setRotated(true);
        staleEntity.setRotatedAt(LocalDateTime.now().minusSeconds(120)); // Rotated 2 minutes ago (>60s)
        staleEntity.setReplacedByToken("some-other-token");

        when(refreshTokenRepository.findByToken(staleToken)).thenReturn(Optional.of(staleEntity));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
                authService.silentRenew(staleToken, "Device", httpRequest, httpResponse)
        );

        assertTrue(exception.getMessage().contains("expired or was revoked"));
        verify(refreshTokenRepository, times(1)).delete(staleEntity);
    }

    @Test
    @DisplayName("Silent renewal rejects expired or revoked refresh token")
    void testSilentRenewalRejectsExpiredToken() {
        String expiredToken = "expired-token-456";
        UserRefreshToken expiredEntity = new UserRefreshToken(
                expiredToken,
                "teacher-123",
                "turing@renaissance.edu",
                "ROLE_TEACHER",
                "Android Pixel 8",
                LocalDateTime.now().minusDays(1) // expired
        );

        when(refreshTokenRepository.findByToken(expiredToken)).thenReturn(Optional.of(expiredEntity));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
                authService.silentRenew(expiredToken, "Device", httpRequest, httpResponse)
        );

        assertTrue(exception.getMessage().contains("expired or was revoked"));
        verify(refreshTokenRepository, times(1)).delete(expiredEntity);
    }

    @Test
    @DisplayName("Silent renewal rejects inactive teacher account")
    void testSilentRenewalRejectsDisabledAccount() {
        String validToken = "valid-token-789";
        UserRefreshToken tokenEntity = new UserRefreshToken(
                validToken,
                "teacher-123",
                "turing@renaissance.edu",
                "ROLE_TEACHER",
                "Android Pixel 8",
                LocalDateTime.now().plusDays(20)
        );

        testTeacher.setActive(false); // Teacher deactivated

        when(refreshTokenRepository.findByToken(validToken)).thenReturn(Optional.of(tokenEntity));
        when(teacherRepository.findById("teacher-123")).thenReturn(Optional.of(testTeacher));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
                authService.silentRenew(validToken, "Device", httpRequest, httpResponse)
        );

        assertTrue(exception.getMessage().contains("inactive"));
        verify(refreshTokenRepository, times(1)).delete(tokenEntity);
    }

    @Test
    @DisplayName("Logout securely revokes refresh token from MongoDB and clears session")
    void testLogoutRevokesTokenAndClearsSession() {
        when(httpRequest.getSession(false)).thenReturn(httpSession);

        authService.logout(httpRequest, "token-to-revoke-999");

        verify(refreshTokenRepository, times(1)).deleteByToken("token-to-revoke-999");
        verify(httpSession, times(1)).invalidate();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
