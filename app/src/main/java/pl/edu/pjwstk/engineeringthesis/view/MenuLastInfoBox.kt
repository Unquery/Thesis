package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.font.interFamily
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import kotlin.math.abs

data class MeasurementCircleItem(
    val title: String,
    val value: Double?,
    val unit: String,
    val valueText: String? = null,
    val trendText: String? = null,
    val icon: ImageVector,
    val baseColor: Color,
    val normalMin: Double,
    val normalMax: Double,
    val criticalMin: Double,
    val criticalMax: Double,
    val decimals: Int = 1,
    val colorForValue: ((Double?) -> Color)? = null
)

@Composable
fun LastMeasurementsCirclesBox(
    items: List<MeasurementCircleItem>,
    modifier: Modifier = Modifier,
    headerTitle: String,
    headerSubtitle: String,
    columns: Int = 2,
    circleSize: Dp = 110.dp,
    circleStroke: Dp = 4.dp,
    iconSize: Dp = 22.dp,
    valueSize: TextUnit = 18.sp,
    fontFamily: FontFamily = interFamily,
) {
    if (items.isEmpty()) return

    val panelShape = RoundedCornerShape(16.dp)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = panelShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerTitle,
                    fontFamily = fontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = headerSubtitle,
                    fontFamily = fontFamily,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            val safeColumns = columns.coerceAtLeast(1)
            val rows = items.chunked(safeColumns)

            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    rowItems.forEach { item ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            MeasurementCircle(
                                item = item,
                                circleSize = circleSize,
                                circleStroke = circleStroke,
                                iconSize = iconSize,
                                valueSize = valueSize,
                                fontFamily = fontFamily
                            )
                        }
                    }

                    repeat(safeColumns - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementCircle(
    item: MeasurementCircleItem,
    circleSize: Dp,
    circleStroke: Dp,
    iconSize: Dp,
    valueSize: TextUnit,
    fontFamily: FontFamily
) {
    val dynamic = dynamicRangeColor(
        value = item.value,
        base = item.baseColor,
        normalMin = item.normalMin,
        normalMax = item.normalMax,
        criticalMin = item.criticalMin,
        criticalMax = item.criticalMax
    )
    val borderColor = item.colorForValue?.invoke(item.value) ?: dynamic

    Box(
        modifier = Modifier
            .size(circleSize)
            .background(item.baseColor.copy(alpha = 0.12f), CircleShape)
            .border(circleStroke, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.baseColor,
                modifier = Modifier.size(iconSize)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = item.valueText ?: (formatValue(item.value, item.decimals) + " " + item.unit),
                fontFamily = fontFamily,
                fontSize = valueSize,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            val trend = item.trendText
            if (!trend.isNullOrBlank()) {

                Text(
                    text = trend,
                    fontFamily = fontFamily,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun dynamicRangeColor(
    value: Double?,
    base: Color,
    normalMin: Double,
    normalMax: Double,
    criticalMin: Double,
    criticalMax: Double
): Color {
    if (value == null) return base

    val safeNormalMin = minOf(normalMin, normalMax)
    val safeNormalMax = maxOf(normalMin, normalMax)
    val safeCriticalMin = minOf(criticalMin, safeNormalMin)
    val safeCriticalMax = maxOf(criticalMax, safeNormalMax)

    return when {
        value < safeNormalMin -> {
            val denom = (safeNormalMin - safeCriticalMin).takeIf { abs(it) > 1e-9 } ?: 1.0
            val t = ((safeNormalMin - value) / denom).toFloat().coerceIn(0f, 1f)
            lerp(base, Color.White, t)
        }
        value > safeNormalMax -> {
            val denom = (safeCriticalMax - safeNormalMax).takeIf { abs(it) > 1e-9 } ?: 1.0
            val t = ((value - safeNormalMax) / denom).toFloat().coerceIn(0f, 1f)
            lerp(base, Color.Black, t)
        }
        else -> base
    }
}

@Composable
private fun formatValue(v: Double?, decimals: Int): String {
    if (v == null) return stringResource(R.string.value_placeholder)

    val d = decimals.coerceIn(0, 4)
    return "%.${d}f".format(v)
}

@Preview(showBackground = true)
@Composable
private fun LastMeasurementsCirclesBoxPreview() {
    EngineeringThesisTheme {
        LastMeasurementsCirclesBox(
            modifier = Modifier.padding(16.dp),
            headerTitle = stringResource(R.string.menu_last_measurements),
            headerSubtitle = stringResource(R.string.menu_updated_minutes_ago, 2),
            items = listOf(
                MeasurementCircleItem(
                    title = stringResource(R.string.metric_body_temperature),
                    value = 36.7,
                    unit = stringResource(R.string.unit_celsius),
                    trendText = stringResource(R.string.trend_stable),
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
                    value = 72.0,
                    unit = stringResource(R.string.unit_bpm),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "+",
                        "3",
                        stringResource(R.string.unit_bpm)
                    ),
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
                    value = 98.0,
                    unit = stringResource(R.string.unit_percent),
                    trendText = stringResource(R.string.trend_stable),
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
                    value = 512.0,
                    unit = stringResource(R.string.unit_us),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "-",
                        "12",
                        stringResource(R.string.unit_us)
                    ),
                    icon = Icons.Filled.SsidChart,
                    baseColor = Color(0xFF6366F1),
                    normalMin = 200.0,
                    normalMax = 900.0,
                    criticalMin = 0.0,
                    criticalMax = 2000.0,
                    decimals = 0
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LastMeasurementsCirclesBoxLowValuesPreview() {
    EngineeringThesisTheme {
        LastMeasurementsCirclesBox(
            modifier = Modifier.padding(16.dp),
            headerTitle = stringResource(R.string.menu_last_measurements),
            headerSubtitle = stringResource(R.string.menu_updated_minutes_ago, 2),
            items = listOf(
                MeasurementCircleItem(
                    title = stringResource(R.string.metric_body_temperature),
                    value = 35.0,
                    unit = stringResource(R.string.unit_celsius),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "-",
                        "0.5",
                        stringResource(R.string.unit_celsius)
                    ),
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
                    value = 45.0,
                    unit = stringResource(R.string.unit_bpm),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "-",
                        "15",
                        stringResource(R.string.unit_bpm)
                    ),
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
                    value = 88.0,
                    unit = stringResource(R.string.unit_percent),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "-",
                        "6",
                        stringResource(R.string.unit_percent)
                    ),
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
                    value = 120.0,
                    unit = stringResource(R.string.unit_us),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "-",
                        "80",
                        stringResource(R.string.unit_us)
                    ),
                    icon = Icons.Filled.SsidChart,
                    baseColor = Color(0xFF6366F1),
                    normalMin = 200.0,
                    normalMax = 900.0,
                    criticalMin = 0.0,
                    criticalMax = 2000.0,
                    decimals = 0
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LastMeasurementsCirclesBoxHighValuesPreview() {
    EngineeringThesisTheme {
        LastMeasurementsCirclesBox(
            modifier = Modifier.padding(16.dp),
            headerTitle = stringResource(R.string.menu_last_measurements),
            headerSubtitle = stringResource(R.string.menu_updated_minutes_ago, 2),
            items = listOf(
                MeasurementCircleItem(
                    title = stringResource(R.string.metric_body_temperature),
                    value = 39.0,
                    unit = stringResource(R.string.unit_celsius),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "+",
                        "1.8",
                        stringResource(R.string.unit_celsius)
                    ),
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
                    value = 130.0,
                    unit = stringResource(R.string.unit_bpm),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "+",
                        "45",
                        stringResource(R.string.unit_bpm)
                    ),
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
                    value = 100.0,
                    unit = stringResource(R.string.unit_percent),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "+",
                        "3",
                        stringResource(R.string.unit_percent)
                    ),
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
                    value = 1400.0,
                    unit = stringResource(R.string.unit_us),
                    trendText = stringResource(
                        R.string.trend_change_format,
                        "+",
                        "420",
                        stringResource(R.string.unit_us)
                    ),
                    icon = Icons.Filled.SsidChart,
                    baseColor = Color(0xFF6366F1),
                    normalMin = 200.0,
                    normalMax = 900.0,
                    criticalMin = 0.0,
                    criticalMax = 2000.0,
                    decimals = 0
                )
            )
        )
    }
}
