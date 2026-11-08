package com.renaissance.teacher.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object CreateSession : Screen("create_session")
    object ActiveSession : Screen("active_session/{sessionId}") {
        fun createRoute(sessionId: String) = "active_session/$sessionId"
    }
    object Timetable : Screen("timetable")
    object History : Screen("history")
    object Settings : Screen("settings")
    object StudentAttendance : Screen("student_attendance")
    object StudentDetail : Screen("student_detail/{studentId}?classId={classId}&subjectId={subjectId}&subjectName={subjectName}") {
        fun createRoute(
            studentId: String,
            classId: String? = null,
            subjectId: String? = null,
            subjectName: String? = null
        ): String {
            val q = mutableListOf<String>()
            if (!classId.isNullOrBlank()) q.add("classId=" + java.net.URLEncoder.encode(classId, "UTF-8"))
            if (!subjectId.isNullOrBlank()) q.add("subjectId=" + java.net.URLEncoder.encode(subjectId, "UTF-8"))
            if (!subjectName.isNullOrBlank()) q.add("subjectName=" + java.net.URLEncoder.encode(subjectName, "UTF-8"))
            return if (q.isEmpty()) "student_detail/$studentId" else "student_detail/$studentId?${q.joinToString("&")}"
        }
    }
}
