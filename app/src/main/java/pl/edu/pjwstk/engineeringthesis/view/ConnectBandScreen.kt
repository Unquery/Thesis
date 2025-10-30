package pl.edu.pjwstk.engineeringthesis.view

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.font.interFamily
import pl.edu.pjwstk.engineeringthesis.ui.theme.DarkGray850
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import pl.edu.pjwstk.engineeringthesis.viewmodel.Band
import pl.edu.pjwstk.engineeringthesis.viewmodel.BluetoothPermissionViewModel
import pl.edu.pjwstk.engineeringthesis.viewmodel.ConnectBandViewModel
import pl.edu.pjwstk.engineeringthesis.viewmodel.ScanUiState
import java.util.Locale

@Composable
fun ConnectBandScreen(
    vmBluetoothPermission : BluetoothPermissionViewModel = hiltViewModel(),
    vmConnectBand : ConnectBandViewModel = hiltViewModel()
) {

    val ctx = LocalContext.current
    val state by vmBluetoothPermission.state.collectAsStateWithLifecycle()

    val temps by vmConnectBand.temps.collectAsStateWithLifecycle()
    val scanState by vmConnectBand.scanState.collectAsStateWithLifecycle()
    val bands by vmConnectBand.bands.collectAsStateWithLifecycle()
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

    LaunchedEffect(state.hasPermissions, state.bluetoothOn) {
        when {
            !state.hasPermissions -> {
                launcher.launch(vmBluetoothPermission.requiredBluetoothPermissions())
            }
            state.hasPermissions && !state.bluetoothOn -> {
                enableBt.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            state.hasPermissions && state.bluetoothOn -> {
                vmConnectBand.startAfterPermissionsGranted()
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
                ScanStatusPanel(
                    modifier = Modifier.fillMaxSize(),
                    state = scanState,
                    bands = bands,
                    onBandClick = { vmConnectBand.connectTo(it.address) },
                    onRepeat = { vmConnectBand.repeatScan() }
                )
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
                containerColor = Color.Black,
                scrolledContainerColor = Color.Black,
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
        modifier.fillMaxSize().background(Color(0xFF2E2E2E)).padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (state) {
                    ScanUiState.Scanning  -> "Scanning…"
                    ScanUiState.Connected -> "Connected"
                    ScanUiState.Empty     -> "Found ${bands.size} bands"
                    ScanUiState.Idle      -> "Idle"
                },
                color = Color.White,
                fontFamily = interFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            )

            if (state != ScanUiState.Scanning) {
                Button(onClick = onRepeat) { Text("Repeat scanning") }
            }
        }

        if (state == ScanUiState.Scanning) {
            val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))
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
                text = "Try moving closer or tapping repeat.",
                color = Color.White, fontFamily = interFamily
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BandRow(band: Band, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF3A3A3A), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = band.name ?: "Unknown", color = Color.White, fontFamily = interFamily, fontWeight = FontWeight.SemiBold)
            Text(text = band.address, color = Color.LightGray, fontFamily = interFamily, fontSize = 12.sp)
        }
        Text(text = "${band.rssi} dBm", color = Color.White, fontFamily = interFamily)
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectBandScreenPreview() {
    EngineeringThesisTheme {
        TopConnectScreenBar()
    }
}