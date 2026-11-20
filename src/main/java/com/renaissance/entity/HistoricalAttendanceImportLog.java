package com.renaissance.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "historical_attendance_import_logs")
public class HistoricalAttendanceImportLog {

    @Id
    private String id;

    @Indexed
    private String adminId;

    @Indexed
    private String adminEmail;

    private String fileName;

    @Indexed
    private LocalDateTime importTimestamp = LocalDateTime.now();

    private int totalRows;
    private int successfulRecords;
    private int failedRecords;
    private int duplicateRecords;

    private List<String> errorDetails = new ArrayList<>();

    private String status = "COMPLETED"; // COMPLETED, FAILED

    public HistoricalAttendanceImportLog() {
    }

    public HistoricalAttendanceImportLog(String adminId, String adminEmail, String fileName,
                                        int totalRows, int successfulRecords, int failedRecords,
                                        int duplicateRecords, List<String> errorDetails, String status) {
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.fileName = fileName;
        this.importTimestamp = LocalDateTime.now();
        this.totalRows = totalRows;
        this.successfulRecords = successfulRecords;
        this.failedRecords = failedRecords;
        this.duplicateRecords = duplicateRecords;
        this.errorDetails = errorDetails != null ? errorDetails : new ArrayList<>();
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getImportTimestamp() {
        return importTimestamp;
    }

    public void setImportTimestamp(LocalDateTime importTimestamp) {
        this.importTimestamp = importTimestamp;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessfulRecords() {
        return successfulRecords;
    }

    public void setSuccessfulRecords(int successfulRecords) {
        this.successfulRecords = successfulRecords;
    }

    public int getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(int failedRecords) {
        this.failedRecords = failedRecords;
    }

    public int getDuplicateRecords() {
        return duplicateRecords;
    }

    public void setDuplicateRecords(int duplicateRecords) {
        this.duplicateRecords = duplicateRecords;
    }

    public List<String> getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(List<String> errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
