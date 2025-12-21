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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun GsrBox(
    title: String,
    bars: List<Float?>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title)
            Spacer(Modifier.height(8.dp))

            val density = LocalDensity.current
            val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val rightLabelPadPx = with(density) { 42.dp.toPx() }
                val bottomLabelPadPx = with(density) { 18.dp.toPx() }

                val chartLeft = 0f
                val chartTop = 0f
                val chartRight = size.width - rightLabelPadPx
                val chartBottom = size.height - bottomLabelPadPx

                val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
                val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

                val vals = bars.take(24).let { list ->
                    if (list.size < 24) list + List(24 - list.size) { null } else list
                }.map { it ?: 0f }

                val maxVal = (vals.maxOrNull() ?: 0f).takeIf { it > 0f } ?: 1f
                val minVal = (vals.minOrNull() ?: 0f)

                val n = 24
                val gap = chartWidth * 0.006f
                val w = (chartWidth - gap * (n - 1)) / n

                for (i in 0 until n) {
                    val ratio = (vals[i] / maxVal).coerceIn(0f, 1f)
                    val h = chartHeight * ratio
                    val x = chartLeft + i * (w + gap)
                    val y = chartTop + (chartHeight - h)

                    drawRect(
                        color = Color.Black.copy(alpha = 0.08f),
                        topLeft = Offset(x, chartTop),
                        size = Size(w, chartHeight)
                    )

                    drawRect(
                        color = Color(0xFF3F51B5),
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

                    val rightX = chartRight + with(density) { 6.dp.toPx() }

                    canvas.nativeCanvas.drawText(
                        "${minVal.roundToInt()}",
                        rightX,
                        chartBottom,
                        paint
                    )

                    canvas.nativeCanvas.drawText(
                        "${maxVal.roundToInt()}",
                        rightX,
                        chartTop + paint.textSize,
                        paint
                    )

                    val timeY = size.height
                    canvas.nativeCanvas.drawText(
                        "00:00",
                        chartLeft,
                        timeY,
                        paint
                    )

                    val rightText = "24:00"
                    val rightTextWidth = paint.measureText(rightText)
                    canvas.nativeCanvas.drawText(
                        rightText,
                        chartRight - rightTextWidth,
                        timeY,
                        paint
                    )
                }
            }
        }
    }
}
