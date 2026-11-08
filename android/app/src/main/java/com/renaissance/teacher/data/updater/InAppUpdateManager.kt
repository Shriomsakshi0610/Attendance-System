package com.renaissance.teacher.data.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.renaissance.teacher.BuildConfig
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.model.AppVersionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class InAppUpdateManager(
    private val context: Context,
    private val apiClient: ApiClient
) {

    private val _availableUpdate = MutableStateFlow<AppVersionInfo?>(null)
    val availableUpdate: StateFlow<AppVersionInfo?> = _availableUpdate.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var activeDownloadId: Long = -1L

    suspend fun checkForUpdates(): AppVersionInfo? {
        try {
            val response = apiClient.appVersionApi.getLatestVersion()
            if (response.isSuccessful && response.body() != null) {
                val info = response.body()!!
                if (info.versionCode > BuildConfig.VERSION_CODE) {
                    _availableUpdate.value = info
                    _statusMessage.value = "New version v${info.versionName} is available."
                    return info
                } else {
                    _availableUpdate.value = null
                    _statusMessage.value = "App is already up to date (v${BuildConfig.VERSION_NAME})."
                }
            } else {
                _availableUpdate.value = null
                _statusMessage.value = "Unable to check updates: server responded HTTP ${response.code()}."
            }
        } catch (e: Exception) {
            _availableUpdate.value = null
            _statusMessage.value = "Network error: Unable to reach update server (${e.localizedMessage ?: "Connection failed"})."
        }
        return null
    }

    fun startDownload(updateInfo: AppVersionInfo) {
        if (updateInfo.downloadUrl.isNullOrBlank()) {
            _statusMessage.value = "Update download error: APK download URL is unavailable."
            return
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            _statusMessage.value = "Update download error: DownloadManager service is unavailable."
            return
        }

        val baseUrl = apiClient.run {
            // resolve absolute URL
            val base = apiClient.sessionPrefs.getBaseUrl()
            if (updateInfo.downloadUrl.startsWith("http")) updateInfo.downloadUrl
            else if (base.endsWith("/") && updateInfo.downloadUrl.startsWith("/")) base + updateInfo.downloadUrl.substring(1)
            else base + updateInfo.downloadUrl
        }

        val uri = try {
            Uri.parse(baseUrl)
        } catch (e: Exception) {
            _statusMessage.value = "Update download error: invalid download URL '$baseUrl'."
            return
        }

        val fileName = "renaissance-teacher-v${updateInfo.versionName}.apk"

        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request = try {
            DownloadManager.Request(uri)
                .setTitle("Renaissance Teacher Update")
                .setDescription("Downloading v${updateInfo.versionName}...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setMimeType("application/vnd.android.package-archive")
        } catch (e: Exception) {
            _statusMessage.value = "Update download error: ${e.message}"
            return
        }

        try {
            activeDownloadId = downloadManager.enqueue(request)
            _isDownloading.value = true
            _statusMessage.value = "Downloading update v${updateInfo.versionName}..."
        } catch (e: Exception) {
            _isDownloading.value = false
            _statusMessage.value = "Failed to start download: ${e.message}"
            return
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == activeDownloadId) {
                    _isDownloading.value = false
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}

                    // Query download status for errors
                    var downloadSuccessful = false
                    var errorReason = "Download failed"
                    val query = DownloadManager.Query().setFilterById(activeDownloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = if (statusIdx != -1) cursor.getInt(statusIdx) else -1
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloadSuccessful = true
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = if (reasonIdx != -1) cursor.getInt(reasonIdx) else -1
                            errorReason = "Download failed with error code $reason"
                        }
                        cursor.close()
                    }

                    if (downloadSuccessful && destinationFile.exists() && destinationFile.length() > 0) {
                        _statusMessage.value = "Download finished (${destinationFile.length() / 1024} KB). Ready to install."
                        triggerPackageInstall(destinationFile)
                    } else {
                        _statusMessage.value = "Update download error: $errorReason. Please check network and retry."
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    fun triggerPackageInstall(apkFile: File) {
        if (!apkFile.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }
}
