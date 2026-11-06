package com.renaissance.exception;

public class AttendanceValidationException extends RuntimeException {

    private final String errorCode;
    private final Double calculatedDistance;

    public AttendanceValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.calculatedDistance = null;
    }

    public AttendanceValidationException(String message, String errorCode, Double calculatedDistance) {
        super(message);
        this.errorCode = errorCode;
        this.calculatedDistance = calculatedDistance;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Double getCalculatedDistance() {
        return calculatedDistance;
    }
}
