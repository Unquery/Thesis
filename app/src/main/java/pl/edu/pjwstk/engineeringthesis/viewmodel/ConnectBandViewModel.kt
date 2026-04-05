package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import pl.edu.pjwstk.engineeringthesis.bluetooth.BleConnectionManager
import javax.inject.Inject

@HiltViewModel
class ConnectBandViewModel @Inject constructor(
    private val bleConnectionManager: BleConnectionManager
) : ViewModel() {

    val scanState = bleConnectionManager.scanState
    val bands = bleConnectionManager.bands
    val connectedDevice = bleConnectionManager.connectedDevice

    fun startAfterPermissionsGranted(timeoutMs: Long = 8_000L) {
        bleConnectionManager.startAfterPermissionsGranted(timeoutMs)
    }

    fun startAppReconnectIfPossible(timeoutMs: Long = 8_000L) {
        bleConnectionManager.startAppReconnectIfPossible(timeoutMs)
    }

    fun startScanWithTimeout(timeoutMs: Long = 20_000L) {
        bleConnectionManager.startScanWithTimeout(timeoutMs)
    }

    fun repeatScan(timeoutMs: Long = 20_000L) {
        bleConnectionManager.repeatScan(timeoutMs)
    }

    fun stop() {
        bleConnectionManager.stop()
    }

    fun onAppVisible() {
        bleConnectionManager.onAppVisible()
    }

    fun onAppHidden() {
        bleConnectionManager.onAppHidden()
    }

    fun onConnectScreenVisible() {
        bleConnectionManager.onConnectScreenVisible()
    }

    fun onConnectScreenHidden() {
        bleConnectionManager.onConnectScreenHidden()
    }

    fun requestDisconnect() {
        bleConnectionManager.requestDisconnect()
    }

    fun connectTo(address: String) {
        bleConnectionManager.connectTo(address)
    }
}
