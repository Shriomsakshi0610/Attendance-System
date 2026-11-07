package com.renaissance.teacher.data.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeofenceCalculator {

    const val EARTH_RADIUS_METERS = 6371000.0
    const val GEOFENCE_RADIUS_METERS = 40.0 // Exactly 40 meters strict college classroom limit
    const val ACCURACY_WARNING_THRESHOLD_METERS = 25.0 // Warn if GPS reading uncertainty is too large

    data class GeofenceEvaluation(
        val distanceMeters: Double,
        val isInside: Boolean,
        val formattedDistance: String,
        val accuracyAcceptable: Boolean
    )

    /**
     * Haversine formula to compute geodesic distance between two latitude/longitude points in meters.
     */
    fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)

        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Evaluates whether a student coordinate is strictly inside the teacher's 40m radius geofence.
     */
    fun evaluate(
        teacherLat: Double,
        teacherLon: Double,
        studentLat: Double,
        studentLon: Double,
        studentAccuracy: Double? = null,
        allowedRadius: Double = GEOFENCE_RADIUS_METERS
    ): GeofenceEvaluation {
        val rawDistance = calculateHaversineDistance(teacherLat, teacherLon, studentLat, studentLon)
        val isInside = rawDistance <= allowedRadius
        val formatted = if (rawDistance < 1.0) {
            String.format("%.1f m", rawDistance)
        } else {
            String.format("%.1f m", rawDistance)
        }
        val accuracyOk = studentAccuracy == null || studentAccuracy <= ACCURACY_WARNING_THRESHOLD_METERS

        return GeofenceEvaluation(
            distanceMeters = rawDistance,
            isInside = isInside,
            formattedDistance = formatted,
            accuracyAcceptable = accuracyOk
        )
    }
}
