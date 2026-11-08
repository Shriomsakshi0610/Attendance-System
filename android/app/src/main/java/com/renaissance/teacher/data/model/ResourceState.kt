package com.renaissance.teacher.data.model

/**
 * Sealed representation of UI Resource States to guarantee deterministic data presentation.
 * Distinctly separates actual empty data states from network dropouts, timeouts, and auth failures,
 * strictly preventing false "No Classes Scheduled" or "No Timetable Assigned" states.
 */
sealed class ResourceState<out T> {

    /**
     * Initial uninitialized state before any query has been dispatched.
     */
    object Idle : ResourceState<Nothing>()

    /**
     * Active asynchronous network request in flight.
     */
    object Loading : ResourceState<Nothing>()

    /**
     * Successfully resolved request returning verified non-empty data.
     */
    data class Success<out T>(val data: T) : ResourceState<T>()

    /**
     * Successfully resolved request where the server explicitly verified that the faculty member
     * genuinely has zero records assigned (e.g. HTTP 200 OK with empty data list).
     */
    data class Empty(val message: String = "No data available") : ResourceState<Nothing>()

    /**
     * General connectivity, DNS resolution, or IO abort errors.
     */
    data class NetworkError(val message: String) : ResourceState<Nothing>()

    /**
     * HTTP 5xx or server-side failure.
     */
    data class ServerError(val code: Int, val message: String) : ResourceState<Nothing>()

    /**
     * Request timed out while connecting or reading from backend.
     */
    data class Timeout(val message: String) : ResourceState<Nothing>()

    /**
     * HTTP 401 Unauthorized or 403 Forbidden.
     */
    data class AuthError(val message: String = "Session expired. Please log in again.") : ResourceState<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isLoading: Boolean get() = this is Loading
    val isEmpty: Boolean get() = this is Empty
    val isError: Boolean get() = this is NetworkError || this is ServerError || this is Timeout || this is AuthError

    fun getDataOrNull(): T? = (this as? Success)?.data

    fun getErrorMessageOrNull(): String? = when (this) {
        is NetworkError -> message
        is ServerError -> message
        is Timeout -> message
        is AuthError -> message
        else -> null
    }
}
