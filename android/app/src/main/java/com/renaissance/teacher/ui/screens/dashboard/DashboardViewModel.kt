package com.renaissance.teacher.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.location.DeviceLocation
import com.renaissance.teacher.data.location.LocationService
import com.renaissance.teacher.data.model.AttendanceSessionItem
import com.renaissance.teacher.data.model.ClassItem
import com.renaissance.teacher.data.model.ResourceState
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
import java.io.IOException
import java.net.SocketTimeoutException

data class DashboardUiState(
    val isLoading: Boolean = false,
    val facultyName: String = "",
    val department: String = "",
    val activeSession: AttendanceSessionItem? = null,
    val assignedClasses: List<ClassItem> = emptyList(),
    val assignedSubjects: List<SubjectItem> = emptyList(),
    val classesState: ResourceState<List<ClassItem>> = ResourceState.Idle,
    val subjectsState: ResourceState<List<SubjectItem>> = ResourceState.Idle,
    val currentLocation: DeviceLocation? = null,
    val errorMessage: String? = null,
    val isStaleData: Boolean = false
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
     * Resilient Dashboard Refresh:
     * 1. Preserves existing valid cached classes and subjects on network/timeout failures (Stale-While-Revalidate).
     * 2. Executes independent calls concurrently via Dispatchers.IO.
     * 3. Prevents duplicate network jobs by checking/cancelling in-flight requests.
     * 4. Categorizes genuine empty states vs transient errors with ResourceState.
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
                val currentClasses = _uiState.value.assignedClasses
                val currentSubjects = _uiState.value.assignedSubjects

                // Concurrent execution of independent network calls
                val classesDeferred = async { apiClient.teacherApi.getAssignedClasses() }
                val subjectsDeferred = async { apiClient.teacherApi.getAssignedSubjects() }
                val activeSessionsDeferred = async { apiClient.sessionApi.getActiveSessions() }

                val classesRes = classesDeferred.await()
                val subjectsRes = subjectsDeferred.await()
                val activeSessionsRes = activeSessionsDeferred.await()

                // Resolve classes with cache preservation
                val newClasses: List<ClassItem>
                val newClassesState: ResourceState<List<ClassItem>>
                if (classesRes.isSuccessful) {
                    val fetched = classesRes.body()?.data
                    if (fetched != null) {
                        newClasses = fetched
                        newClassesState = if (fetched.isEmpty()) ResourceState.Empty("No classes assigned") else ResourceState.Success(fetched)
                    } else {
                        newClasses = currentClasses
                        newClassesState = ResourceState.ServerError(classesRes.code(), "Empty response received for classes")
                    }
                } else {
                    val code = classesRes.code()
                    newClasses = currentClasses
                    newClassesState = if (code == 401 || code == 403) {
                        ResourceState.AuthError("Session expired. Please log in again.")
                    } else {
                        ResourceState.ServerError(code, "Failed to load classes (HTTP $code)")
                    }
                }

                // Resolve subjects with cache preservation
                val newSubjects: List<SubjectItem>
                val newSubjectsState: ResourceState<List<SubjectItem>>
                if (subjectsRes.isSuccessful) {
                    val fetched = subjectsRes.body()?.data
                    if (fetched != null) {
                        newSubjects = fetched
                        newSubjectsState = if (fetched.isEmpty()) ResourceState.Empty("No subjects assigned") else ResourceState.Success(fetched)
                    } else {
                        newSubjects = currentSubjects
                        newSubjectsState = ResourceState.ServerError(subjectsRes.code(), "Empty response received for subjects")
                    }
                } else {
                    val code = subjectsRes.code()
                    newSubjects = currentSubjects
                    newSubjectsState = if (code == 401 || code == 403) {
                        ResourceState.AuthError("Session expired. Please log in again.")
                    } else {
                        ResourceState.ServerError(code, "Failed to load subjects (HTTP $code)")
                    }
                }

                // Identify if an active session belongs to this authenticated faculty member
                val active = if (activeSessionsRes.isSuccessful) {
                    val list = activeSessionsRes.body()?.data ?: emptyList()
                    list.firstOrNull { session ->
                        session.status.equals("ACTIVE", ignoreCase = true) &&
                        (currentTeacherId.isNullOrBlank() || session.teacherId == null || session.teacherId == currentTeacherId)
                    }
                } else {
                    _uiState.value.activeSession
                }

                val hasAnyError = newClassesState.isError || newSubjectsState.isError
                val hasCachedData = newClasses.isNotEmpty() || newSubjects.isNotEmpty()
                val isStale = hasAnyError && hasCachedData
                val errMsg = if (isStale) {
                    "Unable to refresh some faculty data. Showing cached records."
                } else if (hasAnyError && !hasCachedData) {
                    newClassesState.getErrorMessageOrNull() ?: newSubjectsState.getErrorMessageOrNull() ?: "Failed to load dashboard data."
                } else {
                    null
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    assignedClasses = newClasses,
                    assignedSubjects = newSubjects,
                    classesState = newClassesState,
                    subjectsState = newSubjectsState,
                    activeSession = active,
                    isStaleData = isStale,
                    errorMessage = errMsg,
                    facultyName = sessionPrefs.getTeacherName(),
                    department = sessionPrefs.getDepartment()
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val currentClasses = _uiState.value.assignedClasses
                val currentSubjects = _uiState.value.assignedSubjects
                val hasCached = currentClasses.isNotEmpty() || currentSubjects.isNotEmpty()

                val errorState = when (e) {
                    is SocketTimeoutException -> ResourceState.Timeout("Connection timed out. Server may be warming up.")
                    is IOException -> ResourceState.NetworkError("Network connection interrupted.")
                    else -> ResourceState.NetworkError(e.localizedMessage ?: "Failed to refresh dashboard.")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    classesState = errorState,
                    subjectsState = errorState,
                    isStaleData = hasCached,
                    errorMessage = if (hasCached) "Connection timed out. Showing cached data." else (e.localizedMessage ?: "Failed to connect to server.")
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
