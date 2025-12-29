package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import pl.edu.pjwstk.engineeringthesis.font.interFamily
import androidx.compose.ui.geometry.CornerRadius
import kotlin.math.pow

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

    cardHeight: Dp = 190.dp,
    chartHeight: Dp = 60.dp,
    barColor: Color = Color(0xFF3F51B5),
    columnBgColor: Color = Color.Black.copy(alpha = 0.08f),
    maxBarRatio: Float = 0.8f,
    scaleFromMin: Boolean = false,
    yMinOverride: Float? = null,
    yMaxOverride: Float? = null,
    useMedianGradient: Boolean = true,
    lowColorMix: Float = 0.5f,
    highColorMix: Float = 0.5f,
    icon: ImageVector? = null,
    fontFamily: FontFamily = interFamily,
    titleSize: TextUnit = 14.sp,
    fullHeightBars: Boolean = false,
    barHeightFraction: Float = 0.75f,
    colorCurve: Float = 2.0f,
    useReferenceGradient: Boolean = false,
    referenceValue: Float? = null,
    referenceRange: Float = 1f,
    colorStrength: Float = 1.0f,
    lowColorTarget : Color = Color.White,

    leftTimeLabel: String = "00:00",
    rightTimeLabel: String = "24:00",

    valueFormatter: (Float) -> String = { v -> v.toInt().toString() },

    nullAsZero: Boolean = true,
    showMinMaxLabels: Boolean = true,
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .height(cardHeight)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 12.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = titleSize, fontFamily = fontFamily, style = MaterialTheme.typography.bodyLarge)
            }
            if (!unit.isNullOrBlank() && !fullHeightBars) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = unit,
                    fontFamily = fontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }else{
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "See detailed information about $unit",
                        fontFamily = fontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.9f))

            val density = LocalDensity.current
            val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

            // --- precompute scale + colors OUTSIDE canvas (so we can show labels in Column) ---
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

            val lowColor = lerp(barColor, lowColorTarget, lowColorMix.coerceIn(0f, 1f))
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

            Row(
                modifier = Modifier.fillMaxWidth().requiredHeight(chartHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val bottomLabelPadPx = with(density) { 18.dp.toPx() }

                    val chartLeft = 0f
                    val chartTop = 0f
                    val chartRight = size.width
                    val chartBottom = size.height - bottomLabelPadPx

                    val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
                    val chartHeightPx = (chartBottom - chartTop).coerceAtLeast(1f)
                    val barAreaHeightPx = chartHeightPx * barHeightFraction.coerceIn(0.1f, 1f)
                    val barAreaTop = chartTop + (chartHeightPx - barAreaHeightPx)

                    val n = 24
                    val gap = with(density) { 4.dp.toPx() }
                    val w = (chartWidth - gap * (n - 1)) / n

                    for (i in 0 until n) {
                        val raw = vals.getOrNull(i)
                        val isMissing = raw == null || !raw.isFinite()
                        val barValue = if (isMissing) 0f else raw

                        val ratio = if (fullHeightBars) 1f else ((barValue - scaleMin) / scaleRange).coerceIn(0f, 1f)
                        val h = barAreaHeightPx * ratio

                        val x = chartLeft + i * (w + gap)
                        val y = barAreaTop + (barAreaHeightPx - h)

                        val c = when {
                            isMissing || finiteVals.isEmpty() -> barColor

                            useReferenceGradient && referenceValue != null -> {
                                val ref = referenceValue
                                val range = referenceRange.coerceAtLeast(0.0001f)
                                val z = ((barValue - ref) / range).coerceIn(-1f, 1f)
                                val m = kotlin.math.abs(z).pow(colorCurve)
                                val m2 = (m * colorStrength).coerceIn(0f, 1f)

                                if (z >= 0f) {
                                    lerp(barColor, highColor, m2)
                                } else {
                                    lerp(barColor, lowColor, m2)
                                }
                            }

                            useMedianGradient -> {
                                if (barValue <= med) {
                                    val tLow = safeDiv(barValue - realMin, (med - realMin))
                                        .coerceIn(0f, 1f)
                                        .pow(colorCurve)
                                    lerp(lowColor, barColor, tLow)
                                } else {
                                    val tHigh = safeDiv(barValue - med, (realMax - med))
                                        .coerceIn(0f, 1f)
                                        .pow(colorCurve)
                                    lerp(barColor, highColor, tHigh)
                                }
                            }

                            else -> barColor
                        }

                        drawRect(
                            color = columnBgColor,
                            topLeft = Offset(x, barAreaTop),
                            size = Size(w, barAreaHeightPx)
                        )

                        if (!isMissing) {
                            val r = (w * 0.35f).coerceAtMost(with(density) { 6.dp.toPx() })
                            drawRoundRect(
                                color = c,
                                topLeft = Offset(x, y),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(r, r)
                            )
                        }
                    }

                    // time labels
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = labelColor.toArgb()
                            textSize = with(density) { 11.sp.toPx() }
                        }

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

                // Right min/max labels OUTSIDE the canvas (no reserved empty space inside chart)
                if (showMinMaxLabels && finiteVals.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Column(
                        modifier = Modifier
                            .height(chartHeight)
                            .padding(bottom = 18.dp), // aligns bottom label with chartBottom
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = valueFormatter(scaleMax),
                            fontFamily = fontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor
                        )
                        Text(
                            text = valueFormatter(scaleMin),
                            fontFamily = fontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor
                        )
                    }
                }
            }
        }
    }
}
