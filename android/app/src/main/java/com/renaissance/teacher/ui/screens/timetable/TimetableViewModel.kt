package com.renaissance.teacher.ui.screens.timetable

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.model.ResourceState
import com.renaissance.teacher.data.model.TimetableItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class TimetableViewModel(
    private val apiClient: ApiClient
) : ViewModel() {

    private val _timetableState = MutableStateFlow<ResourceState<List<TimetableItem>>>(ResourceState.Idle)
    val timetableState: StateFlow<ResourceState<List<TimetableItem>>> = _timetableState.asStateFlow()

    // Preserved cached data for Stale-While-Revalidate pattern
    private val _cachedSchedules = MutableStateFlow<List<TimetableItem>>(emptyList())
    val cachedSchedules: StateFlow<List<TimetableItem>> = _cachedSchedules.asStateFlow()

    private val initialDay: String = try {
        val d = LocalDate.now().dayOfWeek.name
        if (d == "SUNDAY") "MONDAY" else d
    } catch (_: Exception) {
        "MONDAY"
    }

    private val _selectedDay = MutableStateFlow(initialDay)
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var activeJob: Job? = null
    private val requestCounter = AtomicInteger(0)

    init {
        loadTimetable()
    }

    fun selectDay(day: String) {
        _selectedDay.value = day
    }

    /**
     * Dispatches timetable loading with race condition prevention, stale-while-revalidate caching,
     * and explicit categorization of Genuine Empty vs Network/Timeout/Server errors.
     */
    fun loadTimetable(isUserRefresh: Boolean = false) {
        // Prevent concurrent identical requests
        if (activeJob?.isActive == true && !isUserRefresh) {
            return
        }
        activeJob?.cancel()

        val requestId = requestCounter.incrementAndGet()

        activeJob = viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = isUserRefresh
            if (!isUserRefresh && _cachedSchedules.value.isEmpty()) {
                _timetableState.value = ResourceState.Loading
            }

            try {
                val response = apiClient.timetableApi.getTeacherTimetable()

                // Guard against stale asynchronous response overwriting newer invocation
                if (requestId != requestCounter.get()) {
                    Log.d("TimetableViewModel", "Discarding stale response (id: $requestId != current: ${requestCounter.get()})")
                    return@launch
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    val data = body?.data
                    if (data == null || data.isEmpty()) {
                        // Genuine Server-Verified Empty State
                        _cachedSchedules.value = emptyList()
                        _timetableState.value = ResourceState.Empty("No timetable has been assigned to you yet.")
                    } else {
                        // Success with Data
                        _cachedSchedules.value = data
                        _timetableState.value = ResourceState.Success(data)
                    }
                } else {
                    val code = response.code()
                    val errorState = when (code) {
                        401, 403 -> ResourceState.AuthError("Session expired. Please log in again.")
                        in 500..599 -> ResourceState.ServerError(code, "Server temporarily unavailable (HTTP $code).")
                        else -> ResourceState.ServerError(code, "Failed to load timetable (HTTP $code).")
                    }
                    _timetableState.value = errorState
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d("TimetableViewModel", "Timetable fetch cancelled for requestId: $requestId")
                    throw e
                }

                val errorState = when (e) {
                    is SocketTimeoutException -> ResourceState.Timeout("Connection timed out. Server may be warming up. Tap Retry to reconnect.")
                    is IOException -> ResourceState.NetworkError("Network connection error. Please check your internet connection.")
                    else -> ResourceState.NetworkError(e.localizedMessage ?: "Unexpected error occurred. Please try again.")
                }
                _timetableState.value = errorState
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeJob?.cancel()
    }
}
