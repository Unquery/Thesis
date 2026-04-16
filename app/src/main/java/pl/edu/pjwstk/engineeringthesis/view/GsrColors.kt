package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import pl.edu.pjwstk.engineeringthesis.util.GSR_MENU_MAX_VALUE
import pl.edu.pjwstk.engineeringthesis.util.GSR_NEUTRAL_MAX
import pl.edu.pjwstk.engineeringthesis.util.GSR_NEUTRAL_MIN
import pl.edu.pjwstk.engineeringthesis.util.GSR_VERY_LOW_THRESHOLD

internal fun gsrMenuColor(value: Float, baseColor: Color): Color {
    val safeValue = value.coerceAtLeast(0f)

    return when {
        safeValue < GSR_VERY_LOW_THRESHOLD -> {
            lerp(baseColor, Color.White, 0.82f)
        }

        safeValue < GSR_NEUTRAL_MIN -> {
            val progress = ((safeValue - GSR_VERY_LOW_THRESHOLD) /
                (GSR_NEUTRAL_MIN - GSR_VERY_LOW_THRESHOLD)).coerceIn(0f, 1f)
            lerp(baseColor, Color.White, 0.6f * (1f - progress))
        }

        safeValue <= GSR_NEUTRAL_MAX -> baseColor

        else -> {
            val progress = ((safeValue - GSR_NEUTRAL_MAX) /
                (GSR_MENU_MAX_VALUE - GSR_NEUTRAL_MAX)).coerceIn(0f, 1f)
            lerp(baseColor, Color.Black, 0.78f * progress)
        }
    }
}

internal fun gsrMenuColor(value: Double?, baseColor: Color): Color {
    return value?.let { gsrMenuColor(it.toFloat(), baseColor) } ?: baseColor
}
