package com.renaissance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renaissance.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MongoTestConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Verify SEO files /sitemap.xml and /robots.txt are publicly accessible without authentication")
    void testSeoFilesPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verify static assets and public login pages are accessible without authentication")
    void testPublicPagesAccessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/teacher-login.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin-login.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/student-attendance.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verify /api/auth/status is publicly accessible without authentication")
    void testAuthStatusPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Verify Faculty Login succeeds, establishes session, and grants access to faculty resources")
    void testFacultyLoginAndAccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest("teacher@renaissance.edu", "Teacher@1234");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teacher.email").value("teacher@renaissance.edu"))
                .andExpect(jsonPath("$.data.teacher.role").value("ROLE_TEACHER"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session, "Login must create an authenticated HTTP session");

        // Faculty can access Teacher APIs
        mockMvc.perform(get("/api/teacher/departments").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Faculty is forbidden from Admin APIs
        mockMvc.perform(get("/api/admin/stats").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Verify Admin Login succeeds, establishes session, and grants access to admin resources")
    void testAdminLoginAndAccess() throws Exception {
        LoginRequest adminLogin = new LoginRequest("test-teacher@renaissance.edu", "Password@123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teacher.email").value("test-teacher@renaissance.edu"))
                .andExpect(jsonPath("$.data.teacher.role").value("ROLE_ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session, "Admin login must create an authenticated HTTP session");

        // Admin can access Admin APIs
        mockMvc.perform(get("/api/admin/stats").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Admin can access Teacher APIs
        mockMvc.perform(get("/api/teacher/departments").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verify invalid login credentials return 401 from Auth service, not security filter chain")
    void testInvalidLoginCredentials() throws Exception {
        LoginRequest invalidLogin = new LoginRequest("teacher@renaissance.edu", "WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Verify unauthenticated access to protected APIs returns 401 Unauthorized")
    void testProtectedApisRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/teacher/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Verify unauthenticated access to dashboard HTML pages redirects to login")
    void testProtectedPagesRedirectUnauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher-login.html?error=unauthorized"));

        mockMvc.perform(get("/admin-dashboard.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin-login.html?error=unauthorized"));
    }

    @Test
    @DisplayName("Verify logout invalidates the session")
    void testLogoutInvalidatesSession() throws Exception {
        LoginRequest loginRequest = new LoginRequest("teacher@renaissance.edu", "Teacher@1234");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Perform logout
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Ensure session is invalidated or cannot access protected endpoint
        mockMvc.perform(get("/api/teacher/departments").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Verify old Render domain GET / returns 301 Permanent Redirect to https://relianceattendance.site/")
    void testOldDomainRedirectsToCanonicalDomain() throws Exception {
        mockMvc.perform(get("/")
                        .header("X-Forwarded-Host", "attendance-system-ivna.onrender.com")
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://relianceattendance.site/"));
    }

    @Test
    @DisplayName("Verify old Render domain GET page with query parameters preserves path and query string")
    void testOldDomainPreservesPathAndQueryParams() throws Exception {
        mockMvc.perform(get("/student-attendance.html")
                        .param("sessionId", "session-999")
                        .param("source", "qr")
                        .header("X-Forwarded-Host", "attendance-system-ivna.onrender.com")
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://relianceattendance.site/student-attendance.html?sessionId=session-999&source=qr"));
    }

    @Test
    @DisplayName("Verify protected page on old domain redirects to new domain before Spring Security intercepts")
    void testOldDomainProtectedPageRedirectsBeforeSecurity() throws Exception {
        mockMvc.perform(get("/dashboard.html")
                        .header("X-Forwarded-Host", "attendance-system-ivna.onrender.com")
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://relianceattendance.site/dashboard.html"));
    }

    @Test
    @DisplayName("Verify health check on old domain is NOT redirected (Render monitoring preserved)")
    void testHealthCheckNotRedirectedOnOldDomain() throws Exception {
        mockMvc.perform(get("/health")
                        .header("X-Forwarded-Host", "attendance-system-ivna.onrender.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verify new custom domain relianceattendance.site does NOT redirect (No loop)")
    void testNewCustomDomainDoesNotRedirect() throws Exception {
        mockMvc.perform(get("/")
                        .header("X-Forwarded-Host", "relianceattendance.site")
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().isOk());
    }
}
