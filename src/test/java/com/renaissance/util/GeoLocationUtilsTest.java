package com.renaissance.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoLocationUtilsTest {

    // Reference point: e.g. College classroom in Bengaluru, India
    private static final double TEACHER_LAT = 13.030500;
    private static final double TEACHER_LON = 77.565000;

    @Test
    @DisplayName("Should return 0 meters for identical coordinates")
    void testIdenticalCoordinates() {
        double distance = GeoLocationUtils.calculateDistanceMeters(TEACHER_LAT, TEACHER_LON, TEACHER_LAT, TEACHER_LON);
        assertEquals(0.0, distance, 0.001);
        assertTrue(GeoLocationUtils.isWithinRadius(TEACHER_LAT, TEACHER_LON, TEACHER_LAT, TEACHER_LON, 40.0));
    }

    @Test
    @DisplayName("Should correctly calculate distance ~10 meters away (inside 40m radius)")
    void testInsideRadius10Meters() {
        // ~10 meters north of teacher: 10 / 111195 ~ 0.00009 degrees lat
        double studentLat = TEACHER_LAT + 0.00009;
        double studentLon = TEACHER_LON;

        double distance = GeoLocationUtils.calculateDistanceMeters(TEACHER_LAT, TEACHER_LON, studentLat, studentLon);
        assertTrue(distance > 8.0 && distance < 12.0, "Expected distance around 10m, got: " + distance);
        assertTrue(GeoLocationUtils.isWithinRadius(TEACHER_LAT, TEACHER_LON, studentLat, studentLon, 40.0));
    }

    @Test
    @DisplayName("Should correctly calculate distance ~15 meters away (inside 40m radius)")
    void testInsideRadius15Meters() {
        // ~15 meters north: 15 / 111195 ~ 0.000135 degrees lat
        double studentLat = TEACHER_LAT + 0.000135;
        double studentLon = TEACHER_LON;

        double distance = GeoLocationUtils.calculateDistanceMeters(TEACHER_LAT, TEACHER_LON, studentLat, studentLon);
        assertTrue(distance > 13.0 && distance < 17.0, "Expected distance around 15m, got: " + distance);
        assertTrue(GeoLocationUtils.isWithinRadius(TEACHER_LAT, TEACHER_LON, studentLat, studentLon, 40.0));
    }

    @Test
    @DisplayName("Should correctly calculate distance ~30 meters away (inside 40m radius)")
    void testInsideRadius30Meters() {
        // ~30 meters north: 30 / 111195 ~ 0.00027 degrees lat
        double studentLat = TEACHER_LAT + 0.00027;
        double studentLon = TEACHER_LON;

        double distance = GeoLocationUtils.calculateDistanceMeters(TEACHER_LAT, TEACHER_LON, studentLat, studentLon);
        assertTrue(distance > 28.0 && distance < 32.0, "Expected distance around 30m, got: " + distance);
        assertTrue(GeoLocationUtils.isWithinRadius(TEACHER_LAT, TEACHER_LON, studentLat, studentLon, 40.0));
    }

    @Test
    @DisplayName("Should correctly calculate distance ~40 meters away (inside 40m radius boundary)")
    void testInsideRadius40Meters() {
        // ~39.8 meters north: 39.8 / 111195 ~ 0.000358 degrees lat
        double studentLat = TEACHER_LAT + 0.000358;
        double studentLon = TEACHER_LON;

        double distance = GeoLocationUtils.calculateDistanceMeters(TEACHER_LAT, TEACHER_LON, studentLat, studentLon);
        assertTrue(distance <= 40.0, "Expected distance <= 40.0m, got: " + distance);
        assertTrue(GeoLocationUtils.isWithinRadius(TEACHER_LAT, TEACHER_LON, studentLat, studentLon, 40.0));
    }

    @Test
    @DisplayName("Should strictly reject student outside 40m radius (e.g. 50 meters away)")
    void testOutsideRadius50Meters() {
        // ~50 meters north: ~ 0.00045 degrees lat
        double studentLat = TEACHER_LAT + 0.00045;
        double studentLon = TEACHER_LON;

        double distance = GeoLocationUtils.calculateDistanceMeters(TEACHER_LAT, TEACHER_LON, studentLat, studentLon);
        assertTrue(distance > 45.0 && distance < 55.0, "Expected distance around 50m, got: " + distance);
        assertFalse(GeoLocationUtils.isWithinRadius(TEACHER_LAT, TEACHER_LON, studentLat, studentLon, 40.0));
    }

    @Test
    @DisplayName("Should verify exact 40-meter boundary condition")
    void testExactBoundary() {
        double allowedRadius = 40.0;
        // Test edge cases: 39.99m is within, 40.00m is within, 40.01m is outside
        assertTrue(39.99 <= allowedRadius);
        assertTrue(40.00 <= allowedRadius);
        assertFalse(40.01 <= allowedRadius);
    }

    @Test
    @DisplayName("Should validate valid and invalid coordinates")
    void testCoordinateValidation() {
        assertTrue(GeoLocationUtils.isValidCoordinate(13.03, 77.56));
        assertTrue(GeoLocationUtils.isValidCoordinate(0.0, 0.0));
        assertTrue(GeoLocationUtils.isValidCoordinate(-90.0, 180.0));
        assertFalse(GeoLocationUtils.isValidCoordinate(null, 77.56));
        assertFalse(GeoLocationUtils.isValidCoordinate(91.0, 77.56));
        assertFalse(GeoLocationUtils.isValidCoordinate(13.0, 181.0));
        assertFalse(GeoLocationUtils.isValidCoordinate(Double.NaN, 77.56));
    }

    @Test
    @DisplayName("Should round distance to 2 decimal places")
    void testRounding() {
        assertEquals(12.35, GeoLocationUtils.roundDistance(12.3456));
        assertEquals(40.00, GeoLocationUtils.roundDistance(40.0001));
    }
}
