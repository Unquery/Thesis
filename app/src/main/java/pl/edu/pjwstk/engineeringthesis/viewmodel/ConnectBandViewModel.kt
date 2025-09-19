package pl.edu.pjwstk.engineeringthesis.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConnectBandViewModel @Inject constructor(
    private val adapter: BluetoothAdapter,
    @ApplicationContext context: Context
) : ViewModel() {

    private val UART_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val UART_TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

    private var scanner: BluetoothLeScanner? = null

    private var gatt: BluetoothGatt? = null
    private var txChar: BluetoothGattCharacteristic? = null

    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> = _status

    private val _temps = MutableStateFlow<List<Float>>(emptyList())
    val temps: StateFlow<List<Float>> = _temps

    private val _gsr = MutableStateFlow<List<Int>>(emptyList())
    val gsr: StateFlow<List<Int>> = _gsr

    init {
        scanner = adapter.bluetoothLeScanner
    }

     fun ensurePermsAndScan(context: Context) {
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
             ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
         }
         if (missing.isNotEmpty()) return
         startScan()
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        _status.value = "Scanning…"
        val filters = listOf(
            ScanFilter.Builder().setDeviceName("ESP32-TEMP").build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(UART_SERVICE_UUID)).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(filters, settings, scanCb)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() { scanner?.stopScan(scanCb) }

    private val scanCb = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rec = result.scanRecord
            val name = device.name ?: rec?.deviceName
            val uuids = rec?.serviceUuids?.joinToString { it.uuid.toString() }

            Log.d("SCAN", "addr=${device.address} name=$name uuids=$uuids rssi=${result.rssi}")

            if (name == "ESP32-TEMP" || uuids?.contains("6E400001-B5A3-F393-E0A9-E50E24DCCA9E", ignoreCase = true) == true) {
                stopScan()
                _status.value = "Connecting to ${name}…"
                gatt = device.connectGatt(context, false, gattCb, BluetoothDevice.TRANSPORT_LE)
            }
        }
        override fun onScanFailed(errorCode: Int) { _status.value = "Scan failed: $errorCode" }
    }

    private val gattCb = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                _status.value = "Discovering services…"
                g.discoverServices()
            } else { _status.value = "Disconnected"; disconnect() }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                viewModelScope.launch(Dispatchers.Main) { _status.value = "Service discovery failed" }
                return
            }
            val svc = g.getService(UART_SERVICE_UUID)
            txChar = svc?.getCharacteristic(UART_TX_CHAR_UUID)
            if (txChar == null) {
                viewModelScope.launch(Dispatchers.Main) { _status.value = "UART TX char not found" }
                return
            }

            val props = txChar!!.properties
            val useIndicate = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0

            enableNotifications(g, useIndicate)

            viewModelScope.launch(Dispatchers.Main) { _status.value = "Requesting MTU…" }
            g.requestMtu(185)
        }


        @SuppressLint("MissingPermission")
        private fun enableNotifications(g: BluetoothGatt, useIndicate: Boolean) {
            val ok = g.setCharacteristicNotification(txChar, true)
            val cccd = txChar?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (!ok || cccd == null) {
                viewModelScope.launch(Dispatchers.Main) { _status.value = "Failed to enable notifications" }
                return
            }
            cccd.value = if (useIndicate)
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            else
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(cccd)
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            viewModelScope.launch(Dispatchers.Main) { _status.value = "MTU=$mtu; ensuring subscription…" }
            val useIndicate = (txChar?.properties ?: 0) and
                    BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
            enableNotifications(g, useIndicate)
        }


        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            if (d.uuid.toString().equals("00002902-0000-1000-8000-00805f9b34fb", true)) {
                viewModelScope.launch(Dispatchers.Main) {
                    _status.value = if (status == BluetoothGatt.GATT_SUCCESS)
                        "Subscribed; waiting for data…" else "Subscribe failed ($status)"
                }
            }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (c.uuid == UART_TX_CHAR_UUID) handleNotify(c.value)
        }
    }

    private val sb = StringBuilder()

    private fun JSONArray.toFloatList(): List<Float> =
        List(length()) { i -> getDouble(i).toFloat() }

    private fun JSONArray.toIntList(): List<Int> =
        List(length()) { i -> getInt(i) }

    private fun handleNotify(bytes: ByteArray) {
        val chunk = String(bytes, StandardCharsets.UTF_8)
        sb.append(chunk)

        var nl = sb.indexOf("\n")
        while (nl != -1) {
            val line = sb.substring(0, nl)
            sb.delete(0, nl + 1)
            try {
                val obj = JSONObject(line)

                val tempArr = obj.optJSONArray("temp") ?: obj.optJSONArray("t")
                val gsrArr  = obj.optJSONArray("gsr")

                val tempsList = tempArr?.toFloatList().orEmpty()
                val gsrList   = gsrArr?.toIntList().orEmpty()
                val epoch     = obj.optLong("epoch", -1L)

                viewModelScope.launch(Dispatchers.Main) {
                    if (tempsList.isNotEmpty()) _temps.value = tempsList
                    if (gsrList.isNotEmpty())   _gsr.value   = gsrList
                    _status.value = buildString {
                        append("Last packet")
                        if (epoch >= 0) append(": epoch $epoch")
                        append(" (temp n=${tempsList.size}, gsr n=${gsrList.size})")
                    }
                }
            } catch (e: Exception) {
                Log.e("BLE", "JSON parse error: ${e.message} raw=$line")
            }
            nl = sb.indexOf("\n")
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnect() {
        try { stopScan() } catch (_: Exception) {}
        try { gatt?.disconnect() } catch (_: Exception) {}
        try { gatt?.close() } catch (_: Exception) {}
        gatt = null; txChar = null
    }
}