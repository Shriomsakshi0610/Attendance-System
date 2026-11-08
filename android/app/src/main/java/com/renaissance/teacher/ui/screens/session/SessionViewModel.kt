package com.renaissance.teacher.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.bluetooth.BleAdvertiserManager
import com.renaissance.teacher.data.location.DeviceLocation
import com.renaissance.teacher.data.location.LocationService
import com.renaissance.teacher.data.model.AttendanceRecordItem
import com.renaissance.teacher.data.model.AttendanceSessionItem
import com.renaissance.teacher.data.model.ClassItem
import com.renaissance.teacher.data.model.CreateSessionRequest
import com.renaissance.teacher.data.model.SubjectItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SessionUiState(
    val isLoading: Boolean = false,
    val classes: List<ClassItem> = emptyList(),
    val subjects: List<SubjectItem> = emptyList(),
    val selectedClass: ClassItem? = null,
    val selectedSubject: SubjectItem? = null,
    val durationMinutes: Int = 15,
    val enableBluetoothVerification: Boolean = true, // Enabled by default for dual verification
    val bleRssiThreshold: Int = -75,
    val currentLocation: DeviceLocation? = null,
    val activeSession: AttendanceSessionItem? = null,
    val attendeeRecords: List<AttendanceRecordItem> = emptyList(),
    val isAdvertisingBle: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SessionViewModel(
    private val apiClient: ApiClient,
    private val locationService: LocationService,
    private val bleAdvertiser: BleAdvertiserManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadClassesAndSubjects()
        fetchLocation()
    }

    fun loadClassesAndSubjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val currentClasses = _uiState.value.classes
                val currentSubjects = _uiState.value.subjects

                val cRes = apiClient.teacherApi.getAssignedClasses()
                val sRes = apiClient.teacherApi.getAssignedSubjects()

                val newClasses = if (cRes.isSuccessful && cRes.body()?.data != null) {
                    cRes.body()!!.data!!
                } else {
                    currentClasses
                }

                val newSubjects = if (sRes.isSuccessful && sRes.body()?.data != null) {
                    sRes.body()!!.data!!
                } else {
                    currentSubjects
                }

                val hasFailure = (!cRes.isSuccessful && currentClasses.isEmpty()) || (!sRes.isSuccessful && currentSubjects.isEmpty())
                val errorMsg = if (hasFailure) {
                    "Unable to load classes or subjects. Please check your network connection."
                } else null

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    classes = newClasses,
                    subjects = newSubjects,
                    selectedClass = _uiState.value.selectedClass ?: newClasses.firstOrNull(),
                    selectedSubject = _uiState.value.selectedSubject ?: newSubjects.firstOrNull(),
                    errorMessage = errorMsg
                )
            } catch (e: Exception) {
                val currentClasses = _uiState.value.classes
                val currentSubjects = _uiState.value.subjects
                val hasCached = currentClasses.isNotEmpty() || currentSubjects.isNotEmpty()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (hasCached) "Connection issue. Showing cached classes." else (e.message ?: "Failed to load classes")
                )
            }
        }
    }

    fun fetchLocation() {
        viewModelScope.launch {
            val loc = locationService.getCurrentLocation()
            _uiState.value = _uiState.value.copy(currentLocation = loc)
        }
    }

    fun selectClass(cls: ClassItem) {
        _uiState.value = _uiState.value.copy(selectedClass = cls)
    }

    fun selectSubject(subj: SubjectItem) {
        _uiState.value = _uiState.value.copy(selectedSubject = subj)
    }

    fun setDuration(duration: Int) {
        _uiState.value = _uiState.value.copy(durationMinutes = duration)
    }

    fun toggleBluetoothVerification(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableBluetoothVerification = enabled)
    }

    fun createSession(onSuccess: (String) -> Unit) {
        val s = _uiState.value
        val cls = s.selectedClass
        val subj = s.selectedSubject
        val loc = s.currentLocation

        if (cls == null || subj == null) {
            _uiState.value = s.copy(errorMessage = "Please select both a class and a subject.")
            return
        }

        if (loc == null) {
            _uiState.value = s.copy(errorMessage = "GPS fix not yet acquired. Please wait a moment and try again.")
            return
        }

        viewModelScope.launch {
            _uiState.value = s.copy(isLoading = true, errorMessage = null)
            try {
                val request = CreateSessionRequest(
                    classId = cls.id,
                    subjectId = subj.id,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy.toDouble(),
                    durationMinutes = s.durationMinutes,
                    enableBluetoothVerification = s.enableBluetoothVerification,
                    bleRssiThreshold = s.bleRssiThreshold
                )

                val response = apiClient.sessionApi.createSession(request)
                if (response.isSuccessful && response.body()?.data != null) {
                    val session = response.body()!!.data!!
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeSession = session,
                        successMessage = "Attendance session created successfully!"
                    )

                    // If BLE enabled, start beacon advertising
                    if (session.bluetoothVerificationEnabled && !session.bleBeaconUuid.isNullOrBlank()) {
                        bleAdvertiser.startAdvertising(
                            sessionUuid = session.bleBeaconUuid,
                            onStarted = {
                                _uiState.value = _uiState.value.copy(isAdvertisingBle = true)
                            },
                            onError = { err ->
                                _uiState.value = _uiState.value.copy(errorMessage = "BLE Warning: $err")
                            }
                        )
                    }

                    startPollingAttendees(session.id)
                    onSuccess(session.id)
                } else {
                    val msg = response.body()?.message ?: "Failed to start attendance session."
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error creating session: ${e.message}"
                )
            }
        }
    }

    fun loadActiveSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.sessionApi.getSessionById(sessionId)
                if (response.isSuccessful && response.body()?.data != null) {
                    val session = response.body()!!.data!!
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeSession = session
                    )

                    if (session.bluetoothVerificationEnabled && !session.bleBeaconUuid.isNullOrBlank()) {
                        bleAdvertiser.startAdvertising(
                            sessionUuid = session.bleBeaconUuid,
                            onStarted = {
                                _uiState.value = _uiState.value.copy(isAdvertisingBle = true)
                            }
                        )
                    }

                    startPollingAttendees(sessionId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to load session details."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    private fun startPollingAttendees(sessionId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val recRes = apiClient.sessionApi.getSessionRecords(sessionId)
                    if (recRes.isSuccessful && recRes.body()?.data != null) {
                        val records = recRes.body()!!.data!!
                        _uiState.value = _uiState.value.copy(attendeeRecords = records)
                    }
                    val sessRes = apiClient.sessionApi.getSessionById(sessionId)
                    if (sessRes.isSuccessful && sessRes.body()?.data != null) {
                        val session = sessRes.body()!!.data!!
                        _uiState.value = _uiState.value.copy(activeSession = session)
                    }
                } catch (_: Exception) {}
                delay(3000)
            }
        }
    }

    fun endSession(onEnded: () -> Unit) {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.sessionApi.endSession(session.id)
                bleAdvertiser.stopAdvertising()
                pollingJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeSession = null,
                    isAdvertisingBle = false
                )
                onEnded()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        bleAdvertiser.stopAdvertising()
    }
}
