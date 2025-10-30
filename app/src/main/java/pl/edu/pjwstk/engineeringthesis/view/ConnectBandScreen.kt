package pl.edu.pjwstk.engineeringthesis.view

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.ui.theme.DarkGray850
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import pl.edu.pjwstk.engineeringthesis.viewmodel.BluetoothPermissionViewModel
import pl.edu.pjwstk.engineeringthesis.viewmodel.ConnectBandViewModel
import java.util.Locale

@Composable
fun ConnectBandScreen(
    vmBluetoothPermission : BluetoothPermissionViewModel = hiltViewModel(),
    vmConnectBand : ConnectBandViewModel = hiltViewModel()
) {

    val ctx = LocalContext.current
    val state by vmBluetoothPermission.state.collectAsStateWithLifecycle()

    val temps by vmConnectBand.temps.collectAsStateWithLifecycle()
    val status by vmConnectBand.status.collectAsStateWithLifecycle()
    val gsr   by vmConnectBand.gsr.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {results ->
        val allGranted = vmBluetoothPermission.requiredBluetoothPermissions().all { results[it] == true }
        vmBluetoothPermission.onPermissionsResult(allGranted)
    }

    val enableBt = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){}

    LaunchedEffect(Unit) {
        vmBluetoothPermission.events.collect { ev ->
            when (ev) {
                BluetoothPermissionViewModel.UiEvent.RequestPermissions -> launcher.launch(vmBluetoothPermission.requiredBluetoothPermissions())
                BluetoothPermissionViewModel.UiEvent.ConnectNow -> {
                    if (state.bluetoothOn) {
                        vmConnectBand.startAfterPermissionsGranted()
                    } else {
                        enableBt.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopConnectScreenBar()
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color.DarkGray)
                )
            }

        ) { inner ->
            Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .background(Color.Black)
            ){

            }
        }
    }

//    Column(Modifier.padding(16.dp)) {
//        Text(
//            when {
//                !state.hasPermissions -> "Permissions needed"
//                !state.bluetoothOn    -> "Bluetooth is off"
//                else -> status
//            }
//        )
//
//        Button(onClick = {
//            val hasAll = vmBluetoothPermission.requiredBluetoothPermissions().all {
//                ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
//            }
//            when {
//                hasAll -> vmBluetoothPermission.onPermissionsResult(true)
//                !hasAll -> vmBluetoothPermission.onStart(connectPressed = true, hasPerms = hasAll)
//                !state.bluetoothOn -> enableBt.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
//                else -> {
//                   vmConnectBand.startAfterPermissionsGranted()
//                }
//            }
//        }) {
//            Text("Connect band")
//        }
//
//        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
//            Text(text = "BLE Temp (10 samples every ~10s)", style = MaterialTheme.typography.titleLarge)
//            HorizontalDivider()
//            if (temps.isEmpty()) {
//                Text("Waiting for first packet…")
//            } else {
//                Text("Last 10 temps (°C):")
//                LazyColumn(modifier = Modifier.weight(1f)) {
//                    itemsIndexed(temps) { i, t ->
//                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
//                            Text("#${i+1}", modifier = Modifier.width(48.dp))
//                            LinearProgressIndicator(progress = { ((t - 30f)/15f).coerceIn(0f,1f) }, modifier = Modifier.weight(1f).height(6.dp))
//                            Spacer(Modifier.width(12.dp))
//                            Text(String.format(Locale.US, "%.2f", t))
//                        }
//                        Spacer(Modifier.height(6.dp))
//                    }
//                }
//            }
//        }
//    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopConnectScreenBar(){
    Box {
        TopAppBar(
            title = {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.main_screen_title),
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        fontSize = 40.sp
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            navigationIcon = { /* no impl */ },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                scrolledContainerColor = Color.Black,
                navigationIconContentColor = Color.White,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectBandScreenPreview() {
    EngineeringThesisTheme {
        TopConnectScreenBar()
    }
}