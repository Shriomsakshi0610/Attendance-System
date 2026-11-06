package com.renaissance.teacher.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

class BleAdvertiserManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val advertiser: BluetoothLeAdvertiser? get() = bluetoothAdapter?.bluetoothLeAdvertiser

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _currentUuid = MutableStateFlow<String?>(null)
    val currentUuid: StateFlow<String?> = _currentUuid.asStateFlow()

    private val _advertisingError = MutableStateFlow<String?>(null)
    val advertisingError: StateFlow<String?> = _advertisingError.asStateFlow()

    private var activeCallback: AdvertiseCallback? = null

    fun isBleSupported(): Boolean =
        bluetoothAdapter != null && bluetoothAdapter?.isMultipleAdvertisementSupported == true

    fun isBluetoothEnabled(): Boolean =
        bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startAdvertising(sessionUuid: String, onStarted: () -> Unit = {}, onError: (String) -> Unit = {}) {
        stopAdvertising()

        if (!isBleSupported()) {
            val err = "BLE Multiple Advertisement is not supported on this device."
            _advertisingError.value = err
            onError(err)
            return
        }

        if (!isBluetoothEnabled()) {
            val err = "Bluetooth is disabled. Please enable Bluetooth."
            _advertisingError.value = err
            onError(err)
            return
        }

        val leAdvertiser = advertiser
        if (leAdvertiser == null) {
            val err = "Bluetooth LE Advertiser is unavailable."
            _advertisingError.value = err
            onError(err)
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0) // Advertise indefinitely until stopped
            .build()

        val parsedUuid: UUID = try {
            UUID.fromString(sessionUuid)
        } catch (_: Exception) {
            UUID.nameUUIDFromBytes(sessionUuid.toByteArray(StandardCharsets.UTF_8))
        }

        val pUuid = ParcelUuid(parsedUuid)

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(true)
            .addServiceUuid(pUuid)
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                _isAdvertising.value = true
                _currentUuid.value = sessionUuid
                _advertisingError.value = null
                onStarted()
            }

            override fun onStartFailure(errorCode: Int) {
                _isAdvertising.value = false
                _currentUuid.value = null
                val msg = when (errorCode) {
                    ADVERTISE_FAILED_ALREADY_STARTED -> "BLE advertising already started."
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "BLE advertising data too large."
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "BLE advertising feature unsupported."
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "BLE internal error."
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many BLE advertisers active."
                    else -> "BLE advertising failed with error code $errorCode."
                }
                _advertisingError.value = msg
                onError(msg)
            }
        }

        activeCallback = callback
        try {
            leAdvertiser.startAdvertising(settings, data, callback)
        } catch (e: Exception) {
            _advertisingError.value = e.message
            onError(e.message ?: "Failed to start BLE advertising")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        val cb = activeCallback
        if (cb != null) {
            try {
                advertiser?.stopAdvertising(cb)
            } catch (_: Exception) {}
            activeCallback = null
        }
        _isAdvertising.value = false
        _currentUuid.value = null
        _advertisingError.value = null
    }
}
