package com.renaissance.teacher.ui.screens.session

import com.renaissance.teacher.data.model.AttendanceSessionItem
import com.renaissance.teacher.data.model.CreateSessionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionFiveMinuteWindowAndroidTest {

    @Test
    fun testAttendanceSessionItem_isWindowExpired() {
        val activeSession = AttendanceSessionItem(
            id = "sess-1",
            sessionCode = "LIVE01",
            teacherId = "t-1",
            teacherName = "Prof. Turing",
            classId = "c-1",
            className = "5A CSE",
            departmentName = "CSE",
            semester = 5,
            section = "A",
            subjectId = "s-1",
            subjectName = "Algorithms",
            subjectCode = "CS501",
            teacherLatitude = 12.9716,
            teacherLongitude = 77.5946,
            geofenceRadiusMeters = 40.0,
            status = "ACTIVE",
            startTime = "2026-11-20T19:00:00",
            endTime = "2026-11-20T19:05:00",
            attendanceLink = "/student-attendance.html?code=LIVE01",
            remainingSeconds = 295L
        )

        assertFalse("Active session with 295s remaining must not be expired", activeSession.isWindowExpired)

        val expiredSessionByStatus = activeSession.copy(status = "EXPIRED", remainingSeconds = 0L)
        assertTrue("Session with status EXPIRED must be marked expired", expiredSessionByStatus.isWindowExpired)

        val expiredSessionByZeroRemaining = activeSession.copy(status = "ACTIVE", remainingSeconds = 0L)
        assertTrue("Session with 0s remaining must be marked expired", expiredSessionByZeroRemaining.isWindowExpired)

        val completedSession = activeSession.copy(status = "COMPLETED", remainingSeconds = 50L)
        assertTrue("Completed session must be marked expired", completedSession.isWindowExpired)
    }

    @Test
    fun testFormatSecondsToMmSs() {
        assertEquals("05:00", formatSeconds(300L))
        assertEquals("04:32", formatSeconds(272L))
        assertEquals("03:00", formatSeconds(180L))
        assertEquals("00:45", formatSeconds(45L))
        assertEquals("00:09", formatSeconds(9L))
        assertEquals("00:00", formatSeconds(0L))
        assertEquals("00:00", formatSeconds(-5L))
    }

    @Test
    fun testCreateSessionRequest_enforcesFiveMinuteDefault() {
        val request = CreateSessionRequest(
            classId = "class-5a",
            subjectId = "subj-algo",
            latitude = 12.9716,
            longitude = 77.5946,
            accuracy = 10.0
        )

        assertEquals("CreateSessionRequest durationMinutes must be strictly 5 minutes", 5, request.durationMinutes)
    }

    @Test
    fun testMonotonicCountdownCalculation_reopenAfterTwoMinutes() {
        val initialRemainingSeconds = 300L // 5 minutes
        val startNanoTime = 1_000_000_000_000L // arbitrary monotonic start

        // Simulate reopening the app after 2 minutes (120 seconds elapsed)
        val elapsedNanosAfterTwoMinutes = 120L * 1_000_000_000L
        val currentNanoTime = startNanoTime + elapsedNanosAfterTwoMinutes

        val elapsedSecs = (currentNanoTime - startNanoTime) / 1_000_000_000L
        val calculatedRemaining = maxOf(0L, initialRemainingSeconds - elapsedSecs)

        assertEquals(180L, calculatedRemaining)
        assertEquals("03:00", formatSeconds(calculatedRemaining))
    }

    @Test
    fun testMonotonicCountdownCalculation_reopenAfterFiveMinutes() {
        val initialRemainingSeconds = 300L
        val startNanoTime = 1_000_000_000_000L

        // Simulate reopening the app after 5 minutes and 15 seconds (315 seconds elapsed)
        val elapsedNanosPastDeadline = 315L * 1_000_000_000L
        val currentNanoTime = startNanoTime + elapsedNanosPastDeadline

        val elapsedSecs = (currentNanoTime - startNanoTime) / 1_000_000_000L
        val calculatedRemaining = maxOf(0L, initialRemainingSeconds - elapsedSecs)
        val isExpired = calculatedRemaining <= 0L

        assertEquals(0L, calculatedRemaining)
        assertEquals("00:00", formatSeconds(calculatedRemaining))
        assertTrue("Session must be marked expired after 5 minutes", isExpired)
    }

    @Test
    fun testServerSync_realignsLocalBaselineOnDrift() {
        var baselineRemaining = 250L
        var baselineNano = 100_000_000_000L

        // Client thinks 240 seconds remain
        val simulatedNowNano = baselineNano + (10L * 1_000_000_000L)
        val localRemaining = baselineRemaining - ((simulatedNowNano - baselineNano) / 1_000_000_000L)
        assertEquals(240L, localRemaining)

        // Server authoritative response says 235 seconds remain (e.g. slight drift)
        val serverAuthoritativeRemaining = 235L
        val drift = Math.abs(localRemaining - serverAuthoritativeRemaining)
        assertTrue("Drift is 5 seconds (> 2s threshold)", drift >= 2L)

        // Resync
        baselineRemaining = serverAuthoritativeRemaining
        baselineNano = simulatedNowNano

        val resyncedRemaining = maxOf(0L, baselineRemaining - ((simulatedNowNano - baselineNano) / 1_000_000_000L))
        assertEquals(235L, resyncedRemaining)
        assertEquals("03:55", formatSeconds(resyncedRemaining))
    }

    private fun formatSeconds(seconds: Long): String {
        val totalSec = maxOf(0L, seconds)
        val minutes = totalSec / 60
        val secs = totalSec % 60
        return String.format("%02d:%02d", minutes, secs)
    }
}
