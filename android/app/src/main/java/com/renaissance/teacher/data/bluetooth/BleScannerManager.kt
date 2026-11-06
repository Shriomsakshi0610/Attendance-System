package com.renaissance.teacher.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

data class DetectedBeacon(
    val uuid: String,
    val rssi: Int,
    val timestamp: Long
)

class BleScannerManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _detectedBeacons = MutableStateFlow<Map<String, DetectedBeacon>>(emptyMap())
    val detectedBeacons: StateFlow<Map<String, DetectedBeacon>> = _detectedBeacons.asStateFlow()

    private var activeCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    fun startScan(targetUuid: String? = null) {
        stopScan()
        val leScanner = scanner ?: return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = mutableListOf<ScanFilter>()
        if (!targetUuid.isNullOrBlank()) {
            val pUuid = try {
                ParcelUuid(UUID.fromString(targetUuid))
            } catch (_: Exception) {
                ParcelUuid(UUID.nameUUIDFromBytes(targetUuid.toByteArray(StandardCharsets.UTF_8)))
            }
            filters.add(ScanFilter.Builder().setServiceUuid(pUuid).build())
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result ?: return
                val uuids = result.scanRecord?.serviceUuids ?: return
                for (pUuid in uuids) {
                    val uuidStr = pUuid.uuid.toString()
                    val beacon = DetectedBeacon(uuidStr, result.rssi, System.currentTimeMillis())
                    val updated = _detectedBeacons.value.toMutableMap()
                    updated[uuidStr] = beacon
                    _detectedBeacons.value = updated
                }
            }
        }

        activeCallback = callback
        try {
            leScanner.startScan(filters, settings, callback)
            _isScanning.value = true
        } catch (_: Exception) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val cb = activeCallback
        if (cb != null) {
            try {
                scanner?.stopScan(cb)
            } catch (_: Exception) {}
            activeCallback = null
        }
        _isScanning.value = false
    }
}
