package pl.edu.pjwstk.engineeringthesis.view

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import pl.edu.pjwstk.engineeringthesis.viewmodel.BluetoothPermissionViewModel

@Composable
fun ConnectBandScreen(
    vm : BluetoothPermissionViewModel = hiltViewModel()
) {

    var ctx = LocalContext.current
    val state by vm.state.collectAsState()

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasAllPermissions(): Boolean =
        permissions.all { perm ->
            ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
        }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        vm.onPermissionsResult(hasAllPermissions())
    }

    val enableBt = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        vm.onBlConnect(vm.isBluetoothOn(ctx))
    }

    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                BluetoothPermissionViewModel.UiEvent.RequestPermissions -> launcher.launch(permissions)
                BluetoothPermissionViewModel.UiEvent.ConnectNow -> {

                }
            }
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text(if (state.hasPermissions) "Permissions granted" else if (state.bluetoothOn) "Bluetooth is on" else "Permissions needed")

        Button(onClick = {
            if(!hasAllPermissions()) {
                vm.onStart(connectPressed = true, hasPerms = hasAllPermissions())
            }else if(!state.bluetoothOn){
                enableBt.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }) {
            Text("Connect band")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectBandScreenPreview() {
    EngineeringThesisTheme {
        ConnectBandScreen()
    }
}