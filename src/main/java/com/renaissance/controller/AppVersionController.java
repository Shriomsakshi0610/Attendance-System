package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.AppVersionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/public/app")
public class AppVersionController {

    private static final Logger log = LoggerFactory.getLogger(AppVersionController.class);

    private static final int CURRENT_VERSION_CODE = 8;
    private static final String CURRENT_VERSION_NAME = "1.3.4";
    private static final int MIN_SUPPORTED_VERSION_CODE = 1;
    private static final String DEFAULT_RELEASE_NOTES =
            "Renaissance Teacher App v1.3.4:\n" +
            "• Permanent Fix for Automatic Session Logout: Teachers stay logged in without 1-2 hour unexpected logouts\n" +
            "• RFC 6819 Token Rotation Grace Window: Concurrent requests safely reuse recently rotated tokens\n" +
            "• Atomic Pre-Lock Token Resolution: Eliminates 401 race condition in concurrent dashboard coroutines\n" +
            "• Dynamic OkHttp In-Memory CookieJar Synchronization: Prevents stale session cookie loops\n" +
            "• Server-Side Refresh Token Revocation on Explicit Logout\n" +
            "• MongoDB Persistent Refresh Tokens: 30-day longevity surviving container restarts\n" +
            "• Subject-Specific Student Attendance History with Class & Subject filtering\n" +
            "• 40-meter GPS Geofencing & BLE Proximity Verification\n" +
            "• Direct in-app streaming APK download with live progress indicator";

    @Value("${app.apk.path:src/main/resources/static/downloads/renaissance-teacher.apk}")
    private String configuredApkPath;

    @GetMapping("/version")
    public ResponseEntity<AppVersionResponse> getAppVersion() {
        Resource apkResource = resolveApkResource();
        long size = 0L;
        String sha256 = "";

        if (apkResource != null && apkResource.exists()) {
            try {
                size = apkResource.contentLength();
                sha256 = calculateSha256(apkResource);
            } catch (Exception e) {
                log.warn("Failed to determine size/sha256 of APK: {}", e.getMessage());
            }
        }

        AppVersionResponse response = new AppVersionResponse(
                CURRENT_VERSION_CODE,
                CURRENT_VERSION_NAME,
                MIN_SUPPORTED_VERSION_CODE,
                "/api/public/app/download",
                DEFAULT_RELEASE_NOTES,
                sha256,
                size,
                LocalDateTime.of(2026, 11, 7, 13, 0)
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadApk() {
        Resource apkResource = resolveApkResource();
        if (apkResource == null || !apkResource.exists()) {
            return ResponseEntity.status(404).body(ApiResponse.error(
                    "Renaissance Teacher APK is currently being generated or not available. Please try again shortly."
            ));
        }

        try {
            long length = apkResource.contentLength();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"renaissance-teacher-v" + CURRENT_VERSION_NAME + ".apk\"")
                    .contentLength(length)
                    .body(apkResource);
        } catch (Exception e) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"renaissance-teacher-v" + CURRENT_VERSION_NAME + ".apk\"")
                    .body(apkResource);
        }
    }

    private Resource resolveApkResource() {
        // 1. Check configured relative path on filesystem
        if (configuredApkPath != null && !configuredApkPath.isBlank()) {
            Path path = Paths.get(configuredApkPath);
            if (Files.exists(path)) {
                return new FileSystemResource(path);
            }
        }

        // 2. Check static downloads in project directory
        Path fallback1 = Paths.get("src/main/resources/static/downloads/renaissance-teacher.apk");
        if (Files.exists(fallback1)) {
            return new FileSystemResource(fallback1);
        }

        // 3. Check android release output dir
        Path fallback2 = Paths.get("android/app/build/outputs/apk/release/app-release.apk");
        if (Files.exists(fallback2)) {
            return new FileSystemResource(fallback2);
        }

        // 4. Check android debug output dir
        Path fallback3 = Paths.get("android/app/build/outputs/apk/debug/app-debug.apk");
        if (Files.exists(fallback3)) {
            return new FileSystemResource(fallback3);
        }

        // 5. Fallback to classpath resource (bundled inside the running jar on Render)
        Resource cpResource = new ClassPathResource("static/downloads/renaissance-teacher.apk");
        if (cpResource.exists()) {
            return cpResource;
        }

        return null;
    }

    private String calculateSha256(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.warn("Failed to calculate SHA-256 for APK: {}", e.getMessage());
            return "";
        }
    }
}
