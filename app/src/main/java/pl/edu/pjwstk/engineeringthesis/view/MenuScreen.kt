package pl.edu.pjwstk.engineeringthesis.view

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.util.ChartMetric
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import pl.edu.pjwstk.engineeringthesis.viewmodel.MenuViewModel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow


@Composable
fun MenuScreen(
    onConnectBandClick: () -> Unit,
    onHealthClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onMetricClick: (ChartMetric) -> Unit = {},
    vm: MenuViewModel = hiltViewModel()
) {
    val gsrBars by vm.todayGsrBars.collectAsState()
    val hrBars by vm.todayHrBars.collectAsState()
    val spo2Bars by vm.todaySpo2Bars.collectAsState()
    val tempBars by vm.todayTempBars.collectAsState()
    val lastUpdatedEpoch by vm.todayLatestEpoch.collectAsState()

    Box(Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BottomNavBar(
                    onHealthClick = onHealthClick,
                    onDeviceClick = onConnectBandClick,
                    onProfileClick = onProfileClick
                )
            }

        ) { inner ->
            Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ){
                MenuBody(
                    gsrBars = gsrBars,
                    hrBars = hrBars,
                    spo2Bars = spo2Bars,
                    tempBars = tempBars,
                    lastUpdatedEpoch = lastUpdatedEpoch,
                    onMetricClick = onMetricClick
                )
            }
        }
    }
}

@Composable
private fun MenuBody(
    gsrBars: List<Float?>,
    hrBars: List<Float?>,
    spo2Bars: List<Float?>,
    tempBars: List<Float?>,
    lastUpdatedEpoch: Long?,
    onMetricClick: (ChartMetric) -> Unit
) {
    var stretchTarget by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val maxStretch = with(LocalDensity.current) { 90.dp.toPx() }
    val stretch by animateFloatAsState(
        targetValue = stretchTarget,
        animationSpec = if (isDragging) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "menuStretch"
    )
    val lastTemp = tempBars.lastOrNull { it != null }
    val lastHr = hrBars.lastOrNull { it != null }
    val lastSpo2 = spo2Bars.lastOrNull { it != null }
    val lastGsr = gsrBars.lastOrNull { it != null }

    val headerSubtitle = formatUpdatedSubtitle(lastUpdatedEpoch)

    val tempTrend = trendTextFromBars(tempBars, stringResource(R.string.unit_celsius), 1)
    val hrTrend = trendTextFromBars(hrBars, stringResource(R.string.unit_bpm), 0)
    val spo2Trend = trendTextFromBars(spo2Bars, stringResource(R.string.unit_percent), 0)
    val gsrTrend = trendTextFromBars(gsrBars, stringResource(R.string.unit_us), 0)

    val lastItems = listOf(
        MeasurementCircleItem(
            title = stringResource(R.string.metric_body_temperature),
            value = lastTemp?.toDouble(),
            unit = stringResource(R.string.unit_celsius),
            trendText = tempTrend,
            icon = Icons.Filled.DeviceThermostat,
            baseColor = Color(0xFFF59E0B),
            normalMin = 36.1,
            normalMax = 37.2,
            criticalMin = 34.0,
            criticalMax = 41.0,
            decimals = 1
        ),
        MeasurementCircleItem(
            title = stringResource(R.string.metric_heart_rate),
            value = lastHr?.toDouble(),
            unit = stringResource(R.string.unit_bpm),
            trendText = hrTrend,
            icon = Icons.Filled.MonitorHeart,
            baseColor = Color(0xFFE53935),
            normalMin = 60.0,
            normalMax = 100.0,
            criticalMin = 30.0,
            criticalMax = 200.0,
            decimals = 0
        ),
        MeasurementCircleItem(
            title = stringResource(R.string.metric_blood_oxygen),
            value = lastSpo2?.toDouble(),
            unit = stringResource(R.string.unit_percent),
            trendText = spo2Trend,
            icon = Icons.Filled.Bloodtype,
            baseColor = Color(0xFF0284C7),
            normalMin = 95.0,
            normalMax = 100.0,
            criticalMin = 70.0,
            criticalMax = 100.0,
            decimals = 0
        ),
        MeasurementCircleItem(
            title = stringResource(R.string.metric_skin_conductance),
            value = lastGsr?.toDouble(),
            unit = stringResource(R.string.unit_us),
            trendText = gsrTrend,
            icon = Icons.Filled.SsidChart,
            baseColor = Color(0xFF6366F1),
            normalMin = 200.0,
            normalMax = 900.0,
            criticalMin = 0.0,
            criticalMax = 2000.0,
            decimals = 0
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        stretchTarget = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        stretchTarget = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val next = (stretchTarget + dragAmount * 0.5f).coerceIn(-maxStretch, maxStretch)
                    stretchTarget = next
                }
            }
            .graphicsLayer {
                translationY = stretch
                val stretchRatio = if (maxStretch > 0f) abs(stretch) / maxStretch else 0f
                scaleY = 1f + stretchRatio * 0.05f
                transformOrigin = if (stretch >= 0f) {
                    TransformOrigin(0.5f, 0f)
                } else {
                    TransformOrigin(0.5f, 1f)
                }
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LastMeasurementsCirclesBox(
            items = lastItems,
            modifier = Modifier.fillMaxWidth(),
            headerTitle = stringResource(R.string.menu_last_measurements),
            headerSubtitle = headerSubtitle
        )

        MenuMetricsColumn(
            gsrBars = gsrBars,
            hrBars = hrBars,
            spo2Bars = spo2Bars,
            tempBars = tempBars,
            onGsrClick = { onMetricClick(ChartMetric.Gsr) },
            onHrClick = { onMetricClick(ChartMetric.HeartRate) },
            onSpo2Click = { onMetricClick(ChartMetric.SpO2) },
            onTempClick = { onMetricClick(ChartMetric.Temperature) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    onHealthClick: () -> Unit = {},
    onDeviceClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    healthTint: Color = Color(0xFFE53935),
    deviceTint: Color = Color.White,
    profileTint: Color = Color.White
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0F172A),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp, top = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Filled.MonitorHeart,
                label = stringResource(R.string.bottom_nav_health),
                onClick = onHealthClick,
                iconTint = healthTint,
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                icon = Icons.Filled.Devices,
                label = stringResource(R.string.bottom_nav_device),
                onClick = onDeviceClick,
                iconTint = deviceTint,
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                icon = Icons.Filled.Person,
                label = stringResource(R.string.bottom_nav_profile),
                onClick = onProfileClick,
                iconTint = profileTint,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = Color.White,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
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
    val leftTimeLabel = stringResource(R.string.time_00_00)
    val rightTimeLabel = stringResource(R.string.time_24_00)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(0.5f)) {
                MetricBox24h(
                    title = stringResource(R.string.metric_body_temperature),
                    unit = stringResource(R.string.unit_temperature_label),
                    leftTimeLabel = leftTimeLabel,
                    rightTimeLabel = rightTimeLabel,
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
                    title = stringResource(R.string.metric_heart_rate),
                    unit = stringResource(R.string.unit_rate_label),
                    leftTimeLabel = leftTimeLabel,
                    rightTimeLabel = rightTimeLabel,
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

        Row(modifier = Modifier.fillMaxWidth()) {
            val spo2Min =
                (spo2Bars.filterNotNull().minOrNull()?.minus(1f) ?: 0f).coerceAtLeast(0f)
            Box(Modifier.weight(0.5f)) {
                MetricBox24h(
                    title = stringResource(R.string.metric_blood_oxygen),
                    unit = stringResource(R.string.unit_oxygen_label),
                    leftTimeLabel = leftTimeLabel,
                    rightTimeLabel = rightTimeLabel,
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
                    title = stringResource(R.string.metric_skin_conductance),
                    unit = stringResource(R.string.unit_conductance_label),
                    leftTimeLabel = leftTimeLabel,
                    rightTimeLabel = rightTimeLabel,
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

@Composable
private fun trendTextFromBars(
    bars: List<Float?>,
    unit: String,
    decimals: Int
): String? {
    val lastIndex = bars.indexOfLast { it != null }
    if (lastIndex <= 0) return null

    val last = bars[lastIndex] ?: return null
    val prevIndex = (lastIndex - 1 downTo 0).firstOrNull { bars[it] != null } ?: return null
    val prev = bars[prevIndex] ?: return null

    return buildTrendText(prev.toDouble(), last.toDouble(), unit, decimals)
}

@Composable
private fun buildTrendText(
    previous: Double,
    current: Double,
    unit: String,
    decimals: Int
): String {
    val diff = current - previous
    val threshold = 0.5 * 10.0.pow(-decimals.toDouble())
    if (abs(diff) < threshold) return stringResource(R.string.trend_stable)

    val absDiff = abs(diff)
    val diffText = formatNumber(absDiff, decimals)
    val sign = if (diff > 0) "+" else "-"

    return stringResource(R.string.trend_change_format, sign, diffText, unit)
}

private fun formatNumber(value: Double, decimals: Int): String {
    val safeDecimals = decimals.coerceIn(0, 4)
    return String.format(Locale.US, "%.${safeDecimals}f", value)
}

@Composable
private fun formatUpdatedSubtitle(epochMillis: Long?): String {
    if (epochMillis == null) return stringResource(R.string.menu_no_data_yet)

    val now = System.currentTimeMillis()
    val diff = (now - epochMillis).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> stringResource(R.string.menu_updated_just_now)
        diff < 3_600_000L -> stringResource(R.string.menu_updated_minutes_ago, diff / 60_000L)
        diff < 86_400_000L -> stringResource(R.string.menu_updated_hours_ago, diff / 3_600_000L)
        else -> stringResource(R.string.menu_updated_days_ago, diff / 86_400_000L)
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
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            MenuBody(
                gsrBars = gsrBars,
                hrBars = hrBars,
                spo2Bars = spo2Bars,
                tempBars = tempBars,
                lastUpdatedEpoch = System.currentTimeMillis(),
                onMetricClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavBarPreview() {
    EngineeringThesisTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            BottomNavBar()
        }
    }
}




