package com.renaissance.teacher.data.api

import android.content.Context
import com.renaissance.teacher.data.security.SessionPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
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
