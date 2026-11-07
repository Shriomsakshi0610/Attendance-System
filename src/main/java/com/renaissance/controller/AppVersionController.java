package com.renaissance.controller;

import com.renaissance.dto.ApiResponse;
import com.renaissance.dto.AppVersionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
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

    private static final int CURRENT_VERSION_CODE = 1;
    private static final String CURRENT_VERSION_NAME = "1.0.0";
    private static final int MIN_SUPPORTED_VERSION_CODE = 1;
    private static final String DEFAULT_RELEASE_NOTES =
            "Renaissance Teacher App v1.0.0:\n" +
            "• Secure Teacher Authentication with biometric/keystore storage\n" +
            "• High-precision 40-meter GPS geofencing verification\n" +
            "• Bluetooth Low Energy (BLE) proximity beacon advertising\n" +
            "• Real-time attendance monitoring with live counters\n" +
            "• Timetable, subjects, and class section management\n" +
            "• Built-in background update checker and installer";

    @Value("${app.apk.path:src/main/resources/static/downloads/renaissance-teacher.apk}")
    private String configuredApkPath;

    @GetMapping("/version")
    public ResponseEntity<AppVersionResponse> getAppVersion() {
        File apkFile = resolveApkFile();
        long size = 0L;
        String sha256 = "";

        if (apkFile != null && apkFile.exists()) {
            size = apkFile.length();
            sha256 = calculateSha256(apkFile);
        }

        AppVersionResponse response = new AppVersionResponse(
                CURRENT_VERSION_CODE,
                CURRENT_VERSION_NAME,
                MIN_SUPPORTED_VERSION_CODE,
                "/api/public/app/download",
                DEFAULT_RELEASE_NOTES,
                sha256,
                size,
                LocalDateTime.of(2026, 11, 6, 12, 0)
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadApk() {
        File apkFile = resolveApkFile();
        if (apkFile == null || !apkFile.exists()) {
            return ResponseEntity.status(404).body(ApiResponse.error(
                    "Renaissance Teacher APK is currently being generated or not available. Please try again shortly."
            ));
        }

        Resource resource = new FileSystemResource(apkFile);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"renaissance-teacher-v" + CURRENT_VERSION_NAME + ".apk\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(apkFile.length()))
                .body(resource);
    }

    private File resolveApkFile() {
        // 1. Try configured relative path
        Path path = Paths.get(configuredApkPath);
        if (Files.exists(path)) {
            return path.toFile();
        }

        // 2. Try static downloads in current working dir
        Path fallback1 = Paths.get("src/main/resources/static/downloads/renaissance-teacher.apk");
        if (Files.exists(fallback1)) {
            return fallback1.toFile();
        }

        // 3. Try android release output dir
        Path fallback2 = Paths.get("android/app/build/outputs/apk/release/app-release.apk");
        if (Files.exists(fallback2)) {
            return fallback2.toFile();
        }

        // 4. Try android debug output dir
        Path fallback3 = Paths.get("android/app/build/outputs/apk/debug/app-debug.apk");
        if (Files.exists(fallback3)) {
            return fallback3.toFile();
        }

        return null;
    }

    private String calculateSha256(File file) {
        try (InputStream is = Files.newInputStream(file.toPath())) {
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
