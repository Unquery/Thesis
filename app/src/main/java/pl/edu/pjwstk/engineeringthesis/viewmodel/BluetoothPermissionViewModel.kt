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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothPermissionViewModel @Inject constructor(
    @ApplicationContext private val app: Context
) : ViewModel() {

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
        _state.update {
            it.copy(
                hasPermissions = hasPerms,
                bluetoothOn = if (hasPerms) currentBluetoothState(app) else false
            )
        }
        if (!hasPerms && connectPressed) {
            viewModelScope.launch { _events.emit(UiEvent.RequestPermissions) }
        } else if (hasPerms && connectPressed) {
            viewModelScope.launch { _events.emit(UiEvent.ConnectNow) }
        }
    }

    fun onPermissionsResult(grantedAll: Boolean) {
        _state.update {
            it.copy(
                hasPermissions = grantedAll,
                bluetoothOn = if (grantedAll) currentBluetoothState(app) else false
            )
        }
        if (grantedAll) {
            viewModelScope.launch { _events.emit(UiEvent.ConnectNow) }
        }
    }

    private fun currentBluetoothState(context: Context): Boolean {
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return false
        return try {
            manager.adapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val bluetoothOnFlow: StateFlow<Boolean> =
        _state
            .map { it.hasPermissions }
            .distinctUntilChanged()
            .flatMapLatest { hasPermissions ->
                if (hasPermissions) bluetoothStateFlow(app) else flowOf(false)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )

    private fun bluetoothStateFlow(context: Context): Flow<Boolean> = callbackFlow {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val state = i.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                trySend(state == BluetoothAdapter.STATE_ON)
            }
        }
        var registered = false
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            registered = true
        } catch (_: SecurityException) {
            trySend(false)
        }
        trySend(currentBluetoothState(context))
        awaitClose {
            if (registered) {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }

    init {
        viewModelScope.launch {
            bluetoothOnFlow.collect { on ->
                _state.update { it.copy(bluetoothOn = on) }
            }
        }
    }
}
