package com.renaissance.teacher.data.api

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.renaissance.teacher.BuildConfig
import com.renaissance.teacher.data.security.SessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ApiClient(
    private val context: Context,
    val sessionPrefs: SessionPreferences = SessionPreferences.getInstance(context)
) {

    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()
    private val sessionRenewalLock = Any()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isNotEmpty()) {
                val current = cookieStore[url.host]?.toMutableList() ?: mutableListOf()
                for (newCookie in cookies) {
                    current.removeAll { it.name == newCookie.name }
                    current.add(newCookie)
                    if (newCookie.name == "RENAISSANCE_SESSION" || newCookie.name == "JSESSIONID") {
                        val fullCookie = "${newCookie.name}=${newCookie.value}"
                        sessionPrefs.saveSessionCookie(
                            rawCookie = fullCookie,
                            name = newCookie.name,
                            value = newCookie.value
                        )
                        Log.i("AttendanceNetworkDiag", "[CookieJar] Saved session cookie: ${newCookie.name} for ${url.host}")
                    }
                }
                cookieStore[url.host] = current
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val inMemory = cookieStore[url.host]
            if (!inMemory.isNullOrEmpty()) {
                return inMemory
            }

            // Restore from persistent storage on fresh start / process recreation
            val restoredList = mutableListOf<Cookie>()
            val cookieName = sessionPrefs.getSessionCookieName()
            val cookieVal = sessionPrefs.getSessionCookieValue()

            if (!cookieVal.isNullOrBlank()) {
                try {
                    val cookie = Cookie.Builder()
                        .name(cookieName)
                        .value(cookieVal)
                        .hostOnlyDomain(url.host)
                        .path("/")
                        .apply {
                            if (url.isHttps) secure()
                            httpOnly()
                        }
                        .build()
                    restoredList.add(cookie)
                    cookieStore[url.host] = restoredList
                    Log.i("AttendanceNetworkDiag", "[CookieJar] Restored persistent cookie: ${cookie.name} for ${url.host}")
                } catch (e: Exception) {
                    Log.e("AttendanceNetworkDiag", "[CookieJar] Error restoring cookie: ${e.message}")
                }
            }
            return restoredList
        }
    }

    /**
     * Interceptor ensuring the session cookie is unconditionally attached to every authenticated request,
     * protecting against any edge-case CookieJar domain/path mismatch or process recreation gaps.
     */
    private val authCookieInterceptor = Interceptor { chain ->
        val request = chain.request()
        val cookieHeader = sessionPrefs.getSessionCookieHeader()

        if (!cookieHeader.isNullOrBlank()) {
            val existing = request.header("Cookie")
            if (existing.isNullOrBlank()) {
                val authenticatedRequest = request.newBuilder()
                    .header("Cookie", cookieHeader)
                    .build()
                return@Interceptor chain.proceed(authenticatedRequest)
            } else if (!existing.contains(cookieHeader)) {
                // If existing cookie does not match the latest session cookie, replace session cookie with fresh one
                val cleanExisting = existing.split("; ")
                    .filterNot { it.startsWith("RENAISSANCE_SESSION=") || it.startsWith("JSESSIONID=") }
                    .joinToString("; ")
                val combined = if (cleanExisting.isBlank()) cookieHeader else "$cleanExisting; $cookieHeader"
                val authenticatedRequest = request.newBuilder()
                    .header("Cookie", combined)
                    .build()
                return@Interceptor chain.proceed(authenticatedRequest)
            }
        }
        chain.proceed(request)
    }

    /**
     * Interceptor providing limited exponential backoff retry for transient network disruptions.
     * Retries idempotent GET requests on SocketTimeoutException, ConnectException, and 503 Gateway errors.
     * Strictly ignores 400, 401, 403, 404, and 500 errors.
     */
    private val transientRetryInterceptor = Interceptor { chain ->
        val request = chain.request()
        val isGet = request.method.equals("GET", ignoreCase = true)
        val maxRetries = if (isGet) 1 else 0
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)
                if (response.code == 503 && isGet && attempt < maxRetries) {
                    response.close()
                    attempt++
                    val backoff = 1000L
                    Log.w("AttendanceNetworkDiag", "[Transient Retry] HTTP 503 for ${request.url.encodedPath}. Retrying in ${backoff}ms (Attempt $attempt/$maxRetries)")
                    try {
                        Thread.sleep(backoff)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return@Interceptor response
                    }
                    continue
                }
                return@Interceptor response
            } catch (e: IOException) {
                lastException = e
                val isRetryable = e is SocketTimeoutException || e is ConnectException || e is UnknownHostException
                if (isRetryable && isGet && attempt < maxRetries) {
                    attempt++
                    val backoff = 1000L
                    Log.w("AttendanceNetworkDiag", "[Transient Retry] ${e.javaClass.simpleName} for ${request.url.encodedPath}. Retrying in ${backoff}ms (Attempt $attempt/$maxRetries)")
                    try {
                        Thread.sleep(backoff)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw e
                    }
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }

    /**
     * Diagnostic logging interceptor that records request duration, HTTP status code,
     * authenticated non-sensitive faculty ID, and response element counts.
     * Never records passwords, authorization headers, cookies, or student PII.
     */
    private val auditDiagnosticInterceptor = Interceptor { chain ->
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        val path = request.url.encodedPath
        val method = request.method
        val facultyId = sessionPrefs.getTeacherId() ?: "anonymous"

        try {
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - startTime
            val code = response.code

            // Safely inspect response item count without consuming original response body
            val itemCount = try {
                val peek = response.peekBody(64 * 1024).string()
                val json = JsonParser.parseString(peek)
                if (json.isJsonObject) {
                    val data = json.asJsonObject.get("data")
                    when {
                        data == null || data.isJsonNull -> 0
                        data.isJsonArray -> data.asJsonArray.size()
                        else -> 1
                    }
                } else if (json.isJsonArray) {
                    json.asJsonArray.size()
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }

            val countInfo = if (itemCount != null) " | Items: $itemCount" else ""
            Log.i("AttendanceNetworkDiag", "[API Audit] $method $path -> HTTP $code (${duration}ms) | Faculty: $facultyId$countInfo")
            response
        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime
            Log.e("AttendanceNetworkDiag", "[API Audit Failure] $method $path FAILED after ${duration}ms | Faculty: $facultyId | ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }

    /**
     * Endpoint-specific timeout interceptor tailored for Render Free Tier cold starts.
     * Endpoints subject to server awakening or heavy initialization (auth, teacher profile, reports, session-activation)
     * are granted up to 35 seconds to avoid socket aborts before the JVM finishes booting.
     * Standard transactional APIs retain fast, responsive 20s timeouts.
     */
    private val endpointTimeoutInterceptor = Interceptor { chain ->
        val request = chain.request()
        val path = request.url.encodedPath

        val isDownload = path.contains("api/public/app/download")
        val isColdStartSensitive = path.contains("api/auth") ||
                path.contains("health") ||
                path.contains("api/reports") ||
                path.contains("api/sessions/active") ||
                path.contains("api/public/app") ||
                path.contains("api/teacher") ||
                path.contains("api/timetable")

        if (isDownload) {
            chain.withConnectTimeout(45, TimeUnit.SECONDS)
                .withReadTimeout(120, TimeUnit.SECONDS)
                .proceed(request)
        } else if (isColdStartSensitive) {
            chain.withConnectTimeout(35, TimeUnit.SECONDS)
                .withReadTimeout(35, TimeUnit.SECONDS)
                .proceed(request)
        } else {
            chain.withConnectTimeout(20, TimeUnit.SECONDS)
                .withReadTimeout(20, TimeUnit.SECONDS)
                .proceed(request)
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    /**
     * Thread-Safe Session Renewal Authenticator.
     * Automatically handles 401 Unauthorized responses across all protected endpoints.
     * Synchronizes concurrent requests to prevent duplicate refresh calls, invokes /api/auth/refresh
     * using the persistent refresh token, updates preferences/cookies, and retries the original request.
     */
    private val sessionAuthenticator = Authenticator { _: Route?, response: Response ->
        if (responseCount(response) >= 3) {
            Log.w("AttendanceNetworkDiag", "[Authenticator] Max retry limit reached for ${response.request.url.encodedPath}. Giving up.")
            return@Authenticator null
        }

        val path = response.request.url.encodedPath
        // Do NOT attempt refresh on login or refresh endpoints to prevent recursive loops
        if (path.contains("api/auth/login") || path.contains("api/auth/refresh") || path.contains("api/public/")) {
            return@Authenticator null
        }

        val currentRefreshToken = sessionPrefs.getRefreshToken()
        if (currentRefreshToken.isNullOrBlank()) {
            Log.w("AttendanceNetworkDiag", "[Authenticator] No refresh token available in preferences. Cannot renew session.")
            return@Authenticator null
        }

        synchronized(sessionRenewalLock) {
            // 1. Check if another concurrent thread already renewed the session while this thread waited
            val latestCookie = sessionPrefs.getSessionCookieHeader()
            val requestCookie = response.request.header("Cookie")
            if (!latestCookie.isNullOrBlank() && latestCookie != requestCookie && !requestCookie.isNullOrBlank()) {
                Log.i("AttendanceNetworkDiag", "[Authenticator] Session already renewed by another thread. Retrying with fresh cookie.")
                return@Authenticator response.request.newBuilder()
                    .header("Cookie", latestCookie)
                    .build()
            }

            // 2. Perform synchronized silent renewal
            Log.i("AttendanceNetworkDiag", "[Authenticator] Triggering synchronized silent renewal via /api/auth/refresh...")
            val success = performSynchronousTokenRefresh(currentRefreshToken)
            if (success) {
                val newCookie = sessionPrefs.getSessionCookieHeader()
                Log.i("AttendanceNetworkDiag", "[Authenticator] Silent renewal SUCCEEDED. Retrying original request: $path")
                val builder = response.request.newBuilder()
                if (!newCookie.isNullOrBlank()) {
                    builder.header("Cookie", newCookie)
                }
                return@Authenticator builder.build()
            } else {
                Log.w("AttendanceNetworkDiag", "[Authenticator] Silent renewal failed. Returning null.")
                return@Authenticator null
            }
        }
    }

    /**
     * Performs a direct, synchronous call to /api/auth/refresh without recursive Authenticator attachment.
     * Updates session preferences with rotated refresh token and new session cookie on success.
     */
    fun performSynchronousTokenRefresh(token: String): Boolean {
        return try {
            val baseUrl = sessionPrefs.getBaseUrl().trimEnd('/')
            val refreshUrl = "$baseUrl/api/auth/refresh"
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            val jsonBody = JsonObject().apply {
                addProperty("refreshToken", token)
                addProperty("deviceInfo", "Android Teacher App ${Build.MODEL ?: "Device"}")
            }.toString()

            val request = Request.Builder()
                .url(refreshUrl)
                .post(jsonBody.toRequestBody(jsonMediaType))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Refresh-Token", token)
                .build()

            // Dedicated unauthenticated client with 35s timeout for Render cold start
            val directClient = OkHttpClient.Builder()
                .connectTimeout(35, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val refreshResponse = directClient.newCall(request).execute()
            val code = refreshResponse.code
            val bodyString = refreshResponse.body?.string() ?: ""

            if (refreshResponse.isSuccessful) {
                // 1. Capture and persist new session cookie if provided by server
                val setCookieHeaders = refreshResponse.headers("Set-Cookie")
                for (sc in setCookieHeaders) {
                    if (sc.contains("RENAISSANCE_SESSION") || sc.contains("JSESSIONID")) {
                        val fullCookie = sc.split(";").firstOrNull()?.trim() ?: sc.trim()
                        val parts = fullCookie.split("=", limit = 2)
                        val name = parts.getOrNull(0)?.trim() ?: "RENAISSANCE_SESSION"
                        val value = parts.getOrNull(1)?.trim() ?: ""
                        sessionPrefs.saveSessionCookie(fullCookie, name, value)
                        Log.i("AttendanceNetworkDiag", "[Authenticator] Saved new session cookie: $name")
                        break
                    }
                }

                // 2. Parse response body to update rotated refresh token and teacher profile
                try {
                    val root = JsonParser.parseString(bodyString).asJsonObject
                    if (root.has("data") && !root.get("data").isJsonNull) {
                        val data = root.getAsJsonObject("data")
                        if (data.has("refreshToken") && !data.get("refreshToken").isJsonNull) {
                            val rotatedToken = data.get("refreshToken").asString
                            if (!rotatedToken.isNullOrBlank()) {
                                sessionPrefs.saveRefreshToken(rotatedToken)
                                Log.i("AttendanceNetworkDiag", "[Authenticator] Saved rotated refresh token.")
                            }
                        }
                        if (data.has("teacher") && !data.get("teacher").isJsonNull) {
                            val teacher = data.getAsJsonObject("teacher")
                            val name = if (teacher.has("name") && !teacher.get("name").isJsonNull) teacher.get("name").asString else sessionPrefs.getTeacherName()
                            val id = if (teacher.has("id") && !teacher.get("id").isJsonNull) teacher.get("id").asString else sessionPrefs.getTeacherId()
                            val email = if (teacher.has("email") && !teacher.get("email").isJsonNull) teacher.get("email").asString else (sessionPrefs.getTeacherEmail() ?: "")
                            val dept = if (teacher.has("departmentName") && !teacher.get("departmentName").isJsonNull) teacher.get("departmentName").asString
                            else if (teacher.has("department") && !teacher.get("department").isJsonNull) teacher.get("department").asString
                            else sessionPrefs.getDepartment()
                            sessionPrefs.updateTeacherProfile(name, id, email, dept)
                        }
                    }
                } catch (parseEx: Exception) {
                    Log.w("AttendanceNetworkDiag", "[Authenticator] Error parsing refresh response body: ${parseEx.message}")
                }
                true
            } else {
                Log.w("AttendanceNetworkDiag", "[Authenticator] Refresh endpoint returned HTTP $code: $bodyString")
                if (code == 401 || code == 403) {
                    // Truly invalid / revoked / expired refresh token or deactivated account
                    Log.e("AttendanceNetworkDiag", "[Authenticator] Refresh token is genuinely invalid or revoked. Clearing credentials.")
                    cookieStore.clear()
                    sessionPrefs.clearSession()
                }
                // If 500, 503, timeout, etc., do NOT clear session! Return false to allow retry
                false
            }
        } catch (e: Exception) {
            Log.e("AttendanceNetworkDiag", "[Authenticator] Network exception during refresh: ${e.message}")
            // Do NOT clear credentials on network errors
            false
        }
    }

    suspend fun silentRenewSession(): Boolean {
        val token = sessionPrefs.getRefreshToken() ?: return false
        return withContext(Dispatchers.IO) {
            synchronized(sessionRenewalLock) {
                performSynchronousTokenRefresh(token)
            }
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .authenticator(sessionAuthenticator)
        .addInterceptor(authCookieInterceptor)
        .addInterceptor(transientRetryInterceptor)
        .addInterceptor(endpointTimeoutInterceptor)
        .addInterceptor(auditDiagnosticInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var retrofit: Retrofit = buildRetrofit(sessionPrefs.getBaseUrl())

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun updateBaseUrl(newUrl: String) {
        sessionPrefs.setBaseUrl(newUrl)
        retrofit = buildRetrofit(newUrl)
    }

    fun clearSessionCookies() {
        cookieStore.clear()
        sessionPrefs.clearSession()
        Log.i("AttendanceNetworkDiag", "[ApiClient] Session cookies and preferences cleared.")
    }

    fun restoreSession() {
        try {
            val host = sessionPrefs.getBaseUrl().replace("https://", "").replace("http://", "").split("/")[0]
            val testUrl = HttpUrl.Builder().scheme("https").host(host).build()
            cookieJar.loadForRequest(testUrl)
        } catch (_: Exception) {}
    }

    val authApi: AuthApi get() = retrofit.create(AuthApi::class.java)
    val teacherApi: TeacherApi get() = retrofit.create(TeacherApi::class.java)
    val sessionApi: SessionApi get() = retrofit.create(SessionApi::class.java)
    val timetableApi: TimetableApi get() = retrofit.create(TimetableApi::class.java)
    val appVersionApi: AppVersionApi get() = retrofit.create(AppVersionApi::class.java)
    val reportApi: ReportApi get() = retrofit.create(ReportApi::class.java)
}
