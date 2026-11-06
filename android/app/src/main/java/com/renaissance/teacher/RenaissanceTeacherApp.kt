package com.renaissance.teacher

import android.app.Application
import com.renaissance.teacher.data.api.ApiClient
import com.renaissance.teacher.data.bluetooth.BleAdvertiserManager
import com.renaissance.teacher.data.bluetooth.BleScannerManager
import com.renaissance.teacher.data.location.LocationService
import com.renaissance.teacher.data.security.SessionPreferences
import com.renaissance.teacher.data.updater.InAppUpdateManager

class RenaissanceTeacherApp : Application() {

    lateinit var sessionPreferences: SessionPreferences
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var locationService: LocationService
        private set
    lateinit var bleAdvertiser: BleAdvertiserManager
        private set
    lateinit var bleScanner: BleScannerManager
        private set
    lateinit var updateManager: InAppUpdateManager
        private set

    override fun onCreate() {
        super.onCreate()

        sessionPreferences = SessionPreferences(this)
        apiClient = ApiClient(this)
        locationService = LocationService(this)
        bleAdvertiser = BleAdvertiserManager(this)
        bleScanner = BleScannerManager(this)
        updateManager = InAppUpdateManager(this, apiClient)
    }
}
