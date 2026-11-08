package com.renaissance.teacher.data.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.renaissance.teacher.BuildConfig
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.model.AppVersionInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(val info: AppVersionInfo) : UpdateState
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressText: String
    ) : UpdateState
    object Verifying : UpdateState
    data class ReadyToInstall(val apkFile: File, val info: AppVersionInfo) : UpdateState
    data class UpToDate(val versionName: String, val versionCode: Int) : UpdateState
    data class Error(val message: String, val canRetry: Boolean = true, val targetInfo: AppVersionInfo? = null) : UpdateState
}

class InAppUpdateManager(
    private val context: Context,
    private val apiClient: ApiClient
) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var downloadJob: Job? = null

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppVersionInfo?>(null)
    val availableUpdate: StateFlow<AppVersionInfo?> = _availableUpdate.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _downloadedApkFile = MutableStateFlow<File?>(null)
    val downloadedApkFile: StateFlow<File?> = _downloadedApkFile.asStateFlow()

    companion object {
        private const val TAG = "InAppUpdateManager"
    }

    suspend fun checkForUpdates(): AppVersionInfo? {
        _updateState.value = UpdateState.Checking
        _statusMessage.value = "Checking for application updates..."
        try {
            val response = withContext(Dispatchers.IO) {
                apiClient.appVersionApi.getLatestVersion()
            }
            if (response.isSuccessful && response.body() != null) {
                val info = response.body()!!
                if (info.versionCode > BuildConfig.VERSION_CODE) {
                    _availableUpdate.value = info
                    _statusMessage.value = "New version v${info.versionName} is available."
                    _updateState.value = UpdateState.UpdateAvailable(info)
                    return info
                } else {
                    _availableUpdate.value = null
                    val msg = "App is up to date (v${BuildConfig.VERSION_NAME})."
                    _statusMessage.value = msg
                    _updateState.value = UpdateState.UpToDate(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                }
            } else {
                _availableUpdate.value = null
                val err = "Unable to check updates: server responded HTTP ${response.code()}."
                _statusMessage.value = err
                _updateState.value = UpdateState.Error(err)
            }
        } catch (e: Exception) {
            _availableUpdate.value = null
            val err = "Update check failed: ${e.localizedMessage ?: "Connection error"}"
            _statusMessage.value = err
            _updateState.value = UpdateState.Error(err)
        }
        return null
    }

    fun startDownload(updateInfo: AppVersionInfo) {
        if (_isDownloading.value) {
            Log.w(TAG, "Download already in progress")
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0
            _statusMessage.value = "Initiating APK download for v${updateInfo.versionName}..."
            _updateState.value = UpdateState.Downloading(0, 0L, 0L, "Connecting to update server...")

            val updatesDir = File(context.cacheDir, "apk_updates").apply { mkdirs() }
            val tempFile = File(updatesDir, "download_temp_${System.currentTimeMillis()}.apk")
            val targetFileName = "renaissance-teacher-v${updateInfo.versionName}.apk"
            val finalFile = File(updatesDir, targetFileName)

            try {
                withContext(Dispatchers.IO) {
                    val downloadUrl = updateInfo.downloadUrl ?: "/api/public/app/download"
                    val fullUrl = if (downloadUrl.startsWith("http")) {
                        downloadUrl
                    } else {
                        val base = apiClient.sessionPrefs.getBaseUrl()
                        if (base.endsWith("/") && downloadUrl.startsWith("/")) {
                            base + downloadUrl.substring(1)
                        } else if (!base.endsWith("/") && !downloadUrl.startsWith("/")) {
                            "$base/$downloadUrl"
                        } else {
                            base + downloadUrl
                        }
                    }

                    Log.i(TAG, "Streaming APK from: $fullUrl")
                    val response = apiClient.appVersionApi.downloadApkFromUrl(fullUrl)

                    if (!response.isSuccessful) {
                        throw IOException("Server returned HTTP ${response.code()} (${response.message()})")
                    }

                    val body = response.body() ?: throw IOException("Update server returned empty body")
                    val contentLength = body.contentLength()
                    val inputStream = body.byteStream()

                    if (tempFile.exists()) tempFile.delete()

                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        var lastUiUpdate = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdate > 120 || (contentLength > 0 && totalBytesRead == contentLength)) {
                                lastUiUpdate = now
                                val percent = if (contentLength > 0) {
                                    ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                                } else {
                                    0
                                }
                                val downloadedMb = String.format("%.2f MB", totalBytesRead / (1024f * 1024f))
                                val totalMb = if (contentLength > 0) {
                                    String.format("%.2f MB", contentLength / (1024f * 1024f))
                                } else {
                                    "Unknown size"
                                }
                                val text = if (contentLength > 0) {
                                    "$percent% ($downloadedMb / $totalMb)"
                                } else {
                                    "Downloading: $downloadedMb"
                                }

                                withContext(Dispatchers.Main) {
                                    _downloadProgress.value = percent
                                    _statusMessage.value = "Downloading update: $text"
                                    _updateState.value = UpdateState.Downloading(
                                        progressPercent = percent,
                                        bytesDownloaded = totalBytesRead,
                                        totalBytes = contentLength,
                                        progressText = text
                                    )
                                }
                            }
                        }
                        output.flush()
                    }

                    // --- APK VERIFICATION ---
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Verifying APK integrity & signature..."
                        _updateState.value = UpdateState.Verifying
                    }

                    if (tempFile.length() < 1_000_000L) {
                        throw IOException("Downloaded file is incomplete (${tempFile.length()} bytes)")
                    }

                    // Verify ZIP / APK Magic Bytes
                    tempFile.inputStream().use { stream ->
                        val magic = ByteArray(4)
                        val readCount = stream.read(magic)
                        val isZip = readCount == 4 &&
                                magic[0] == 0x50.toByte() &&
                                magic[1] == 0x4B.toByte() &&
                                magic[2] == 0x03.toByte() &&
                                magic[3] == 0x04.toByte()
                        if (!isZip) {
                            throw IOException("Corrupt download: file does not match APK/ZIP format")
                        }
                    }

                    // Verify Android package manifest with PackageManager
                    val archiveInfo = context.packageManager.getPackageArchiveInfo(
                        tempFile.absolutePath,
                        PackageManager.GET_ACTIVITIES
                    ) ?: throw IOException("Android package manager could not parse APK manifest")

                    if (archiveInfo.packageName != context.packageName) {
                        throw IOException("Package mismatch: expected ${context.packageName}, got ${archiveInfo.packageName}")
                    }

                    // Move verified APK to final installation destination
                    if (finalFile.exists()) finalFile.delete()
                    if (!tempFile.renameTo(finalFile)) {
                        tempFile.copyTo(finalFile, overwrite = true)
                        tempFile.delete()
                    }
                }

                _isDownloading.value = false
                _downloadProgress.value = 100
                _downloadedApkFile.value = finalFile
                val successMsg = "v${updateInfo.versionName} downloaded & verified (${finalFile.length() / (1024 * 1024)} MB). Ready to install."
                _statusMessage.value = successMsg
                _updateState.value = UpdateState.ReadyToInstall(finalFile, updateInfo)

                // Prompt immediate install
                triggerPackageInstall(finalFile)

            } catch (e: Exception) {
                _isDownloading.value = false
                _downloadProgress.value = 0
                if (tempFile.exists()) tempFile.delete()

                if (e is CancellationException) {
                    _statusMessage.value = "Download cancelled by user."
                    _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                } else {
                    val errMsg = "Download failed: ${e.localizedMessage ?: e.message ?: "Network error"}"
                    Log.e(TAG, "APK download failed", e)
                    _statusMessage.value = errMsg
                    _updateState.value = UpdateState.Error(errMsg, canRetry = true, targetInfo = updateInfo)
                }
            }
        }
    }

    fun cancelDownload() {
        if (_isDownloading.value) {
            downloadJob?.cancel()
            _isDownloading.value = false
            _downloadProgress.value = 0
            _statusMessage.value = "Download cancelled."
        }
    }

    fun triggerPackageInstall(apkFile: File) {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            _statusMessage.value = "Error: APK file not found on device storage."
            _updateState.value = UpdateState.Error("APK file missing on device storage.")
            return
        }

        val authority = "${context.packageName}.provider"
        val apkUri = try {
            FileProvider.getUriForFile(context, authority, apkFile)
        } catch (e: Exception) {
            val err = "FileProvider failed to generate content URI: ${e.message}"
            Log.e(TAG, err, e)
            _statusMessage.value = err
            _updateState.value = UpdateState.Error(err)
            return
        }

        Log.i(TAG, "Launching Android Package Installer with URI: $apkUri")

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            // On Android 8.0+, check if unknown app sources are allowed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Log.w(TAG, "Install permission not yet granted; opening system settings...")
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    _statusMessage.value = "Please allow installation from Renaissance Teacher in Settings, then tap Install."
                    return
                }
            }

            context.startActivity(installIntent)
            _statusMessage.value = "Package installer launched."
        } catch (e: Exception) {
            val err = "Failed to launch package installer: ${e.message}"
            Log.e(TAG, err, e)
            _statusMessage.value = err
            _updateState.value = UpdateState.Error(err)
        }
    }

    fun openDownloadInBrowser(updateInfo: AppVersionInfo) {
        val downloadUrl = updateInfo.downloadUrl ?: "/api/public/app/download"
        val fullUrl = if (downloadUrl.startsWith("http")) {
            downloadUrl
        } else {
            val base = apiClient.sessionPrefs.getBaseUrl()
            if (base.endsWith("/") && downloadUrl.startsWith("/")) {
                base + downloadUrl.substring(1)
            } else if (!base.endsWith("/") && !downloadUrl.startsWith("/")) {
                "$base/$downloadUrl"
            } else {
                base + downloadUrl
            }
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch browser: ${e.message}")
        }
    }
}
