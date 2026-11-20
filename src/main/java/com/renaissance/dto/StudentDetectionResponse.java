package com.renaissance.dto;

import java.util.List;

public class StudentDetectionResponse {

    private boolean success;
    private String code; // EXACT_MATCH, MULTIPLE_MATCHES, NOT_FOUND, INVALID_INPUT
    private String usn;
    private String studentName;
    private String departmentName;
    private String departmentCode;
    private Integer semester;
    private String section;
    private String message;
    private List<String> candidateUsns;

    public StudentDetectionResponse() {
    }

    public static StudentDetectionResponse exactMatch(String usn, String studentName, String departmentName,
                                                     String departmentCode, Integer semester, String section) {
        StudentDetectionResponse res = new StudentDetectionResponse();
        res.setSuccess(true);
        res.setCode("EXACT_MATCH");
        res.setUsn(usn);
        res.setStudentName(studentName);
        res.setDepartmentName(departmentName);
        res.setDepartmentCode(departmentCode);
        res.setSemester(semester);
        res.setSection(section);
        res.setMessage("Student verified successfully: " + studentName + " (" + usn + ")");
        return res;
    }

    public static StudentDetectionResponse multipleMatches(List<String> candidates, String message) {
        StudentDetectionResponse res = new StudentDetectionResponse();
        res.setSuccess(false);
        res.setCode("MULTIPLE_MATCHES");
        res.setCandidateUsns(candidates);
        res.setMessage(message != null ? message : "Multiple students match these digits. Please enter your complete USN for confirmation.");
        return res;
    }

    public static StudentDetectionResponse notFound(String message) {
        StudentDetectionResponse res = new StudentDetectionResponse();
        res.setSuccess(false);
        res.setCode("NOT_FOUND");
        res.setMessage(message != null ? message : "No student found in this class with the provided digits.");
        return res;
    }

    public static StudentDetectionResponse invalidInput(String message) {
        StudentDetectionResponse res = new StudentDetectionResponse();
        res.setSuccess(false);
        res.setCode("INVALID_INPUT");
        res.setMessage(message != null ? message : "Please enter exactly three numeric digits.");
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getCandidateUsns() {
        return candidateUsns;
    }

    public void setCandidateUsns(List<String> candidateUsns) {
        this.candidateUsns = candidateUsns;
    }
}
