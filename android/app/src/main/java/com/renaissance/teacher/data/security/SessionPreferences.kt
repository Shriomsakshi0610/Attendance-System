package com.renaissance.teacher.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionPreferences(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "renaissance_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("SessionPreferences", "Failed to initialize EncryptedSharedPreferences, falling back to private prefs: ${e.message}")
        context.getSharedPreferences("renaissance_secure_prefs_fallback", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_SESSION_COOKIE = "session_cookie"
        private const val KEY_SESSION_COOKIE_NAME = "session_cookie_name"
        private const val KEY_SESSION_COOKIE_VALUE = "session_cookie_value"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_TEACHER_NAME = "teacher_name"
        private const val KEY_TEACHER_ID = "teacher_id"
        private const val KEY_TEACHER_EMAIL = "teacher_email"
        private const val KEY_DEPARTMENT = "teacher_department"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        const val DEFAULT_BASE_URL = "https://attendance-system-gfr7.onrender.com/"
    }

    private fun parseCookieTokens(rawCookie: String): Triple<String, String, String> {
        val cleanToken = rawCookie.split(";").firstOrNull()?.trim() ?: rawCookie.trim()
        val parts = cleanToken.split("=", limit = 2)
        val name = if (parts.isNotEmpty()) parts[0].trim() else "RENAISSANCE_SESSION"
        val value = if (parts.size > 1) parts[1].trim() else ""
        return Triple(name, value, "$name=$value")
    }

    fun saveSession(cookie: String, name: String, teacherId: String?, email: String, department: String?) {
        val (cName, cVal, cleanHeader) = parseCookieTokens(cookie)
        prefs.edit()
            .putString(KEY_SESSION_COOKIE, cleanHeader)
            .putString(KEY_SESSION_COOKIE_NAME, cName)
            .putString(KEY_SESSION_COOKIE_VALUE, cVal)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_TEACHER_NAME, name)
            .putString(KEY_TEACHER_ID, teacherId)
            .putString(KEY_TEACHER_EMAIL, email)
            .putString(KEY_DEPARTMENT, department)
            .apply()
    }

    fun saveSessionCookie(rawCookie: String, name: String? = null, value: String? = null) {
        val (cName, cVal, cleanHeader) = if (name != null && value != null) {
            Triple(name, value, "$name=$value")
        } else {
            parseCookieTokens(rawCookie)
        }
        prefs.edit()
            .putString(KEY_SESSION_COOKIE, cleanHeader)
            .putString(KEY_SESSION_COOKIE_NAME, cName)
            .putString(KEY_SESSION_COOKIE_VALUE, cVal)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun updateTeacherProfile(name: String, teacherId: String?, email: String, department: String?) {
        prefs.edit()
            .putString(KEY_TEACHER_NAME, name)
            .putString(KEY_TEACHER_ID, teacherId)
            .putString(KEY_TEACHER_EMAIL, email)
            .putString(KEY_DEPARTMENT, department)
            .apply()
    }

    fun getSessionCookie(): String? = prefs.getString(KEY_SESSION_COOKIE, null)
    fun getSessionCookieName(): String = prefs.getString(KEY_SESSION_COOKIE_NAME, "RENAISSANCE_SESSION") ?: "RENAISSANCE_SESSION"
    fun getSessionCookieValue(): String? = prefs.getString(KEY_SESSION_COOKIE_VALUE, null)
    fun getSessionCookieHeader(): String? {
        val v = getSessionCookieValue() ?: return null
        val n = getSessionCookieName()
        return "$n=$v"
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !getSessionCookieValue().isNullOrBlank()

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
            .remove(KEY_SESSION_COOKIE_NAME)
            .remove(KEY_SESSION_COOKIE_VALUE)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_TEACHER_NAME)
            .remove(KEY_TEACHER_ID)
            .remove(KEY_TEACHER_EMAIL)
            .remove(KEY_DEPARTMENT)
            .apply()
    }
}
