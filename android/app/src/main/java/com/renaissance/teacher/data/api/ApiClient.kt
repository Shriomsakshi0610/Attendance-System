package com.renaissance.teacher.data.api

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import com.renaissance.teacher.BuildConfig
import com.renaissance.teacher.data.security.SessionPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {

    private val sessionPrefs = SessionPreferences(context)
    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
            // Also store any session cookie
            for (cookie in cookies) {
                if (cookie.name == "JSESSIONID" || cookie.name == "RENAISSANCE_SESSION") {
                    val fullCookie = "${cookie.name}=${cookie.value}"
                    val currentName = sessionPrefs.getTeacherName()
                    val currentId = sessionPrefs.getTeacherId()
                    val currentEmail = sessionPrefs.getTeacherEmail() ?: ""
                    val currentDept = sessionPrefs.getDepartment()
                    sessionPrefs.saveSession(fullCookie, currentName, currentId, currentEmail, currentDept)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = cookieStore[url.host]?.toMutableList() ?: mutableListOf()
            // If empty in memory, attempt from encrypted prefs
            if (list.isEmpty()) {
                val savedCookie = sessionPrefs.getSessionCookie()
                if (!savedCookie.isNullOrBlank() && savedCookie.contains("=")) {
                    val parts = savedCookie.split("=", limit = 2)
                    try {
                        val cookie = Cookie.Builder()
                            .name(parts[0].trim())
                            .value(parts[1].trim())
                            .domain(url.host)
                            .path("/")
                            .build()
                        list.add(cookie)
                    } catch (_: Exception) {}
                }
            }
            return list
        }
    }

    /**
     * Interceptor providing limited exponential backoff retry for transient network disruptions.
     * Retries idempotent GET requests on SocketTimeoutException, ConnectException, and 503 Gateway errors.
     * Strictly ignores 400, 401, 403, 404, and 500 errors.
     */
    private val transientRetryInterceptor = Interceptor { chain ->
        val request = chain.request()
        val isGet = request.method.equals("GET", ignoreCase = true)
        val maxRetries = if (isGet) 2 else 0
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)
                if (response.code == 503 && isGet && attempt < maxRetries) {
                    response.close()
                    attempt++
                    val backoff = if (attempt == 1) 600L else 1500L
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
                    val backoff = if (attempt == 1) 600L else 1500L
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
     * Endpoints subject to server awakening or heavy initialization (auth, health, reports, session-activation)
     * are granted up to 45 seconds to avoid socket aborts before the JVM finishes booting.
     * Standard transactional APIs retain fast, responsive timeouts (15s connect, 20s read).
     */
    private val endpointTimeoutInterceptor = Interceptor { chain ->
        val request = chain.request()
        val path = request.url.encodedPath

        val isColdStartSensitive = path.contains("api/auth") ||
                path.contains("health") ||
                path.contains("api/reports") ||
                path.contains("api/sessions/active") ||
                path.contains("api/public/app")

        if (isColdStartSensitive) {
            chain.withConnectTimeout(45, TimeUnit.SECONDS)
                .withReadTimeout(45, TimeUnit.SECONDS)
                .proceed(request)
        } else {
            chain.withConnectTimeout(15, TimeUnit.SECONDS)
                .withReadTimeout(20, TimeUnit.SECONDS)
                .proceed(request)
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
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

    val authApi: AuthApi get() = retrofit.create(AuthApi::class.java)
    val teacherApi: TeacherApi get() = retrofit.create(TeacherApi::class.java)
    val sessionApi: SessionApi get() = retrofit.create(SessionApi::class.java)
    val timetableApi: TimetableApi get() = retrofit.create(TimetableApi::class.java)
    val appVersionApi: AppVersionApi get() = retrofit.create(AppVersionApi::class.java)
    val reportApi: ReportApi get() = retrofit.create(ReportApi::class.java)
}
