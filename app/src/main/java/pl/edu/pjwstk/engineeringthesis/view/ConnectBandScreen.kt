package pl.edu.pjwstk.engineeringthesis.view

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.font.interFamily
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import pl.edu.pjwstk.engineeringthesis.viewmodel.Band
import pl.edu.pjwstk.engineeringthesis.viewmodel.BluetoothPermissionViewModel
import pl.edu.pjwstk.engineeringthesis.viewmodel.ConnectedDevice
import pl.edu.pjwstk.engineeringthesis.viewmodel.ConnectBandViewModel
import pl.edu.pjwstk.engineeringthesis.viewmodel.ScanUiState
import kotlin.math.absoluteValue

@Composable
fun ConnectBandScreen(
    vmBluetoothPermission: BluetoothPermissionViewModel = hiltViewModel(),
    vmConnectBand: ConnectBandViewModel = hiltViewModel()
) {

    val state by vmBluetoothPermission.state.collectAsStateWithLifecycle()

    val scanState by vmConnectBand.scanState.collectAsStateWithLifecycle()
    val bands by vmConnectBand.bands.collectAsStateWithLifecycle()
    val connectedDevice by vmConnectBand.connectedDevice.collectAsStateWithLifecycle()

    DisposableEffect(vmConnectBand) {
        vmConnectBand.onConnectScreenVisible()
        onDispose { vmConnectBand.onConnectScreenHidden() }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {results ->
        val allGranted = vmBluetoothPermission.requiredBluetoothPermissions().all { results[it] == true }
        vmBluetoothPermission.onPermissionsResult(allGranted)
    }

    val enableBt = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){}

    LaunchedEffect(state.hasPermissions, state.bluetoothOn) {
        when {
            !state.hasPermissions -> {
                launcher.launch(vmBluetoothPermission.requiredBluetoothPermissions())
            }
            state.hasPermissions && !state.bluetoothOn -> {
                enableBt.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            else -> {
                vmConnectBand.startAfterPermissionsGranted()
            }
        }
    }



    Box(Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopConnectScreenBar()
            }

        ) { inner ->
            Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ){
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ScanStatusPanel(
                        modifier = Modifier.weight(1f),
                        state = scanState,
                        bands = bands,
                        onBandClick = { vmConnectBand.connectTo(it.address) },
                        onRepeat = { vmConnectBand.repeatScan() }
                    )
                    ConnectedDevicePanel(
                        modifier = Modifier.weight(1f),
                        device = connectedDevice,
                        onDisconnect = { vmConnectBand.requestDisconnect() }
                    )
                }
            }
        }
    }
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
                        stringResource(R.string.connect_band_screen_title),
                        fontFamily = interFamily,
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
                containerColor = Color(0xFF0F172A),
                scrolledContainerColor = Color(0xFF0F172A),
                navigationIconContentColor = Color.White,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }
}


@Composable
private fun ScanStatusPanel(
    modifier: Modifier = Modifier,
    state: ScanUiState,
    bands: List<Band>,
    onBandClick: (Band) -> Unit,
    onRepeat: () -> Unit
) {
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (state) {
                    ScanUiState.Scanning  -> stringResource(R.string.scan_state_scanning)
                    ScanUiState.Connected -> stringResource(R.string.scan_state_connected)
                    ScanUiState.Empty     -> stringResource(R.string.scan_state_found_bands, bands.size)
                    ScanUiState.Idle      -> stringResource(R.string.scan_state_idle)
                },
                color = Color.White,
                fontFamily = interFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            )

            if (state != ScanUiState.Scanning) {
                Button(
                    onClick = onRepeat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color.White
                    )
                ) { Text(stringResource(R.string.scan_repeat)) }
            }
        }

        if (state == ScanUiState.Scanning) {
            val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                LottieAnimation(composition = comp, iterations = LottieConstants.IterateForever, modifier = Modifier.size(72.dp))
            }
        }

        if (bands.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bands.size) { i ->
                    val item = bands[i]
                    BandRow(item, onClick = { onBandClick(item) })
                }
            }
        } else if (state == ScanUiState.Empty) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.scan_try_move_closer),
                color = Color.White, fontFamily = interFamily
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConnectedDevicePanel(
    modifier: Modifier = Modifier,
    device: ConnectedDevice?,
    onDisconnect: () -> Unit
) {
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.connected_device_title),
            color = Color.White,
            fontFamily = interFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(12.dp))
        if (device == null) {
            Text(
                text = stringResource(R.string.connected_device_none),
                color = Color.White,
                fontFamily = interFamily
            )
        } else {
            ConnectedDeviceRow(device = device, onDisconnect = onDisconnect)
        }
    }
}

@Composable
private fun ConnectedDeviceRow(
    device: ConnectedDevice,
    onDisconnect: () -> Unit
) {
    var showAction by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val statusText = when {
        device.disconnectPending -> stringResource(R.string.band_status_disconnect_pending)
        device.isConnected -> stringResource(R.string.band_status_connected)
        else -> stringResource(R.string.band_status_sleeping)
    }
    val transition = updateTransition(targetState = showAction, label = "disconnect_action")
    val spacerWidth by transition.animateDp(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 200)
            } else {
                tween(durationMillis = 400)
            }
        },
        label = "disconnect_spacer"
    ) { isVisible ->
        if (isVisible) 68.dp else 0.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF3A3A3A))
                .clickable(onClick = { showAction = !showAction })
                .onSizeChanged { size -> rowHeight = size.height }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = device.name ?: stringResource(R.string.band_unknown),
                    color = Color.White,
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = device.address,
                    color = Color.LightGray,
                    fontFamily = interFamily,
                    fontSize = 12.sp
                )
                Text(
                    text = statusText,
                    color = Color.LightGray,
                    fontFamily = interFamily,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(spacerWidth))
        }

        transition.AnimatedVisibility(
            visible = { it },
            modifier = Modifier
                .align(Alignment.CenterEnd),
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .height(with(LocalDensity.current) { rowHeight.toDp() })
                    .width(56.dp)
                    .background(Color(0xFFEF4444), RoundedCornerShape(10.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showAction = false
                        if (!device.disconnectPending) {
                            showDisconnectDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.band_disconnect),
                    tint = Color.White
                )
            }
        }

        if (showDisconnectDialog) {
            AlertDialog(
                onDismissRequest = { showDisconnectDialog = false },
                title = {
                    Text(text = stringResource(R.string.band_disconnect_confirm_title))
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = {
                                showDisconnectDialog = false
                                onDisconnect()
                            },
                            modifier = Modifier.width(124.dp),
                            border = BorderStroke(2.dp, DIALOG_ACTION_COLOR),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = stringResource(R.string.action_yes))
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Button(
                            onClick = { showDisconnectDialog = false },
                            modifier = Modifier.width(124.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DIALOG_ACTION_COLOR,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = stringResource(R.string.action_no))
                        }
                    }
                }
            )
        }
    }
}

private val DIALOG_ACTION_COLOR = Color(0xFF0F172A)

@Composable
private fun BandRow(band: Band, onClick: () -> Unit) {
    var showAction by remember { mutableStateOf(false) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val signalStrengthText = when (band.rssi.absoluteValue) {
        in 0..59 -> stringResource(R.string.band_signal_very_high)
        in 60..70 -> stringResource(R.string.band_signal_high)
        in 71..80 -> stringResource(R.string.band_signal_medium)
        in 81..90 -> stringResource(R.string.band_signal_low)
        else -> stringResource(R.string.band_signal_very_low)
    }

    val transition = updateTransition(targetState = showAction, label = "action")

    val spacerWidth by transition.animateDp(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 200)
            } else {
                tween(durationMillis = 400)
            }
        },
        label = "spacer"
    ) { isVisible ->
        if (isVisible) 68.dp else 0.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF3A3A3A))
                .clickable(onClick = { showAction = !showAction })
                .onSizeChanged { size ->
                    rowHeight = size.height
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = band.name ?: stringResource(R.string.band_unknown),
                        color = Color.White,
                        fontFamily = interFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.band_signal_label),
                        color = Color.White,
                        fontFamily = interFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = band.address,
                        color = Color.LightGray,
                        fontFamily = interFamily,
                        fontSize = 12.sp
                    )
                    Text(
                        text = signalStrengthText,
                        color = Color.LightGray,
                        fontFamily = interFamily,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(spacerWidth))
        }

        transition.AnimatedVisibility(
            visible = { it },
            modifier = Modifier
                .align(Alignment.CenterEnd),
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .height(with(LocalDensity.current) { rowHeight.toDp() })
                    .width(56.dp)
                    .background(Color(0xFF22C55E), RoundedCornerShape(10.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showAction = false
                        onClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.band_connect),
                    tint = Color.Black
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectBandScreenPreview() {
    EngineeringThesisTheme {
        TopConnectScreenBar()
    }
}
