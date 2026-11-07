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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

    private var refreshJob: Job? = null

    init {
        refreshDashboard()
        listenLocation()
    }

    /**
     * Optimized Dashboard Refresh:
     * 1. Cancels any in-flight refresh job to prevent duplicate network requests.
     * 2. Executes on Dispatchers.IO.
     * 3. Dispatches classes, subjects, and active session checks concurrently via async/await.
     * 4. Queries getActiveSessions() directly instead of downloading entire historical session records.
     * 5. Preserves user session metadata, active states, and error handling.
     */
    fun refreshDashboard() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                facultyName = sessionPrefs.getTeacherName(),
                department = sessionPrefs.getDepartment()
            )
            try {
                val currentTeacherId = sessionPrefs.getTeacherId()

                // Concurrent execution of independent network calls
                val classesDeferred = async { apiClient.teacherApi.getAssignedClasses() }
                val subjectsDeferred = async { apiClient.teacherApi.getAssignedSubjects() }
                val activeSessionsDeferred = async { apiClient.sessionApi.getActiveSessions() }

                val classesRes = classesDeferred.await()
                val subjectsRes = subjectsDeferred.await()
                val activeSessionsRes = activeSessionsDeferred.await()

                val classes = if (classesRes.isSuccessful) classesRes.body()?.data ?: emptyList() else emptyList()
                val subjects = if (subjectsRes.isSuccessful) subjectsRes.body()?.data ?: emptyList() else emptyList()

                // Identify if an active session belongs to this authenticated faculty member
                val activeSessions = if (activeSessionsRes.isSuccessful) activeSessionsRes.body()?.data ?: emptyList() else emptyList()
                val active = activeSessions.firstOrNull { session ->
                    session.status.equals("ACTIVE", ignoreCase = true) &&
                    (currentTeacherId.isNullOrBlank() || session.teacherId == null || session.teacherId == currentTeacherId)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    assignedClasses = classes,
                    assignedSubjects = subjects,
                    activeSession = active,
                    facultyName = sessionPrefs.getTeacherName(),
                    department = sessionPrefs.getDepartment()
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to refresh dashboard"
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

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
