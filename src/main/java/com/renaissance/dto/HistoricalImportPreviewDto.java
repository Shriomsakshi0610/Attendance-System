package com.renaissance.dto;

import java.util.ArrayList;
import java.util.List;

public class HistoricalImportPreviewDto {

    private String importToken;
    private String fileName;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private int duplicateRows;
    private List<HistoricalImportRowDto> previewRows = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public HistoricalImportPreviewDto() {
    }

    public String getImportToken() {
        return importToken;
    }

    public void setImportToken(String importToken) {
        this.importToken = importToken;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public void setInvalidRows(int invalidRows) {
        this.invalidRows = invalidRows;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public void setDuplicateRows(int duplicateRows) {
        this.duplicateRows = duplicateRows;
    }

    public List<HistoricalImportRowDto> getPreviewRows() {
        return previewRows;
    }

    public void setPreviewRows(List<HistoricalImportRowDto> previewRows) {
        this.previewRows = previewRows;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
