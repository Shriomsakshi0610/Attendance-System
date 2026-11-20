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
    val durationMinutes: Int = 5,
    val enableBluetoothVerification: Boolean = true, // Enabled by default for dual verification
    val bleRssiThreshold: Int = -75,
    val currentLocation: DeviceLocation? = null,
    val activeSession: AttendanceSessionItem? = null,
    val attendeeRecords: List<AttendanceRecordItem> = emptyList(),
    val isAdvertisingBle: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val remainingSeconds: Long = 0L,
    val formattedRemainingTime: String = "05:00",
    val isSessionExpired: Boolean = false
)

class SessionViewModel(
    private val apiClient: ApiClient,
    private val locationService: LocationService,
    private val bleAdvertiser: BleAdvertiserManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var timerJob: Job? = null
    private var baselineRemainingSeconds: Long = 0L
    private var baselineNanoTime: Long = 0L

    init {
        loadClassesAndSubjects()
        fetchLocation()
    }

    private fun startCountdownTimer(initialSeconds: Long) {
        timerJob?.cancel()
        baselineRemainingSeconds = maxOf(0L, initialSeconds)
        baselineNanoTime = System.nanoTime()

        val isExpired = baselineRemainingSeconds <= 0L
        _uiState.value = _uiState.value.copy(
            remainingSeconds = baselineRemainingSeconds,
            formattedRemainingTime = formatSecondsToMmSs(baselineRemainingSeconds),
            isSessionExpired = isExpired
        )

        if (isExpired) return

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val elapsedSecs = (System.nanoTime() - baselineNanoTime) / 1_000_000_000L
                val currentRemaining = maxOf(0L, baselineRemainingSeconds - elapsedSecs)
                val expiredNow = currentRemaining <= 0L

                _uiState.value = _uiState.value.copy(
                    remainingSeconds = currentRemaining,
                    formattedRemainingTime = formatSecondsToMmSs(currentRemaining),
                    isSessionExpired = expiredNow
                )

                if (expiredNow) {
                    val currentSess = _uiState.value.activeSession
                    if (currentSess != null && currentSess.status.equals("ACTIVE", ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(
                            activeSession = currentSess.copy(status = "EXPIRED", remainingSeconds = 0L)
                        )
                    }
                    break
                }
            }
        }
    }

    private fun syncServerRemainingTime(serverRemaining: Long) {
        val elapsedSecs = (System.nanoTime() - baselineNanoTime) / 1_000_000_000L
        val currentLocalRemaining = maxOf(0L, baselineRemainingSeconds - elapsedSecs)
        if (Math.abs(currentLocalRemaining - serverRemaining) >= 2L) {
            baselineRemainingSeconds = serverRemaining
            baselineNanoTime = System.nanoTime()
            val expiredNow = serverRemaining <= 0L
            _uiState.value = _uiState.value.copy(
                remainingSeconds = serverRemaining,
                formattedRemainingTime = formatSecondsToMmSs(serverRemaining),
                isSessionExpired = expiredNow
            )
            if (expiredNow) {
                timerJob?.cancel()
            }
        }
    }

    private fun formatSecondsToMmSs(seconds: Long): String {
        val totalSec = maxOf(0L, seconds)
        val minutes = totalSec / 60
        val secs = totalSec % 60
        return String.format("%02d:%02d", minutes, secs)
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
                    val remaining = session.remainingSeconds ?: 300L
                    startCountdownTimer(remaining)

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
                    val isExpired = session.isWindowExpired || (session.remainingSeconds != null && session.remainingSeconds <= 0L)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeSession = session,
                        isSessionExpired = isExpired
                    )

                    if (isExpired) {
                        _uiState.value = _uiState.value.copy(
                            remainingSeconds = 0L,
                            formattedRemainingTime = "00:00",
                            isSessionExpired = true
                        )
                        timerJob?.cancel()
                    } else {
                        val remaining = session.remainingSeconds ?: 300L
                        startCountdownTimer(remaining)
                    }

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
                        if (session.isWindowExpired || (session.remainingSeconds != null && session.remainingSeconds <= 0L)) {
                            _uiState.value = _uiState.value.copy(
                                isSessionExpired = true,
                                remainingSeconds = 0L,
                                formattedRemainingTime = "00:00"
                            )
                            timerJob?.cancel()
                        } else if (session.remainingSeconds != null) {
                            syncServerRemainingTime(session.remainingSeconds)
                        }
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
                timerJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeSession = null,
                    isAdvertisingBle = false,
                    remainingSeconds = 0L,
                    formattedRemainingTime = "00:00",
                    isSessionExpired = true
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
        timerJob?.cancel()
        bleAdvertiser.stopAdvertising()
    }
}
