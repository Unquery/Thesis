package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GsrBox(
    title: String,
    bars: List<Float?>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title)
            Spacer(Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val vals = bars.map { it ?: 0f }
                val max = (vals.maxOrNull() ?: 0f).takeIf { it > 0f } ?: 1f

                val n = 24
                val gap = size.width * 0.006f
                val w = (size.width - gap * (n - 1)) / n

                for (i in 0 until n) {
                    val ratio = (vals[i] / max).coerceIn(0f, 1f)
                    val h = size.height * ratio
                    val x = i * (w + gap)
                    val y = size.height - h

                    drawRect(
                        color = Color.Black.copy(alpha = 0.08f),
                        topLeft = Offset(x, 0f),
                        size = Size(w, size.height)
                    )
                    drawRect(
                        color = Color(0xFF3F51B5),
                        topLeft = Offset(x, y),
                        size = Size(w, h)
                    )
                }
            }
        }
    }
}
