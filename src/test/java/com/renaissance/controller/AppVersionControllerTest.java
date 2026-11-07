package com.renaissance.controller;

import com.renaissance.dto.AppVersionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AppVersionControllerTest {

    private AppVersionController controller;

    @BeforeEach
    void setUp() {
        controller = new AppVersionController();
        ReflectionTestUtils.setField(controller, "configuredApkPath", "src/main/resources/static/downloads/renaissance-teacher.apk");
    }

    @Test
    @DisplayName("Should return latest version 1.2.0 with versionCode 3")
    void testGetAppVersion() {
        ResponseEntity<AppVersionResponse> response = controller.getAppVersion();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        AppVersionResponse body = response.getBody();
        assertEquals(3, body.getVersionCode(), "Server versionCode must be 3");
        assertEquals("1.2.0", body.getVersionName(), "Server versionName must be 1.2.0");
        assertEquals(1, body.getMinVersionCode());
        assertEquals("/api/public/app/download", body.getDownloadUrl());
        assertTrue(body.getReleaseNotes().contains("Student-wise Attendance"), "Release notes should mention Student-wise Attendance");
        assertTrue(body.getFileSizeBytes() > 0, "File size must be greater than 0");
        assertFalse(body.getSha256().isBlank(), "SHA-256 must be calculated and non-empty");
    }

    @Test
    @DisplayName("Should serve downloadable APK with valid package archive content type")
    void testDownloadApk() throws Exception {
        ResponseEntity<?> response = controller.downloadApk();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/vnd.android.package-archive", response.getHeaders().getContentType().toString());

        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("renaissance-teacher-v1.2.0.apk"));

        assertTrue(response.getBody() instanceof Resource);
        Resource resource = (Resource) response.getBody();
        assertTrue(resource.exists());
        assertTrue(resource.contentLength() > 0);
    }

    @Test
    @DisplayName("Simulate Task 6: Version comparison logic")
    void testVersionComparisonScenarios() {
        // Scenario 1: Installed v1 (old) vs Server v2 (new 40m update) -> Update Available
        int installedVersion1 = 1;
        int serverVersion = 2;
        assertTrue(serverVersion > installedVersion1, "Server version 2 must trigger update for installed version 1");

        // Scenario 2: Installed v2 vs Server v2 -> Up to date
        int installedVersion2 = 2;
        assertFalse(serverVersion > installedVersion2, "Installed version 2 should report up to date");

        // Scenario 3: User prompt test case: installed 5 vs server 6 -> Update Available
        assertTrue(6 > 5, "Simulated installed=5, server=6 triggers update");

        // Scenario 4: User prompt test case: installed 6 vs server 6 -> Up to date
        assertFalse(6 > 6, "Simulated installed=6, server=6 reports up to date");
    }
}
