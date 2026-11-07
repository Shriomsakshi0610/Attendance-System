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
    object StudentDetail : Screen("student_detail/{studentId}") {
        fun createRoute(studentId: String) = "student_detail/$studentId"
    }
}
