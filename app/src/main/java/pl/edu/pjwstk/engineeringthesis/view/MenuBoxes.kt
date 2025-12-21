package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp

private fun medianOf(values: List<Float>): Float {
    if (values.isEmpty()) return 0f
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2f else sorted[mid]
}

private fun safeDiv(num: Float, den: Float): Float = if (den == 0f) 0f else num / den

@Composable
fun MetricBox24h(
    title: String,
    bars: List<Float?>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,

    // appearance
    cardHeight: Dp = 190.dp,
    chartHeight: Dp = 130.dp,
    barColor: Color = Color(0xFF3F51B5),
    columnBgColor: Color = Color.Black.copy(alpha = 0.08f),
    maxBarRatio: Float = 0.8f,
    scaleFromMin: Boolean = false,
    yMinOverride: Float? = null,
    yMaxOverride: Float? = null,
    useMedianGradient: Boolean = true,
    lowColorMix: Float = 0.13f,
    highColorMix: Float = 0.13f,

    // labels
    leftTimeLabel: String = "00:00",
    rightTimeLabel: String = "24:00",

    // label formatting (temp: { "%.1f".format(it) }, HR: { it.toInt().toString() })
    valueFormatter: (Float) -> String = { v -> v.toInt().toString() },

    // how to treat empty data
    nullAsZero: Boolean = true,
    showMinMaxLabels: Boolean = true,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title)
            if (!unit.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(8.dp))

            val density = LocalDensity.current
            val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                val rightLabelPadPx = with(density) { 46.dp.toPx() }  // space for min/max labels
                val bottomLabelPadPx = with(density) { 18.dp.toPx() } // space for time labels

                val chartLeft = 0f
                val chartTop = 0f
                val chartRight = size.width - rightLabelPadPx
                val chartBottom = size.height - bottomLabelPadPx

                val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
                val chartHeightPx = (chartBottom - chartTop).coerceAtLeast(1f)

                val normalizedBars = bars.take(24).let { list ->
                    if (list.size < 24) list + List(24 - list.size) { null } else list
                }

                val vals = normalizedBars.map { v ->
                    when {
                        v == null && nullAsZero -> 0f
                        v == null -> Float.NaN
                        else -> v
                    }
                }

                val finiteVals = vals.filter { it.isFinite() }

                val realMin = finiteVals.minOrNull() ?: 0f
                val realMax = finiteVals.maxOrNull() ?: 0f
                val med = medianOf(finiteVals)


                val lowColor = lerp(barColor, Color.White, lowColorMix.coerceIn(0f, 1f))
                val highColor = lerp(barColor, Color.Black, highColorMix.coerceIn(0f, 1f))

                val baseMin = when {
                    yMinOverride != null -> yMinOverride
                    scaleFromMin -> realMin
                    else -> 0f
                }

                val topMax = when {
                    yMaxOverride != null -> yMaxOverride
                    else -> {
                        val safeRatio = maxBarRatio.coerceIn(0.05f, 0.95f)
                        val range = (realMax - baseMin).takeIf { it > 0f } ?: 1f
                        baseMin + (range / safeRatio)
                    }
                }

                val scaleMin = baseMin
                val scaleMax = topMax
                val scaleRange = (scaleMax - scaleMin).takeIf { it > 0f } ?: 1f

                val n = 24
                val gap = chartWidth * 0.006f
                val w = (chartWidth - gap * (n - 1)) / n

                for (i in 0 until n) {
                    val v = vals.getOrNull(i)
                    val barValue = if (v == null || !v.isFinite()) 0f else v
                    val ratio = ((barValue - scaleMin) / scaleRange).coerceIn(0f, 1f)
                    val h = chartHeightPx * ratio

                    val x = chartLeft + i * (w + gap)
                    val y = chartTop + (chartHeightPx - h)

                    val c = if (!useMedianGradient || finiteVals.isEmpty()) {
                        barColor
                    } else {
                        if (barValue <= med) {
                            // realMin -> lowColor, median -> barColor
                            val tLow = safeDiv(barValue - realMin, (med - realMin)).coerceIn(0f, 1f)
                            lerp(lowColor, barColor, tLow)
                        } else {
                            // median -> barColor, realMax -> highColor
                            val tHigh = safeDiv(barValue - med, (realMax - med)).coerceIn(0f, 1f)
                            lerp(barColor, highColor, tHigh)
                        }
                    }

                    // background column
                    drawRect(
                        color = columnBgColor,
                        topLeft = Offset(x, chartTop),
                        size = Size(w, chartHeightPx)
                    )

                    // bar
                    drawRect(
                        color = c,
                        topLeft = Offset(x, y),
                        size = Size(w, h)
                    )
                }

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = labelColor.toArgb()
                        textSize = with(density) { 11.sp.toPx() }
                    }

                    // right side labels (max top, min bottom)
                    if (showMinMaxLabels && finiteVals.isNotEmpty()) {
                        val rightX = chartRight + with(density) { 6.dp.toPx() }

                        canvas.nativeCanvas.drawText(
                            valueFormatter(scaleMin),
                            rightX,
                            chartBottom, // baseline near bottom
                            paint
                        )

                        canvas.nativeCanvas.drawText(
                            valueFormatter(scaleMax),
                            rightX,
                            chartTop + paint.textSize, // baseline near top
                            paint
                        )
                    }

                    // bottom time labels
                    val timeY = size.height
                    canvas.nativeCanvas.drawText(leftTimeLabel, chartLeft, timeY, paint)

                    val rightTextWidth = paint.measureText(rightTimeLabel)
                    canvas.nativeCanvas.drawText(
                        rightTimeLabel,
                        chartRight - rightTextWidth,
                        timeY,
                        paint
                    )
                }
            }
        }
    }
}
