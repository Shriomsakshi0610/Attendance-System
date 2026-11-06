package com.renaissance.util;

/**
 * Utility for GPS distance and geofence calculations using the Haversine formula.
 * All computations are performed on the server side to prevent client spoofing.
 */
public final class GeoLocationUtils {

    // Mean radius of Earth in meters
    public static final double EARTH_RADIUS_METERS = 6371000.0;
    public static final double DEFAULT_GEOFENCE_RADIUS_METERS = 15.0;

    private GeoLocationUtils() {
        // Private constructor for utility class
    }

    /**
     * Calculates the great-circle distance between two geographic coordinates
     * using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance between the two points in meters
     */
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        // Quick check for identical coordinates
        if (Double.compare(lat1, lat2) == 0 && Double.compare(lon1, lon2) == 0) {
            return 0.0;
        }

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double sinHalfDeltaLat = Math.sin(deltaLatRad / 2.0);
        double sinHalfDeltaLon = Math.sin(deltaLonRad / 2.0);

        double a = (sinHalfDeltaLat * sinHalfDeltaLat)
                + (Math.cos(lat1Rad) * Math.cos(lat2Rad) * sinHalfDeltaLon * sinHalfDeltaLon);

        // Clamp 'a' between 0 and 1 to prevent precision issues with Math.asin / Math.atan2
        a = Math.min(1.0, Math.max(0.0, a));

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Verifies whether student coordinates fall within the allowed radius of teacher coordinates.
     *
     * @param teacherLat Teacher's session latitude
     * @param teacherLon Teacher's session longitude
     * @param studentLat Student's current latitude
     * @param studentLon Student's current longitude
     * @param allowedRadiusMeters Allowed geofence boundary (e.g. 15.0 meters)
     * @return true if within allowed radius, false otherwise
     */
    public static boolean isWithinRadius(double teacherLat, double teacherLon,
                                        double studentLat, double studentLon,
                                        double allowedRadiusMeters) {
        double distance = calculateDistanceMeters(teacherLat, teacherLon, studentLat, studentLon);
        return distance <= allowedRadiusMeters;
    }

    /**
     * Rounds a distance value to 2 decimal places for user-facing responses.
     */
    public static double roundDistance(double distanceMeters) {
        return Math.round(distanceMeters * 100.0) / 100.0;
    }

    /**
     * Validates that coordinate values fall within standard latitude [-90, 90]
     * and longitude [-180, 180] ranges.
     */
    public static boolean isValidCoordinate(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return false;
        }
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            return false;
        }
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }
}
