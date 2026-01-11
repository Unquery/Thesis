package pl.edu.pjwstk.engineeringthesis.view

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.font.interFamily
import pl.edu.pjwstk.engineeringthesis.ui.theme.DarkGray700
import pl.edu.pjwstk.engineeringthesis.ui.theme.DarkGray850
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import pl.edu.pjwstk.engineeringthesis.viewmodel.MenuViewModel


@Composable
fun MenuScreen(
    onConnectBandClick: () -> Unit,
    vm: MenuViewModel = hiltViewModel()
) {
    val gsrBars by vm.todayGsrBars.collectAsState()
    val hrBars by vm.todayHrBars.collectAsState()
    val spo2Bars by vm.todaySpo2Bars.collectAsState()
    val tempBars by vm.todayTempBars.collectAsState()

    var connectBandMenuOpen by remember { mutableStateOf(false) }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (connectBandMenuOpen) 0.35f else 0f,
        label = stringResource(R.string.menu_alpha)
    )
    Box(Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopMenuBar { connectBandMenuOpen = !connectBandMenuOpen }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color.Black)
                )
            }

        ) { inner ->
            Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .background(Color.Black)
            ){
                MenuBody(
                    gsrBars = gsrBars,
                    hrBars = hrBars,
                    spo2Bars = spo2Bars,
                    tempBars = tempBars
                )
            }
        }

        if (scrimAlpha > 0f) {
            AlphaOverlay(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { connectBandMenuOpen = false }
            )
        }

        AnimatedVisibility(
            visible = connectBandMenuOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Box(Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 60.dp, end = 18.dp)
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    color = DarkGray850,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column{
                        MenuActionItem(
                            text = stringResource(R.string.connect_band_menu_button),
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                            onClick = {
                                connectBandMenuOpen = false
                                onConnectBandClick()
                            }
                        )

                        MenuActionItem(
                            text = stringResource(R.string.connect_band_menu_button),
                            shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                            onClick = { /* TODO: navigate to settings or any action */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuBody(
    gsrBars: List<Float?>,
    hrBars: List<Float?>,
    spo2Bars: List<Float?>,
    tempBars: List<Float?>
) {
    val lastTemp = tempBars.lastOrNull { it != null }
    val lastHr = hrBars.lastOrNull { it != null }
    val lastSpo2 = spo2Bars.lastOrNull { it != null }
    val lastGsr = gsrBars.lastOrNull { it != null }

    val lastItems = listOf(
        MeasurementCircleItem(
            title = "Body temperature",
            value = lastTemp?.toDouble(),
            unit = "C",
            trendText = "- stable",
            icon = Icons.Filled.DeviceThermostat,
            baseColor = Color(0xFFF59E0B),
            normalMin = 36.1,
            normalMax = 37.2,
            criticalMin = 34.0,
            criticalMax = 41.0,
            decimals = 1
        ),
        MeasurementCircleItem(
            title = "Heart rate",
            value = lastHr?.toDouble(),
            unit = "bpm",
            trendText = "- stable",
            icon = Icons.Filled.MonitorHeart,
            baseColor = Color(0xFFE53935),
            normalMin = 60.0,
            normalMax = 100.0,
            criticalMin = 30.0,
            criticalMax = 200.0,
            decimals = 0
        ),
        MeasurementCircleItem(
            title = "Blood oxygen",
            value = lastSpo2?.toDouble(),
            unit = "%",
            trendText = "- stable",
            icon = Icons.Filled.Bloodtype,
            baseColor = Color(0xFF0284C7),
            normalMin = 95.0,
            normalMax = 100.0,
            criticalMin = 70.0,
            criticalMax = 100.0,
            decimals = 0
        ),
        MeasurementCircleItem(
            title = "Skin conductance",
            value = lastGsr?.toDouble(),
            unit = "uS",
            trendText = "- stable",
            icon = Icons.Filled.SsidChart,
            baseColor = Color(0xFF6366F1),
            normalMin = 200.0,
            normalMax = 900.0,
            criticalMin = 0.0,
            criticalMax = 2000.0,
            decimals = 0
        )
    )

    Column(Modifier.fillMaxSize()) {
        LastMeasurementsCirclesBox(
            items = lastItems,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        MenuMetricsColumn(
            gsrBars = gsrBars,
            hrBars = hrBars,
            spo2Bars = spo2Bars,
            tempBars = tempBars,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun MenuMetricsColumn(
    gsrBars: List<Float?>,
    hrBars: List<Float?>,
    spo2Bars: List<Float?>,
    tempBars: List<Float?>,
    onGsrClick: () -> Unit = {},
    onHrClick: () -> Unit = {},
    onSpo2Click: () -> Unit = {},
    onTempClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = modifier) {
                Box(Modifier.weight(0.5f)) {
                    MetricBox24h(
                        title = "Body temperature",
                        unit = "temperature",
                        bars = tempBars,
                        onClick = onTempClick,
                        valueFormatter = { v -> String.format("%.1f", v) },
                        nullAsZero = false,
                        scaleFromMin = true,
                        maxBarRatio = 0.85f,
                        barColor = Color(0xFFF59E0B),
                        icon = Icons.Filled.DeviceThermostat,
                        fullHeightBars = true,
                        showMinMaxLabels = false,
                        referenceValue = 36.6f,
                        referenceRange = 0.5f,
                        colorCurve = 1.2f,
                        colorStrength = 1.4f,
                        lowColorMix = 0.5f,
                        highColorMix = 0.75f
                    )
                }
                Spacer(Modifier.width(5.dp))

                Box(Modifier.weight(0.5f)) {
                    MetricBox24h(
                        title = "Heart rate",
                        unit = "rate",
                        bars = hrBars,
                        onClick = onHrClick,
                        valueFormatter = { it.toInt().toString() },
                        nullAsZero = false,
                        scaleFromMin = false,
                        maxBarRatio = 0.80f,
                        barColor = Color(0xFFE53935),
                        icon = Icons.Filled.MonitorHeart,
                        fullHeightBars = true,
                        showMinMaxLabels = false,
                        useReferenceGradient = true,
                        referenceValue = 70f,
                        referenceRange = 15f,
                        colorCurve = 1.3f,
                        colorStrength = 1.2f,
                        highColorMix = 0.65f,
                        lowColorMix = 0.3f
                    )
                }
            }
        }

        item {
            Row {
                val spo2Min =
                    (spo2Bars.filterNotNull().minOrNull()?.minus(1f) ?: 0f).coerceAtLeast(0f)
                Box(Modifier.weight(0.5f)) {
                    MetricBox24h(
                        title = "Blood oxygen",
                        unit = "oxygen",
                        bars = spo2Bars,
                        onClick = onSpo2Click,
                        valueFormatter = { it.toInt().toString() },
                        nullAsZero = false,
                        scaleFromMin = true,
                        yMinOverride = spo2Min,
                        yMaxOverride = 100f,
                        barColor = Color(0xFF0284C7),
                        icon = Icons.Filled.Bloodtype,
                        fullHeightBars = true,
                        showMinMaxLabels = false,
                        useReferenceGradient = true,
                        referenceValue = 98f,
                        referenceRange = 7.0f,
                        colorCurve = 1.3f,
                        colorStrength = 1.3f,
                        highColorMix = 0.25f,
                        lowColorMix = 0.4f,
                        lowColorTarget = Color.Black
                    )
                }
                Spacer(Modifier.width(5.dp))

                Box(Modifier.weight(0.5f)) {
                    MetricBox24h(
                        title = "Skin conductance",
                        unit = "conductance",
                        bars = gsrBars,
                        onClick = onGsrClick,
                        valueFormatter = { it.toInt().toString() },
                        nullAsZero = false,
                        scaleFromMin = false,
                        maxBarRatio = 0.80f,
                        barColor = Color(0xFF6366F1),
                        icon = Icons.Filled.SsidChart,
                        fullHeightBars = true,
                        showMinMaxLabels = false,
                        lowColorMix = 0.14f,
                        highColorMix = 0.14f,
                        colorCurve = 2.2f
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuActionItem(
    text: String,
    shape: Shape,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Surface(
        modifier = Modifier
            .clip(shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = if (pressed) DarkGray700 else DarkGray850
    ) {
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .heightIn(min = 56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AlphaOverlay(modifier: Modifier){
    Box(
        modifier = modifier
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopMenuBar(
    onPlusClicked : () -> Unit
){
    Box {
        TopAppBar(
            title = {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.main_screen_title),
                        fontFamily = interFamily,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        fontSize = 40.sp
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            navigationIcon = { /* no impl */ },
            actions = {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onPlusClicked
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.connect_band_menu_button),
                            tint = Color.White
                        )
                    }
                }
            },
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
private fun MenuBodyPreview() {
    val tempBars = List(24) { i -> 36.3f + (i % 6) * 0.1f }
    val hrBars = List(24) { i -> 58f + (i % 8) * 3f }
    val spo2Bars = List(24) { i -> 93f + (i % 6) * 1f }
    val gsrBars = List(24) { i -> 200f + (i % 10) * 40f }

    EngineeringThesisTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(12.dp)
        ) {
            MenuBody(
                gsrBars = gsrBars,
                hrBars = hrBars,
                spo2Bars = spo2Bars,
                tempBars = tempBars
            )
        }
    }
}




