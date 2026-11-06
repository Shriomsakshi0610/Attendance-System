package com.renaissance.teacher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.renaissance.teacher.ui.navigation.Screen
import com.renaissance.teacher.ui.screens.auth.LoginScreen
import com.renaissance.teacher.ui.screens.dashboard.DashboardScreen
import com.renaissance.teacher.ui.screens.dashboard.DashboardViewModel
import com.renaissance.teacher.ui.screens.history.AttendanceHistoryScreen
import com.renaissance.teacher.ui.screens.session.ActiveSessionScreen
import com.renaissance.teacher.ui.screens.session.CreateSessionScreen
import com.renaissance.teacher.ui.screens.session.SessionViewModel
import com.renaissance.teacher.ui.screens.splash.SplashScreen
import com.renaissance.teacher.ui.screens.timetable.TimetableScreen
import com.renaissance.teacher.ui.screens.update.SettingsUpdateScreen
import com.renaissance.teacher.ui.theme.RenaissanceTeacherTheme

class MainActivity : ComponentActivity() {

    private val app: RenaissanceTeacherApp get() = application as RenaissanceTeacherApp

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions evaluated
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()

        setContent {
            RenaissanceTeacherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TeacherNavHost(app = app)
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }
}

@Composable
fun TeacherNavHost(app: RenaissanceTeacherApp) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                sessionPrefs = app.sessionPreferences,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                apiClient = app.apiClient,
                sessionPrefs = app.sessionPreferences,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val dashboardVm = viewModel {
                DashboardViewModel(
                    apiClient = app.apiClient,
                    sessionPrefs = app.sessionPreferences,
                    locationService = app.locationService
                )
            }
            DashboardScreen(
                viewModel = dashboardVm,
                onStartNewSession = { navController.navigate(Screen.CreateSession.route) },
                onOpenActiveSession = { id -> navController.navigate(Screen.ActiveSession.createRoute(id)) },
                onOpenTimetable = { navController.navigate(Screen.Timetable.route) },
                onOpenHistory = { navController.navigate(Screen.History.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    app.sessionPreferences.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateSession.route) {
            val sessionVm = viewModel {
                SessionViewModel(
                    apiClient = app.apiClient,
                    locationService = app.locationService,
                    bleAdvertiser = app.bleAdvertiser
                )
            }
            CreateSessionScreen(
                viewModel = sessionVm,
                onNavigateBack = { navController.popBackStack() },
                onSessionCreated = { sessionId ->
                    navController.navigate(Screen.ActiveSession.createRoute(sessionId)) {
                        popUpTo(Screen.CreateSession.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ActiveSession.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val sessionVm = viewModel {
                SessionViewModel(
                    apiClient = app.apiClient,
                    locationService = app.locationService,
                    bleAdvertiser = app.bleAdvertiser
                )
            }
            ActiveSessionScreen(
                sessionId = sessionId,
                viewModel = sessionVm,
                onNavigateBack = { navController.popBackStack() },
                onSessionEnded = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Timetable.route) {
            TimetableScreen(
                apiClient = app.apiClient,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            AttendanceHistoryScreen(
                apiClient = app.apiClient,
                onNavigateBack = { navController.popBackStack() },
                onSelectSession = { id ->
                    navController.navigate(Screen.ActiveSession.createRoute(id))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsUpdateScreen(
                updateManager = app.updateManager,
                bleAdvertiser = app.bleAdvertiser,
                sessionPrefs = app.sessionPreferences,
                apiClient = app.apiClient,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
