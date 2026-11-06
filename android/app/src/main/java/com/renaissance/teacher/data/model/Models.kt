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
    val id: String,
    val classId: String,
    val className: String?,
    val subjectId: String,
    val subjectName: String?,
    val subjectCode: String?,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val roomNumber: String?
)

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
    val geofenceRadiusMeters: Double = 15.0,
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
