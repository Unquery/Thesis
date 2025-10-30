package pl.edu.pjwstk.engineeringthesis.viewmodel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import pl.edu.pjwstk.engineeringthesis.bluetooth.BleUartClient
import pl.edu.pjwstk.engineeringthesis.bluetooth.Packet
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConnectBandViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val adapter: BluetoothAdapter
): ViewModel() {

    private val _status = MutableStateFlow("Idle");
    val status = _status

    private val _temps  = MutableStateFlow<List<Float>>(emptyList());
    val temps = _temps

    private val _gsr    = MutableStateFlow<List<Int>>(emptyList());
    val gsr   = _gsr

    private val client = BleUartClient(
        ctx,
        adapter,
        UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"), // UART service
        UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")  // TX characteristic (Notify)
    ).apply {
        setDeviceNameFilter("ESP32-TEMP")
        setListener(object : BleUartClient.Listener {
            override fun onStatus(s: String) { _status.value = s }
            override fun onConnected(address: String, mtu: Int) { _status.value = "Connected (MTU=$mtu)" }
            override fun onDisconnected() { _status.value = "Disconnected" }
            override fun onPacket(p: Packet) {
                _temps.value = p.temps
                _gsr.value = p.gsr
            }
            override fun onError(msg: String, t: Throwable?) { _status.value = "Error: $msg" }
        })
    }

    fun startAfterPermissionsGranted() {
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

        client.startScan()
    }

    fun stop() { client.stop() }
}


