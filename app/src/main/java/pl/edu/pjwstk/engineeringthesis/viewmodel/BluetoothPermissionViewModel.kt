package pl.edu.pjwstk.engineeringthesis.viewmodel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothPermissionViewModel @Inject constructor( @ApplicationContext app: Context) : ViewModel() {

    data class UiState(
        val hasPermissions: Boolean = false,
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

    fun requiredBluetoothPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

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
        } catch (_: SecurityException) {
            false
        }
    }

    private val bluetoothOnFlow: StateFlow<Boolean> =
        bluetoothStateFlow(app)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = isBluetoothOn(app)
            )

    private fun bluetoothStateFlow(context: Context): Flow<Boolean> = callbackFlow {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val state = i.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                trySend(state == BluetoothAdapter.STATE_ON)
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        trySend((context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled) == true)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    init {
        viewModelScope.launch {
            bluetoothOnFlow.collect { on ->
                _state.update { it.copy(bluetoothOn = on) }
            }
        }
    }
}