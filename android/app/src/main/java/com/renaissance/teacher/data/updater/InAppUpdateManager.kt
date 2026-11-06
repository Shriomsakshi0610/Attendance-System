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
                    return info
                } else {
                    _availableUpdate.value = null
                    _statusMessage.value = "App is already up to date (v${BuildConfig.VERSION_NAME})."
                }
            } else {
                _statusMessage.value = "Unable to check updates: server responded ${response.code()}."
            }
        } catch (e: Exception) {
            _statusMessage.value = "Failed to check for updates: ${e.message}"
        }
        return null
    }

    fun startDownload(updateInfo: AppVersionInfo) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            _statusMessage.value = "DownloadManager not available."
            return
        }

        val baseUrl = apiClient.run {
            // resolve absolute URL
            val base = com.renaissance.teacher.data.security.SessionPreferences(context).getBaseUrl()
            if (updateInfo.downloadUrl.startsWith("http")) updateInfo.downloadUrl
            else if (base.endsWith("/") && updateInfo.downloadUrl.startsWith("/")) base + updateInfo.downloadUrl.substring(1)
            else base + updateInfo.downloadUrl
        }

        val uri = Uri.parse(baseUrl)
        val fileName = "renaissance-teacher-v${updateInfo.versionName}.apk"

        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request = DownloadManager.Request(uri)
            .setTitle("Renaissance Teacher Update")
            .setDescription("Downloading v${updateInfo.versionName}...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setMimeType("application/vnd.android.package-archive")

        activeDownloadId = downloadManager.enqueue(request)
        _isDownloading.value = true
        _statusMessage.value = "Downloading update..."

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == activeDownloadId) {
                    _isDownloading.value = false
                    _statusMessage.value = "Download finished. Ready to install."
                    triggerPackageInstall(destinationFile)
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}
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
