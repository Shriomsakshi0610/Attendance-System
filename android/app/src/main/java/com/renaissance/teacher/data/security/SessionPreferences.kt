package com.renaissance.teacher.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionPreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "renaissance_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_SESSION_COOKIE = "session_cookie"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_TEACHER_NAME = "teacher_name"
        private const val KEY_TEACHER_ID = "teacher_id"
        private const val KEY_TEACHER_EMAIL = "teacher_email"
        private const val KEY_DEPARTMENT = "teacher_department"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        const val DEFAULT_BASE_URL = "https://attendance-system-gfr7.onrender.com/"
    }

    fun saveSession(cookie: String, name: String, teacherId: String?, email: String, department: String?) {
        prefs.edit()
            .putString(KEY_SESSION_COOKIE, cookie)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_TEACHER_NAME, name)
            .putString(KEY_TEACHER_ID, teacherId)
            .putString(KEY_TEACHER_EMAIL, email)
            .putString(KEY_DEPARTMENT, department)
            .apply()
    }

    fun getSessionCookie(): String? = prefs.getString(KEY_SESSION_COOKIE, null)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !getSessionCookie().isNullOrEmpty()

    fun getTeacherName(): String = prefs.getString(KEY_TEACHER_NAME, "Faculty") ?: "Faculty"
    fun getTeacherId(): String? = prefs.getString(KEY_TEACHER_ID, null)
    fun getTeacherEmail(): String? = prefs.getString(KEY_TEACHER_EMAIL, null)
    fun getDepartment(): String = prefs.getString(KEY_DEPARTMENT, "Department") ?: "Department"

    fun getBaseUrl(): String = prefs.getString(KEY_CUSTOM_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL

    fun setBaseUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_CUSTOM_BASE_URL, formatted).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_SESSION_COOKIE)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_TEACHER_NAME)
            .remove(KEY_TEACHER_ID)
            .remove(KEY_TEACHER_EMAIL)
            .remove(KEY_DEPARTMENT)
            .apply()
    }
}
