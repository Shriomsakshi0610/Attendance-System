package com.renaissance.teacher.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

data class LoginRequest(
    @SerializedName("email", alternate = ["username"])
    val email: String,
    @SerializedName("password")
    val password: String
)

data class TeacherLoginData(
    val message: String? = null,
    val teacher: TeacherProfile? = null,
    val authenticated: Boolean? = null
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: TeacherLoginData? = null,
    val username: String? = null,
    val role: String? = null,
    val displayName: String? = null,
    val teacherId: String? = null,
    val department: String? = null
)

data class TeacherProfile(
    val id: String?,
    val name: String,
    val email: String,
    val employeeId: String?,
    val department: String?,
    val departmentName: String? = null,
    val departmentCode: String? = null,
    val phone: String?
)

data class ClassItem(
    val id: String,
    val departmentId: String?,
    val departmentName: String?,
    val departmentCode: String?,
    val semester: Int,
    val section: String,
    val displayName: String?,
    val academicYear: String?,
    val studentCount: Int = 0
)

data class SubjectItem(
    val id: String,
    val code: String,
    val name: String,
    val departmentId: String?,
    val departmentName: String?,
    val semester: Int,
    val credits: Int = 3
)

data class TimetableItem(
    val id: String? = null,
    val classId: String? = null,
    val className: String? = null,
    val section: String? = null,
    val semester: Int? = null,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val subjectCode: String? = null,
    val teacherId: String? = null,
    val teacherName: String? = null,
    val dayOfWeek: String? = null,
    val dayName: String? = null,
    val periodNumber: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val startTimeFormatted: String? = null,
    val endTimeFormatted: String? = null,
    val timeSlot: String? = null,
    val roomNumber: String? = null,
    val status: String? = null
) {
    val displayDay: String
        get() = (dayName ?: dayOfWeek ?: "").trim().uppercase()

    val displayPeriod: String
        get() = "Period ${periodNumber ?: 1}"

    val displayTime: String
        get() {
            if (!timeSlot.isNullOrBlank()) return timeSlot
            val s = startTimeFormatted ?: startTime ?: ""
            val e = endTimeFormatted ?: endTime ?: ""
            return if (s.isNotBlank() && e.isNotBlank()) "$s – $e" else s
        }

    val displayClass: String
        get() = when {
            !className.isNullOrBlank() && !section.isNullOrBlank() && !className.contains(section) -> "$className ($section)"
            !className.isNullOrBlank() -> className
            !section.isNullOrBlank() -> "Section $section"
            else -> "Class"
        }
}

data class AttendanceSessionItem(
    val id: String,
    val sessionCode: String,
    val teacherId: String?,
    val teacherName: String?,
    val classId: String?,
    val className: String?,
    val departmentName: String?,
    val semester: Int = 0,
    val section: String?,
    val subjectId: String?,
    val subjectName: String?,
    val subjectCode: String?,
    val teacherLatitude: Double,
    val teacherLongitude: Double,
    val geofenceRadiusMeters: Double = 40.0,
    val bluetoothVerificationEnabled: Boolean = false,
    val bleBeaconUuid: String? = null,
    val bleRssiThreshold: Int? = -75,
    val startTime: String?,
    val endTime: String?,
    val status: String,
    val presentCount: Long = 0,
    val totalEnrolled: Long = 0,
    val attendanceLink: String?
)

data class CreateSessionRequest(
    val classId: String,
    val subjectId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val durationMinutes: Int = 15,
    val enableBluetoothVerification: Boolean = false,
    val bleRssiThreshold: Int = -75
)

data class AttendanceRecordItem(
    val id: String,
    val studentName: String?,
    val usn: String?,
    val status: String,
    val studentLatitude: Double?,
    val studentLongitude: Double?,
    val distanceFromTeacher: Double?,
    val bleVerified: Boolean = false,
    val bleRssi: Int? = null,
    val markedAt: String?
)

data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val minVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String?,
    val sha256: String?,
    val fileSizeBytes: Long = 0,
    val publishedAt: String?
)

data class TeacherDashboardStats(
    val teacherName: String?,
    val department: String?,
    val activeSessionsCount: Int = 0,
    val totalClassesToday: Int = 0,
    val totalStudentsMarkedToday: Int = 0,
    val activeSession: AttendanceSessionItem? = null
)

data class StudentItem(
    val id: String,
    val usn: String,
    val name: String,
    val email: String? = null,
    val departmentId: String? = null,
    val departmentCode: String? = null,
    val departmentName: String? = null,
    val classId: String? = null,
    val className: String? = null,
    val semester: Int? = null,
    val section: String? = null,
    val status: String? = null
)

data class AttendanceSummaryItem(
    val studentId: String,
    val usn: String,
    val studentName: String,
    val department: String?,
    val semester: Int = 0,
    val section: String?,
    val subject: String?,
    val totalClasses: Long = 0,
    val attended: Long = 0,
    val absent: Long = 0,
    val percentage: Double = 0.0
)

data class SubjectAttendanceItem(
    val subjectId: String?,
    val subjectCode: String?,
    val subjectName: String?,
    val totalConducted: Long = 0,
    val totalPresent: Long = 0,
    val totalAbsent: Long = 0,
    val percentage: Double = 0.0
)

data class StudentHistoryRecordItem(
    val sessionId: String?,
    val sessionCode: String?,
    val subjectCode: String?,
    val subjectName: String?,
    val sessionDate: String?,
    val sessionTime: String?,
    val status: String?,
    val markedTime: String?,
    val distanceFromTeacher: String?
)

data class StudentProfileItem(
    val id: String,
    val usn: String,
    val name: String,
    val email: String? = null,
    val department: String? = null,
    val semester: Int = 0,
    val section: String? = null,
    val totalConducted: Long = 0,
    val totalPresent: Long = 0,
    val totalAbsent: Long = 0,
    val overallPercentage: Double = 0.0,
    val attendanceHistory: List<StudentHistoryRecordItem> = emptyList(),
    val subjectWiseAttendance: List<SubjectAttendanceItem> = emptyList()
)

data class DateWiseRecordItem(
    val date: String,
    val sessionId: String? = null,
    val sessionCode: String? = null,
    val studentId: String? = null,
    val studentName: String,
    val usn: String? = null,
    val studentUsn: String? = null,
    val className: String? = null,
    val section: String? = null,
    val subjectCode: String? = null,
    val subjectName: String? = null,
    val status: String,
    val markedTime: String? = null,
    val distanceFromTeacher: String? = null
)

