package pl.edu.pjwstk.engineeringthesis.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.edu.pjwstk.engineeringthesis.data.repository.GsrSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.HearthRateSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.SpO2SampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.TempSampleRepository
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.HearthRateSample
import pl.edu.pjwstk.engineeringthesis.model.SpO2Sample
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import pl.edu.pjwstk.engineeringthesis.viewmodel.Band
import pl.edu.pjwstk.engineeringthesis.viewmodel.ConnectedDevice
import pl.edu.pjwstk.engineeringthesis.viewmodel.ScanUiState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleConnectionManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val adapter: BluetoothAdapter,
    private val gsrRepo: GsrSampleRepository,
    private val tempRepo: TempSampleRepository,
    private val hrRepo: HearthRateSampleRepository,
    private val spo2Repo: SpO2SampleRepository,
    private val profileRepo: ProfileRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState

    private val _bands = MutableStateFlow<List<Band>>(emptyList())
    val bands: StateFlow<List<Band>> = _bands

    private var scanJob: Job? = null
    private var reconnectJob: Job? = null
    @Volatile private var gotConnection = false
    @Volatile private var isAppVisible = false
    @Volatile private var isConnectScreenVisible = false

    private val prefs by lazy {
        ctx.getSharedPreferences("ble_band_prefs", Context.MODE_PRIVATE)
    }

    private val disconnectTimeoutMs = 3 * 60 * 1000L

    private val _connectedDevice = MutableStateFlow(loadConnectedDevice())
    val connectedDevice: StateFlow<ConnectedDevice?> = _connectedDevice

    private var preferredAddress: String? =
        prefs.getString(KEY_LAST_BAND_ADDR, null)
    private var reconnectOnNextOpen: Boolean =
        prefs.getBoolean(KEY_RECONNECT_ON_NEXT_OPEN, false) ||
            prefs.getBoolean(KEY_CONNECTED_IS_CONNECTED, false)

    private val client = BleUartClient(
        ctx,
        adapter,
        UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
        UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    ).apply {
        setDeviceNameFilter("ESP32-TEMP")
        setPushTimeOnConnect(true)
        setListener(object : BleUartClient.Listener {
            override fun onStatus(s: String) {
                if (s.startsWith("Scanning")) _scanState.value = ScanUiState.Scanning
            }

            override fun onDeviceFound(address: String, name: String?, rssi: Int) {
                val updated = _bands.value.toMutableList()
                val idx = updated.indexOfFirst { it.address == address }
                if (idx >= 0) {
                    updated[idx] = updated[idx].copy(name = name, rssi = rssi)
                } else {
                    updated += Band(address, name, rssi)
                }
                _bands.value = updated.sortedByDescending { it.rssi }

                val connected = _connectedDevice.value
                if (connected != null && connected.address == address && !name.isNullOrEmpty()) {
                    if (connected.name != name) {
                        updateConnectedDevice { it.copy(name = name) }
                    }
                }

                val preferred = preferredAddress
                if (!gotConnection && preferred != null && preferred == address) {
                    try {
                        connectTo(address)
                    } catch (_: SecurityException) {
                    }
                }
            }

            override fun onConnected(address: String, mtu: Int) {
                gotConnection = true
                reconnectJob?.cancel()
                _scanState.value = ScanUiState.Connected
                savePreferredAddress(address)
                val resolvedName = resolveBandName(address)
                val existing = _connectedDevice.value
                if (existing != null && existing.address == address && existing.disconnectPending) {
                    removeBandFromScanResults(address)
                    disconnectCurrent()
                    clearConnectedDevice(clearPreferred = true)
                    return
                }
                markConnected(address, resolvedName)
                removeBandFromScanResults(address)
            }

            override fun onDisconnected() {
                val now = System.currentTimeMillis()
                updateConnectedDevice {
                    it.copy(
                        isConnected = false,
                        sleepStartedAt = if (it.disconnectPending) null else now
                    )
                }
                _scanState.value = ScanUiState.Idle
                val device = _connectedDevice.value
                if (
                    shouldMaintainConnection(device) &&
                    reconnectJob?.isActive != true
                ) {
                    startReconnectSequence(
                        attempts = Int.MAX_VALUE,
                        timeoutMs = 20_000L,
                        pauseMs = 2_000L
                    )
                }
            }

            override fun onPacket(p: Packet) {
                scope.launch(Dispatchers.IO) {
                    val userId = resolveTargetUserId()
                    val epochMillis = normalizeEpochMillis(p.epoch)
                    try {
                        p.temps.forEach { value ->
                            tempRepo.upsert(
                                TempSample(id = 0, userId = userId, epoch = epochMillis, temperature = value)
                            )
                        }
                        p.gsr.forEach { value ->
                            gsrRepo.upsert(
                                GsrSample(id = 0, userId = userId, epoch = epochMillis, gsr = value)
                            )
                        }
                        p.hearthRate.forEach { value ->
                            hrRepo.upsert(
                                HearthRateSample(id = 0, userId = userId, epoch = epochMillis, hearthRate = value)
                            )
                        }
                        p.spo2.forEach { value ->
                            spo2Repo.upsert(
                                SpO2Sample(id = 0, userId = userId, epoch = epochMillis, spo2 = value.toInt())
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("BleConnectionManager", "Failed to insert samples into the database", e)
                    }
                }
            }

            override fun onError(msg: String, t: Throwable?) {
                _scanState.value = ScanUiState.Idle
            }
        })
    }

    init {
        startDisconnectCleanup()
    }

    fun startAfterPermissionsGranted(timeoutMs: Long = 8_000L) {
        val needed: List<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        val missing = needed.filter {
            ActivityCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) return

        if (_connectedDevice.value == null) {
            startScanWithTimeout(timeoutMs)
        } else if (reconnectOnNextOpen) {
            ensureForegroundServiceRunning()
            setReconnectOnNextOpen(false)
            startReconnectSequence(
                attempts = Int.MAX_VALUE,
                timeoutMs = timeoutMs,
                pauseMs = 2_000L
            )
        } else {
            _scanState.value = ScanUiState.Idle
        }
    }

    fun startAppReconnectIfPossible(timeoutMs: Long = 8_000L) {
        val device = _connectedDevice.value ?: return
        if (!isAppVisible || !hasBluetoothPermissions() || !isBluetoothEnabled()) return
        if (device.disconnectPending || device.isConnected) return
        if (reconnectJob?.isActive == true) return

        ensureForegroundServiceRunning()
        setReconnectOnNextOpen(false)
        startReconnectSequence(
            attempts = Int.MAX_VALUE,
            timeoutMs = timeoutMs,
            pauseMs = 2_000L
        )
    }

    fun resumeManagedSession(timeoutMs: Long = 20_000L) {
        val device = _connectedDevice.value ?: return
        if (!shouldMaintainConnection(device)) return
        if (!hasBluetoothPermissions() || !isBluetoothEnabled()) return
        if (device.isConnected || reconnectJob?.isActive == true) return

        startReconnectSequence(
            attempts = Int.MAX_VALUE,
            timeoutMs = timeoutMs,
            pauseMs = 2_000L
        )
    }

    fun startScanWithTimeout(timeoutMs: Long = 20_000L) {
        if (!isConnectScreenVisible) {
            _scanState.value = ScanUiState.Idle
            return
        }
        if (!hasBluetoothPermissions() || !isBluetoothEnabled()) {
            _scanState.value = ScanUiState.Idle
            return
        }
        scanJob?.cancel()
        gotConnection = false
        _scanState.value = ScanUiState.Scanning
        scanJob = scope.launch(Dispatchers.Main.immediate) {
            try {
                client.startScan()
            } catch (_: SecurityException) {
                _scanState.value = ScanUiState.Idle
                return@launch
            } catch (_: IllegalStateException) {
                _scanState.value = ScanUiState.Idle
                return@launch
            }

            val end = System.currentTimeMillis() + timeoutMs
            while (isActive && System.currentTimeMillis() < end && !gotConnection) {
                delay(100)
            }

            client.stop()
            if (!gotConnection) _scanState.value = ScanUiState.Empty
        }
    }

    fun repeatScan(timeoutMs: Long = 20_000L) = startScanWithTimeout(timeoutMs)

    fun stop() {
        scanJob?.cancel()
        reconnectJob?.cancel()
        client.stop()
        _scanState.value = ScanUiState.Idle
        stopForegroundService()
    }

    fun onAppVisible() {
        isAppVisible = true
    }

    fun onAppHidden() {
        val connected = _connectedDevice.value
        setReconnectOnNextOpen(shouldMaintainConnection(connected))
        isAppVisible = false
        scanJob?.cancel()
        if (_scanState.value == ScanUiState.Scanning) {
            client.stop()
            _scanState.value = if (connected?.isConnected == true) {
                ScanUiState.Connected
            } else {
                ScanUiState.Idle
            }
        }
        if (!shouldMaintainConnection(connected)) {
            reconnectJob?.cancel()
        }
    }

    fun onConnectScreenVisible() {
        isConnectScreenVisible = true
    }

    fun onConnectScreenHidden() {
        isConnectScreenVisible = false
        scanJob?.cancel()
        if (_scanState.value == ScanUiState.Scanning) {
            client.stop()
            val connected = _connectedDevice.value
            _scanState.value = if (connected?.isConnected == true) {
                ScanUiState.Connected
            } else {
                ScanUiState.Idle
            }
        }
    }

    fun requestDisconnect() {
        val device = _connectedDevice.value ?: return

        // A manual disconnect should forget the band immediately and never schedule reconnect.
        reconnectJob?.cancel()
        scanJob?.cancel()
        gotConnection = false
        client.stop()
        removeBandFromScanResults(device.address)
        clearConnectedDevice(clearPreferred = true)
        _scanState.value = ScanUiState.Idle
        disconnectCurrent()
    }

    fun connectTo(address: String) {
        ensureForegroundServiceRunning()
        try {
            client.connect(address)
        } catch (_: SecurityException) {
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        val needed: List<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        return needed.all {
            ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        return try {
            adapter.isEnabled
        } catch (_: SecurityException) {
            false
        }
    }

    private fun savePreferredAddress(addr: String) {
        preferredAddress = addr
        prefs.edit { putString(KEY_LAST_BAND_ADDR, addr) }
    }

    private fun clearPreferredAddress() {
        preferredAddress = null
        prefs.edit { remove(KEY_LAST_BAND_ADDR) }
    }

    private fun setReconnectOnNextOpen(enabled: Boolean) {
        reconnectOnNextOpen = enabled
        prefs.edit {
            if (enabled) {
                putBoolean(KEY_RECONNECT_ON_NEXT_OPEN, true)
            } else {
                remove(KEY_RECONNECT_ON_NEXT_OPEN)
            }
        }
    }

    private fun loadConnectedDevice(): ConnectedDevice? {
        val addr = prefs.getString(KEY_CONNECTED_ADDR, null) ?: return null
        val name = prefs.getString(KEY_CONNECTED_NAME, null)
        val lastConnected = prefs.getLong(KEY_CONNECTED_LAST_CONNECTED, System.currentTimeMillis())
        val pending = prefs.getBoolean(KEY_CONNECTED_DISCONNECT_PENDING, false)
        val requestedAt = prefs.getLong(KEY_CONNECTED_DISCONNECT_REQUESTED, 0L)
        val sleepStartedAt = prefs.getLong(KEY_CONNECTED_SLEEP_STARTED, 0L)
        val resolvedRequestedAt = when {
            requestedAt > 0L -> requestedAt
            pending -> System.currentTimeMillis()
            else -> 0L
        }
        val resolvedSleepStartedAt = when {
            sleepStartedAt > 0L -> sleepStartedAt
            pending -> 0L
            else -> lastConnected
        }
        return ConnectedDevice(
            address = addr,
            name = name,
            lastConnectedAt = lastConnected,
            isConnected = false,
            disconnectPending = pending,
            disconnectRequestedAt = resolvedRequestedAt.takeIf { it > 0L },
            sleepStartedAt = resolvedSleepStartedAt.takeIf { it > 0L }
        )
    }

    private fun persistConnectedDevice(device: ConnectedDevice?) {
        prefs.edit {
            if (device == null) {
                remove(KEY_CONNECTED_ADDR)
                remove(KEY_CONNECTED_NAME)
                remove(KEY_CONNECTED_LAST_CONNECTED)
                remove(KEY_CONNECTED_IS_CONNECTED)
                remove(KEY_CONNECTED_DISCONNECT_PENDING)
                remove(KEY_CONNECTED_DISCONNECT_REQUESTED)
                remove(KEY_CONNECTED_SLEEP_STARTED)
            } else {
                putString(KEY_CONNECTED_ADDR, device.address)
                putString(KEY_CONNECTED_NAME, device.name)
                putLong(KEY_CONNECTED_LAST_CONNECTED, device.lastConnectedAt)
                putBoolean(KEY_CONNECTED_IS_CONNECTED, device.isConnected)
                putBoolean(KEY_CONNECTED_DISCONNECT_PENDING, device.disconnectPending)
                putLong(KEY_CONNECTED_DISCONNECT_REQUESTED, device.disconnectRequestedAt ?: 0L)
                putLong(KEY_CONNECTED_SLEEP_STARTED, device.sleepStartedAt ?: 0L)
            }
        }
    }

    private fun setConnectedDevice(device: ConnectedDevice?) {
        _connectedDevice.value = device
        persistConnectedDevice(device)
    }

    private fun updateConnectedDevice(transform: (ConnectedDevice) -> ConnectedDevice) {
        val current = _connectedDevice.value ?: return
        setConnectedDevice(transform(current))
    }

    private fun resolveBandName(address: String): String? {
        val fromList = _bands.value.firstOrNull { it.address == address }?.name
        if (!fromList.isNullOrBlank()) return fromList

        val fromConnected = _connectedDevice.value?.takeIf { it.address == address }?.name
        if (!fromConnected.isNullOrBlank()) return fromConnected

        return try {
            adapter.getRemoteDevice(address)?.name
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    private fun removeBandFromScanResults(address: String) {
        _bands.value = _bands.value.filterNot { it.address == address }
    }

    private fun markConnected(address: String, name: String? = null) {
        val now = System.currentTimeMillis()
        val existing = _connectedDevice.value
        val keepPending = existing?.address == address && existing.disconnectPending
        val resolvedName = name ?: resolveBandName(address)
        setReconnectOnNextOpen(false)
        setConnectedDevice(
            ConnectedDevice(
                address = address,
                name = resolvedName,
                lastConnectedAt = now,
                isConnected = true,
                disconnectPending = keepPending,
                disconnectRequestedAt = if (keepPending) existing.disconnectRequestedAt else null,
                sleepStartedAt = null
            )
        )
    }

    private fun clearConnectedDevice(clearPreferred: Boolean) {
        setReconnectOnNextOpen(false)
        setConnectedDevice(null)
        if (clearPreferred) {
            clearPreferredAddress()
        }
        stopForegroundService()
    }

    private fun startDisconnectCleanup() {
        scope.launch {
            while (isActive) {
                delay(5_000)
                val device = _connectedDevice.value ?: continue
                if (device.isConnected) continue
                val now = System.currentTimeMillis()
                if (device.disconnectPending) {
                    val requestedAt = device.disconnectRequestedAt ?: continue
                    if (now - requestedAt >= disconnectTimeoutMs) {
                        clearConnectedDevice(clearPreferred = true)
                    }
                }
            }
        }
    }

    private fun startReconnectSequence(
        attempts: Int = 3,
        timeoutMs: Long = 8_000L,
        pauseMs: Long = 1_000L
    ) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var currentPauseMs = pauseMs
            repeat(attempts) {
                val device = _connectedDevice.value
                if (!shouldMaintainConnection(device)) return@launch
                if (preferredAddress == null && device == null) return@launch
                if (device != null && device.isConnected) return@launch
                val address = preferredAddress ?: device?.address ?: return@launch
                gotConnection = false
                connectTo(address)

                val end = System.currentTimeMillis() + timeoutMs
                while (
                    isActive &&
                    shouldMaintainConnection() &&
                    System.currentTimeMillis() < end &&
                    !gotConnection
                ) {
                    delay(100)
                }

                if (gotConnection) return@launch
                if (!shouldMaintainConnection()) return@launch
                delay(currentPauseMs)
                currentPauseMs = (currentPauseMs * 2).coerceAtMost(60_000L)
            }
        }
    }

    private fun disconnectCurrent() {
        try {
            client.disconnect()
        } catch (_: SecurityException) {
        }
    }

    private fun shouldMaintainConnection(device: ConnectedDevice? = _connectedDevice.value): Boolean {
        return device != null && !device.disconnectPending
    }

    private fun ensureForegroundServiceRunning() {
        BleForegroundService.start(ctx)
    }

    private fun stopForegroundService() {
        BleForegroundService.stop(ctx)
    }

    private suspend fun resolveTargetUserId(): Int {
        return profileRepo.getActive()?.id ?: 1
    }

    private fun normalizeEpochMillis(epoch: Long): Long {
        if (epoch <= 0L) return System.currentTimeMillis()
        return if (epoch < 100_000_000_000L) epoch * 1_000L else epoch
    }

    private companion object {
        const val KEY_LAST_BAND_ADDR = "last_band_address"
        const val KEY_CONNECTED_ADDR = "connected_band_address"
        const val KEY_CONNECTED_NAME = "connected_band_name"
        const val KEY_CONNECTED_LAST_CONNECTED = "connected_band_last_connected"
        const val KEY_CONNECTED_IS_CONNECTED = "connected_band_is_connected"
        const val KEY_CONNECTED_DISCONNECT_PENDING = "connected_band_disconnect_pending"
        const val KEY_CONNECTED_DISCONNECT_REQUESTED = "connected_band_disconnect_requested_at"
        const val KEY_CONNECTED_SLEEP_STARTED = "connected_band_sleep_started_at"
        const val KEY_RECONNECT_ON_NEXT_OPEN = "reconnect_on_next_open"
    }
}
