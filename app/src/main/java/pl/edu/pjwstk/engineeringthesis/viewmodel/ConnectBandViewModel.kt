package pl.edu.pjwstk.engineeringthesis.viewmodel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.edu.pjwstk.engineeringthesis.bluetooth.BleUartClient
import pl.edu.pjwstk.engineeringthesis.bluetooth.Packet
import pl.edu.pjwstk.engineeringthesis.data.db.dao.GsrSampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.TempSampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity
import java.util.UUID
import javax.inject.Inject
import androidx.core.content.edit


sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Scanning : ScanUiState
    data object Empty : ScanUiState
    data object Connected : ScanUiState
}

data class Band(
    val address: String,
    val name: String? = null,
    val rssi: Int = Int.MIN_VALUE
)

@HiltViewModel
class ConnectBandViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val adapter: BluetoothAdapter,
    private val gsrSampleDao : GsrSampleDao,
    private val tempSampleDao: TempSampleDao
): ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState

    private val _bands = MutableStateFlow<List<Band>>(emptyList())
    val bands: StateFlow<List<Band>> = _bands

    private var scanJob: Job? = null
    @Volatile private var gotConnection = false

    private val prefs by lazy {
        ctx.getSharedPreferences("ble_band_prefs", Context.MODE_PRIVATE)
    }
    private val KEY_LAST_BAND_ADDR = "last_band_address"

    private var preferredAddress: String? =
        prefs.getString(KEY_LAST_BAND_ADDR, null)

    private fun savePreferredAddress(addr: String) {
        preferredAddress = addr
        prefs.edit() { putString(KEY_LAST_BAND_ADDR, addr) }
    }

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
                if (idx >= 0) updated[idx] = updated[idx].copy(name = name, rssi = rssi)
                else updated += Band(address, name, rssi)
                _bands.value = updated.sortedByDescending { it.rssi }
                val preferred = preferredAddress
                if (!gotConnection && preferred != null && preferred == address) {
                    try {
                        connectTo(address)
                    } catch (_: SecurityException) {
                        // ignore, permissions already checked before scan
                    }
                }
            }

            override fun onConnected(address: String, mtu: Int) {
                gotConnection = true
                _scanState.value = ScanUiState.Connected
                savePreferredAddress(address)
            }
            override fun onDisconnected() {
                _scanState.value = ScanUiState.Idle
                if (preferredAddress != null) {
                    viewModelScope.launch {
                        delay(1000)
                        startScanWithTimeout(20_000L)
                    }
                }
            }

            override fun onPacket(p: Packet) {
                viewModelScope.launch(Dispatchers.IO) {
                    val tempSamples = p.temps.map { TempSampleEntity(epoch = p.epoch, temperature = it) }
                    val gsrSamples = p.gsr.map { GsrSampleEntity(epoch = p.epoch, gsr = it) }

                    try {
                        tempSamples.forEach { tempSampleDao.upsertTempSample(it) }
                        gsrSamples.forEach { gsrSampleDao.upsertGsrSample(it) }
                    } catch (e: Exception) {
                        Log.e("ConnectBandViewModel", "Failed to insert samples into the database", e)
                    }
                }

            }

            override fun onError(msg: String, t: Throwable?) {
                _scanState.value = ScanUiState.Idle
            }
        })
    }

    fun startAfterPermissionsGranted(timeoutMs: Long = 8000L) {
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

        startScanWithTimeout(timeoutMs)
    }

    fun startScanWithTimeout(timeoutMs: Long = 20000L) {
        scanJob?.cancel()
        gotConnection = false
        _scanState.value = ScanUiState.Scanning
        Log.d("asd",_scanState.value.toString())
        scanJob = viewModelScope.launch(Dispatchers.Main.immediate) {
            try { client.startScan() }
            catch (_: SecurityException) {
                _scanState.value = ScanUiState.Idle
                Log.d("asd",_scanState.value.toString())
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

    fun repeatScan(timeoutMs: Long = 20000L) = startScanWithTimeout(timeoutMs)

    fun stop() {
        scanJob?.cancel()
        client.stop()
        _scanState.value = ScanUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }

    fun connectTo(address: String) {
        try {
            client.connect(address)
        } catch (_ : SecurityException){
        }
    }
}


