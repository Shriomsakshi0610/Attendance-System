package com.renaissance.teacher.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.location.DeviceLocation
import com.renaissance.teacher.data.location.LocationService
import com.renaissance.teacher.data.model.AttendanceSessionItem
import com.renaissance.teacher.data.model.ClassItem
import com.renaissance.teacher.data.model.SubjectItem
import com.renaissance.teacher.data.security.SessionPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val facultyName: String = "",
    val department: String = "",
    val activeSession: AttendanceSessionItem? = null,
    val assignedClasses: List<ClassItem> = emptyList(),
    val assignedSubjects: List<SubjectItem> = emptyList(),
    val currentLocation: DeviceLocation? = null,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val apiClient: ApiClient,
    private val sessionPrefs: SessionPreferences,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            facultyName = sessionPrefs.getTeacherName(),
            department = sessionPrefs.getDepartment()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshDashboard()
        listenLocation()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // Fetch assigned classes
                val classesRes = apiClient.teacherApi.getAssignedClasses()
                val classes = if (classesRes.isSuccessful) classesRes.body()?.data ?: emptyList() else emptyList()

                // Fetch assigned subjects
                val subjectsRes = apiClient.teacherApi.getAssignedSubjects()
                val subjects = if (subjectsRes.isSuccessful) subjectsRes.body()?.data ?: emptyList() else emptyList()

                // Fetch teacher sessions to find any ACTIVE session
                val sessionsRes = apiClient.sessionApi.getTeacherSessions()
                val sessions = if (sessionsRes.isSuccessful) sessionsRes.body()?.data ?: emptyList() else emptyList()
                val active = sessions.firstOrNull { it.status.equals("ACTIVE", ignoreCase = true) }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    assignedClasses = classes,
                    assignedSubjects = subjects,
                    activeSession = active,
                    facultyName = sessionPrefs.getTeacherName(),
                    department = sessionPrefs.getDepartment()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    private fun listenLocation() {
        viewModelScope.launch {
            try {
                locationService.getLocationUpdates().collect { loc ->
                    _uiState.value = _uiState.value.copy(currentLocation = loc)
                }
            } catch (_: Exception) {}
        }
    }
}
