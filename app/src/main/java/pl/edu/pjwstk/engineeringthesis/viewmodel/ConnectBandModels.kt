package pl.edu.pjwstk.engineeringthesis.viewmodel

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

data class ConnectedDevice(
    val address: String,
    val name: String? = null,
    val lastConnectedAt: Long,
    val isConnected: Boolean,
    val disconnectPending: Boolean,
    val disconnectRequestedAt: Long? = null,
    val sleepStartedAt: Long? = null
)
