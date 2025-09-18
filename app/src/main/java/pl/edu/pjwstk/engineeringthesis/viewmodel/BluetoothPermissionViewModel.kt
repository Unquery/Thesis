package pl.edu.pjwstk.engineeringthesis.viewmodel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BluetoothPermissionViewModel @Inject constructor() : ViewModel() {
    private val UART_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val UART_TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var scanner: BluetoothLeScanner? = null


    private var gatt: BluetoothGatt? = null
    private var txChar: BluetoothGattCharacteristic? = null

    data class UiState(
        val hasPermissions: Boolean = false,
        val connecting: Boolean = false,
        val error: String? = null,
        val bluetoothOn : Boolean = false
    )

    sealed interface UiEvent {
        data object RequestPermissions : UiEvent
        data object ConnectNow : UiEvent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    fun onStart(connectPressed: Boolean = false, hasPerms: Boolean) {
        _state.update { it.copy(hasPermissions = hasPerms) }
        if (!hasPerms && connectPressed) {
            viewModelScope.launch { _events.emit(UiEvent.RequestPermissions) }
        } else if (hasPerms && connectPressed) {
            viewModelScope.launch { _events.emit(UiEvent.ConnectNow) }
        }
    }

    fun onPermissionsResult(grantedAll: Boolean) {
        _state.update { it.copy(hasPermissions = grantedAll) }
        if (grantedAll) {
            viewModelScope.launch { _events.emit(UiEvent.ConnectNow) }
        }
    }

    fun isBluetoothOn(context: Context): Boolean {
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager.adapter ?: return false
        return try {
            adapter.isEnabled
        } catch (se: SecurityException) {
            false
        }
    }

    fun onBlConnect(isOn: Boolean) {
        _state.update { it.copy(bluetoothOn = isOn) }
    }

}