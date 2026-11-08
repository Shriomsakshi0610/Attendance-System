package com.renaissance.teacher.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * High-Reliability Persistent Session Store for Renaissance Teacher App.
 *
 * Architecture:
 * 1. Singleton pattern ensures a single, globally synchronized preference cache across
 *    Application, ApiClient, ViewModels, and UpdateManager.
 * 2. Primary storage uses atomic synchronous commits (commit() instead of apply())
 *    to prevent data loss during process death or app swipe from Recent Apps.
 * 3. Graceful migration: On cold startup, automatically checks legacy EncryptedSharedPreferences
 *    and fallback preferences to ensure existing logged-in teachers are never logged out.
 * 4. Resilient fallback: Immune to KeyStore crashes, AEADBadTagExceptions, and OEM hardware crypto timeouts.
 */
class SessionPreferences private constructor(context: Context) {

    private val appContext = context.applicationContext

    // Primary rock-solid private preferences (UID-sandboxed by Android OS)
    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    init {
        migrateLegacySessionIfNeeded()
    }

    companion object {
        private const val TAG = "SessionPreferences"
        private const val PREFS_NAME = "renaissance_teacher_session"
        private const val LEGACY_ENCRYPTED_PREFS = "renaissance_secure_prefs"
        private const val LEGACY_FALLBACK_PREFS = "renaissance_secure_prefs_fallback"

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

        @Volatile
        private var instance: SessionPreferences? = null

        @JvmStatic
        fun getInstance(context: Context): SessionPreferences {
            return instance ?: synchronized(this) {
                instance ?: SessionPreferences(context).also { instance = it }
            }
        }
    }

    /**
     * Backward-compatible public constructor delegating to the thread-safe singleton.
     */
    constructor(context: Context, @Suppress("UNUSED_PARAMETER") dummy: Boolean = false) : this(context) {
        synchronized(SessionPreferences::class.java) {
            if (instance == null) {
                instance = this
            }
        }
    }

    /**
     * Migrates saved credentials from legacy stores (EncryptedSharedPreferences or fallback)
     * if the primary store is uninitialized.
     */
    private fun migrateLegacySessionIfNeeded() {
        if (prefs.contains(KEY_SESSION_COOKIE_VALUE) && !prefs.getString(KEY_SESSION_COOKIE_VALUE, null).isNullOrBlank()) {
            return // Primary store already contains session data
        }

        // Try migrating from legacy fallback prefs first
        try {
            val fallbackPrefs = appContext.getSharedPreferences(LEGACY_FALLBACK_PREFS, Context.MODE_PRIVATE)
            val fallbackCookie = fallbackPrefs.getString(KEY_SESSION_COOKIE_VALUE, null)
                ?: fallbackPrefs.getString(KEY_SESSION_COOKIE, null)
            if (!fallbackCookie.isNullOrBlank()) {
                Log.i(TAG, "Migrating session from legacy fallback preferences...")
                copyPrefs(fallbackPrefs, prefs)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legacy fallback migration check failed: ${e.message}")
        }

        // Try migrating from legacy EncryptedSharedPreferences if accessible
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encPrefs = EncryptedSharedPreferences.create(
                appContext,
                LEGACY_ENCRYPTED_PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val encCookie = encPrefs.getString(KEY_SESSION_COOKIE_VALUE, null)
                ?: encPrefs.getString(KEY_SESSION_COOKIE, null)
            if (!encCookie.isNullOrBlank()) {
                Log.i(TAG, "Migrating session from legacy EncryptedSharedPreferences...")
                copyPrefs(encPrefs, prefs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legacy encrypted preferences not readable or uninitialized: ${e.message}")
        }
    }

    private fun copyPrefs(src: SharedPreferences, dst: SharedPreferences) {
        val editor = dst.edit()
        val all = src.all
        for ((k, v) in all) {
            when (v) {
                is String -> editor.putString(k, v)
                is Boolean -> editor.putBoolean(k, v)
                is Int -> editor.putInt(k, v)
                is Long -> editor.putLong(k, v)
                is Float -> editor.putFloat(k, v)
            }
        }
        editor.commit()
    }

    private fun parseCookieTokens(rawCookie: String): Triple<String, String, String> {
        val cleanToken = rawCookie.split(";").firstOrNull()?.trim() ?: rawCookie.trim()
        val parts = cleanToken.split("=", limit = 2)
        val name = if (parts.isNotEmpty()) parts[0].trim() else "RENAISSANCE_SESSION"
        val value = if (parts.size > 1) parts[1].trim() else ""
        return Triple(name, value, "$name=$value")
    }

    @Synchronized
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
            .commit() // Synchronous atomic flush to disk
        Log.i(TAG, "[Session Saved] Cookie: $cName (len=${cVal.length}), Teacher: $name ($email)")
    }

    @Synchronized
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
            .commit() // Synchronous atomic flush to disk
        Log.i(TAG, "[Session Cookie Saved] Cookie: $cName (len=${cVal.length})")
    }

    @Synchronized
    fun updateTeacherProfile(name: String, teacherId: String?, email: String, department: String?) {
        prefs.edit()
            .putString(KEY_TEACHER_NAME, name)
            .putString(KEY_TEACHER_ID, teacherId)
            .putString(KEY_TEACHER_EMAIL, email)
            .putString(KEY_DEPARTMENT, department)
            .commit()
    }

    fun getSessionCookie(): String? = prefs.getString(KEY_SESSION_COOKIE, null)
    fun getSessionCookieName(): String = prefs.getString(KEY_SESSION_COOKIE_NAME, "RENAISSANCE_SESSION") ?: "RENAISSANCE_SESSION"
    fun getSessionCookieValue(): String? = prefs.getString(KEY_SESSION_COOKIE_VALUE, null)

    fun getSessionCookieHeader(): String? {
        val v = getSessionCookieValue() ?: return null
        val n = getSessionCookieName()
        return "$n=$v"
    }

    fun isLoggedIn(): Boolean {
        val hasFlag = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val hasCookie = !getSessionCookieValue().isNullOrBlank()
        return hasFlag && hasCookie
    }

    fun getTeacherName(): String = prefs.getString(KEY_TEACHER_NAME, "Faculty") ?: "Faculty"
    fun getTeacherId(): String? = prefs.getString(KEY_TEACHER_ID, null)
    fun getTeacherEmail(): String? = prefs.getString(KEY_TEACHER_EMAIL, null)
    fun getDepartment(): String = prefs.getString(KEY_DEPARTMENT, "Department") ?: "Department"

    fun getBaseUrl(): String = prefs.getString(KEY_CUSTOM_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL

    @Synchronized
    fun setBaseUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_CUSTOM_BASE_URL, formatted).commit()
    }

    @Synchronized
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
            .commit()
        Log.i(TAG, "[Session Cleared] User logged out.")
    }
}
