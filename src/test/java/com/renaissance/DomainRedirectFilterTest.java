package com.renaissance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class DomainRedirectFilterTest {

    private DomainRedirectFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DomainRedirectFilter();
        filter.setRedirectEnabled(true);
        filter.setTargetDomain("relianceattendance.site");
        filter.setTargetScheme("https");
        filter.setOldDomainsConfig("attendance-system-ivna.onrender.com,attendance-system-gfr7.onrender.com");
    }

    @Test
    @DisplayName("Verify cleanHost utility strips schemes, ports, and trailing slashes")
    void testCleanHost() {
        assertEquals("relianceattendance.site", DomainRedirectFilter.cleanHost("https://relianceattendance.site"));
        assertEquals("relianceattendance.site", DomainRedirectFilter.cleanHost("http://relianceattendance.site:8080/"));
        assertEquals("attendance-system-ivna.onrender.com", DomainRedirectFilter.cleanHost("attendance-system-ivna.onrender.com:443"));
        assertEquals("attendance-system-ivna.onrender.com", DomainRedirectFilter.cleanHost("  attendance-system-ivna.onrender.com  "));
        assertEquals("", DomainRedirectFilter.cleanHost(null));
    }

    @Test
    @DisplayName("Verify isOldDomain correctly detects old domains and prevents loops on target domain")
    void testIsOldDomain() {
        // Old domains
        assertTrue(filter.isOldDomain("attendance-system-ivna.onrender.com"));
        assertTrue(filter.isOldDomain("attendance-system-gfr7.onrender.com"));
        assertTrue(filter.isOldDomain("random-test-service.onrender.com"));
        assertTrue(filter.isOldDomain("attendance-system-ivna.onrender.com:443"));

        // Target domain and www subdomain (MUST NEVER REDIRECT - NO LOOPS)
        assertFalse(filter.isOldDomain("relianceattendance.site"));
        assertFalse(filter.isOldDomain("www.relianceattendance.site"));
        assertFalse(filter.isOldDomain("https://relianceattendance.site"));

        // Local / test environments
        assertFalse(filter.isOldDomain("localhost"));
        assertFalse(filter.isOldDomain("127.0.0.1"));
        assertFalse(filter.isOldDomain("::1"));
        assertFalse(filter.isOldDomain("server.local"));
        assertFalse(filter.isOldDomain("internal.service.internal"));

        // Null / empty
        assertFalse(filter.isOldDomain(null));
        assertFalse(filter.isOldDomain(""));
    }

    @Test
    @DisplayName("GET / on old Render domain returns 301 Permanent Redirect to new custom domain root")
    void testRedirectHomePageOnOldDomain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
        assertEquals("https://relianceattendance.site/", response.getHeader("Location"));
        assertNotNull(response.getHeader("Cache-Control"));
    }

    @Test
    @DisplayName("GET page on old domain with query string preserves full path and query parameters")
    void testRedirectWebPageWithQueryParams() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/student-attendance.html");
        request.setServerName("attendance-system-ivna.onrender.com");
        request.setQueryString("sessionId=65fa1234&source=qr");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
        assertEquals("https://relianceattendance.site/student-attendance.html?sessionId=65fa1234&source=qr",
                response.getHeader("Location"));
    }

    @Test
    @DisplayName("X-Forwarded-Host header with port from reverse proxy is properly detected and redirected")
    void testRedirectWithXForwardedHost() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/teacher-login.html");
        request.addHeader("X-Forwarded-Host", "attendance-system-ivna.onrender.com:443");
        request.setServerName("10.0.4.12"); // internal proxy IP
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
        assertEquals("https://relianceattendance.site/teacher-login.html", response.getHeader("Location"));
    }

    @Test
    @DisplayName("Any *.onrender.com subdomain is identified as old domain and redirected")
    void testRedirectGenericRenderSubdomain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-login.html");
        request.setServerName("renaissance-backup-9988.onrender.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
        assertEquals("https://relianceattendance.site/admin-login.html", response.getHeader("Location"));
    }

    @Test
    @DisplayName("Requests to new custom domain relianceattendance.site are NOT redirected (No loop)")
    void testNoRedirectOnTargetDomain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("relianceattendance.site");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestFilterChain filterChain = new TestFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Location"));
        assertTrue(filterChain.isInvoked(), "FilterChain must be invoked for target domain requests");
    }

    @Test
    @DisplayName("Requests to www.relianceattendance.site are NOT redirected by this filter")
    void testNoRedirectOnWwwTargetDomain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
        request.setServerName("www.relianceattendance.site");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestFilterChain filterChain = new TestFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Location"));
        assertTrue(filterChain.isInvoked());
    }

    @Test
    @DisplayName("Requests on localhost or internal IP are NOT redirected (local testing intact)")
    void testNoRedirectOnLocalhost() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestFilterChain filterChain = new TestFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Location"));
        assertTrue(filterChain.isInvoked());
    }

    @Test
    @DisplayName("Health check endpoints /health and /api/public/health are NOT redirected (Render monitoring preserved)")
    void testNoRedirectOnHealthCheck() throws ServletException, IOException {
        // /health
        MockHttpServletRequest healthReq = new MockHttpServletRequest("GET", "/health");
        healthReq.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse healthRes = new MockHttpServletResponse();
        TestFilterChain healthChain = new TestFilterChain();

        filter.doFilter(healthReq, healthRes, healthChain);
        assertNull(healthRes.getHeader("Location"));
        assertTrue(healthChain.isInvoked(), "/health must not be redirected");

        // /api/public/health
        MockHttpServletRequest pubHealthReq = new MockHttpServletRequest("GET", "/api/public/health");
        pubHealthReq.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse pubHealthRes = new MockHttpServletResponse();
        TestFilterChain pubHealthChain = new TestFilterChain();

        filter.doFilter(pubHealthReq, pubHealthRes, pubHealthChain);
        assertNull(pubHealthRes.getHeader("Location"));
        assertTrue(pubHealthChain.isInvoked(), "/api/public/health must not be redirected");

        // /actuator/health
        MockHttpServletRequest actReq = new MockHttpServletRequest("GET", "/actuator/health");
        actReq.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse actRes = new MockHttpServletResponse();
        TestFilterChain actChain = new TestFilterChain();

        filter.doFilter(actReq, actRes, actChain);
        assertNull(actRes.getHeader("Location"));
        assertTrue(actChain.isInvoked(), "/actuator/health must not be redirected");
    }

    @Test
    @DisplayName("API endpoints /api/** are NOT redirected (Preserves mobile apps and AJAX)")
    void testNoRedirectOnApiEndpoint() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/status");
        request.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestFilterChain filterChain = new TestFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(response.getHeader("Location"));
        assertTrue(filterChain.isInvoked(), "/api/auth/status must pass through to controller");
    }

    @Test
    @DisplayName("POST /api/attendance/mark is NOT redirected (Preserves attendance submissions and payloads)")
    void testNoRedirectOnPostRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/attendance/mark");
        request.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestFilterChain filterChain = new TestFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(response.getHeader("Location"));
        assertTrue(filterChain.isInvoked(), "POST requests must never be redirected");
    }

    @Test
    @DisplayName("OPTIONS preflight requests are NOT redirected (Preserves CORS)")
    void testNoRedirectOnOptionsRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/attendance/mark");
        request.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestFilterChain filterChain = new TestFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(response.getHeader("Location"));
        assertTrue(filterChain.isInvoked(), "OPTIONS requests must pass through");
    }

    @Test
    @DisplayName("WebSocket endpoints /ws/** are NOT redirected")
    void testNoRedirectOnWebSocket() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/attendance");
        request.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestFilterChain filterChain = new TestFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(response.getHeader("Location"));
        assertTrue(filterChain.isInvoked(), "WebSocket handshakes must not be redirected");
    }

    @Test
    @DisplayName("SEO files /sitemap.xml and /robots.txt on old domain ARE redirected to new domain")
    void testRedirectSeoFiles() throws ServletException, IOException {
        MockHttpServletRequest sitemapReq = new MockHttpServletRequest("GET", "/sitemap.xml");
        sitemapReq.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse sitemapRes = new MockHttpServletResponse();
        TestFilterChain sitemapChain = new TestFilterChain();

        filter.doFilter(sitemapReq, sitemapRes, sitemapChain);
        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, sitemapRes.getStatus());
        assertEquals("https://relianceattendance.site/sitemap.xml", sitemapRes.getHeader("Location"));

        MockHttpServletRequest robotsReq = new MockHttpServletRequest("GET", "/robots.txt");
        robotsReq.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse robotsRes = new MockHttpServletResponse();
        TestFilterChain robotsChain = new TestFilterChain();

        filter.doFilter(robotsReq, robotsRes, robotsChain);
        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, robotsRes.getStatus());
        assertEquals("https://relianceattendance.site/robots.txt", robotsRes.getHeader("Location"));
    }

    @Test
    @DisplayName("When redirectEnabled is false, no redirect occurs")
    void testFilterDisabled() throws ServletException, IOException {
        filter.setRedirectEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("attendance-system-ivna.onrender.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestFilterChain filterChain = new TestFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(response.getHeader("Location"));
        assertTrue(filterChain.isInvoked());
    }

    /**
     * Helper filter chain to check invocation status.
     */
    private static class TestFilterChain implements FilterChain {
        private boolean invoked = false;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            this.invoked = true;
        }

        public boolean isInvoked() {
            return invoked;
        }
    }
}
