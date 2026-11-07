package com.renaissance.service;

import com.renaissance.exception.AttendanceValidationException;
import com.renaissance.util.GeoLocationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocationVerificationService {

    @Value("${renaissance.geofence.radius-meters:40.0}")
    private double defaultRadiusMeters;

    @Value("${renaissance.geofence.max-allowed-gps-accuracy:100.0}")
    private double maxAllowedAccuracyMeters;

    public static class VerificationResult {
        private final double distanceMeters;
        private final boolean withinRadius;
        private final double allowedRadiusMeters;
        private final double studentAccuracy;

        public VerificationResult(double distanceMeters, boolean withinRadius, double allowedRadiusMeters, double studentAccuracy) {
            this.distanceMeters = distanceMeters;
            this.withinRadius = withinRadius;
            this.allowedRadiusMeters = allowedRadiusMeters;
            this.studentAccuracy = studentAccuracy;
        }

        public double getDistanceMeters() {
            return distanceMeters;
        }

        public boolean isWithinRadius() {
            return withinRadius;
        }

        public double getAllowedRadiusMeters() {
            return allowedRadiusMeters;
        }

        public double getStudentAccuracy() {
            return studentAccuracy;
        }

        public String getFormattedDistance() {
            return String.format("%.1f meters", distanceMeters);
        }
    }

    /**
     * Independently computes distance between teacher session coordinates and student coordinates,
     * validates GPS accuracy, and checks whether the student falls strictly within the 40-meter geofence.
     */
    public VerificationResult verifyLocation(Double teacherLat, Double teacherLon,
                                             Double studentLat, Double studentLon,
                                             Double studentAccuracy, Double sessionRadiusMeters) {
        // Validate coordinates
        if (!GeoLocationUtils.isValidCoordinate(teacherLat, teacherLon)) {
            throw new AttendanceValidationException("Teacher session location is invalid.", "INVALID_TEACHER_LOCATION");
        }
        if (!GeoLocationUtils.isValidCoordinate(studentLat, studentLon)) {
            throw new AttendanceValidationException("Unable to verify your location. Please enable GPS permissions and try again.", "INVALID_STUDENT_LOCATION");
        }

        // Validate reported GPS accuracy
        double accuracy = studentAccuracy != null ? studentAccuracy : 0.0;
        if (accuracy > maxAllowedAccuracyMeters) {
            throw new AttendanceValidationException(
                    "GPS accuracy is too low (" + Math.round(accuracy) + "m). Please move to an open area, turn on High Accuracy mode, and retry.",
                    "LOW_GPS_ACCURACY"
            );
        }

        double radius = sessionRadiusMeters != null ? sessionRadiusMeters : defaultRadiusMeters;
        double distance = GeoLocationUtils.calculateDistanceMeters(teacherLat, teacherLon, studentLat, studentLon);
        double roundedDistance = GeoLocationUtils.roundDistance(distance);

        boolean isWithin = distance <= radius;

        if (!isWithin) {
            throw new AttendanceValidationException(
                    "You are outside the " + Math.round(radius) + "-meter attendance radius.",
                    "OUTSIDE_RADIUS",
                    roundedDistance
            );
        }

        return new VerificationResult(roundedDistance, true, radius, accuracy);
    }

    public double getDefaultRadiusMeters() {
        return defaultRadiusMeters;
    }
}
