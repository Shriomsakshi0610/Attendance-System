package com.renaissance.dto;

import java.time.LocalDateTime;

public class AppVersionResponse {

    private int versionCode;
    private String versionName;
    private int minVersionCode;
    private String downloadUrl;
    private String releaseNotes;
    private String sha256;
    private long fileSizeBytes;
    private LocalDateTime publishedAt;

    public AppVersionResponse() {
    }

    public AppVersionResponse(int versionCode, String versionName, int minVersionCode,
                              String downloadUrl, String releaseNotes, String sha256,
                              long fileSizeBytes, LocalDateTime publishedAt) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.minVersionCode = minVersionCode;
        this.downloadUrl = downloadUrl;
        this.releaseNotes = releaseNotes;
        this.sha256 = sha256;
        this.fileSizeBytes = fileSizeBytes;
        this.publishedAt = publishedAt;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getMinVersionCode() {
        return minVersionCode;
    }

    public void setMinVersionCode(int minVersionCode) {
        this.minVersionCode = minVersionCode;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(String releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
