package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import pl.edu.pjwstk.engineeringthesis.util.GSR_MENU_MAX_VALUE
import pl.edu.pjwstk.engineeringthesis.util.GSR_NEUTRAL_MAX
import pl.edu.pjwstk.engineeringthesis.util.GSR_NEUTRAL_MIN
import pl.edu.pjwstk.engineeringthesis.util.GSR_VERY_LOW_THRESHOLD

internal fun gsrMenuColor(
    value: Float,
    baseColor: Color,
    neutralMin: Float = GSR_NEUTRAL_MIN,
    neutralMax: Float = GSR_NEUTRAL_MAX
): Color {
    val safeValue = value.coerceAtLeast(0f)
    val safeNeutralMin = neutralMin
        .coerceAtLeast(GSR_VERY_LOW_THRESHOLD + 0.0001f)
        .coerceAtMost(GSR_MENU_MAX_VALUE - 0.0002f)
    val safeNeutralMax = neutralMax
        .coerceAtLeast(safeNeutralMin + 0.0001f)
        .coerceAtMost(GSR_MENU_MAX_VALUE - 0.0001f)

    return when {
        safeValue < GSR_VERY_LOW_THRESHOLD -> {
            lerp(baseColor, Color.White, 0.82f)
        }

        safeValue < safeNeutralMin -> {
            val progress = ((safeValue - GSR_VERY_LOW_THRESHOLD) /
                (safeNeutralMin - GSR_VERY_LOW_THRESHOLD)).coerceIn(0f, 1f)
            lerp(baseColor, Color.White, 0.6f * (1f - progress))
        }

        safeValue <= safeNeutralMax -> baseColor

        else -> {
            val progress = ((safeValue - safeNeutralMax) /
                (GSR_MENU_MAX_VALUE - safeNeutralMax)).coerceIn(0f, 1f)
            lerp(baseColor, Color.Black, 0.78f * progress)
        }
    }
}

internal fun gsrMenuColor(
    value: Double?,
    baseColor: Color,
    neutralMin: Float = GSR_NEUTRAL_MIN,
    neutralMax: Float = GSR_NEUTRAL_MAX
): Color {
    return value?.let { gsrMenuColor(it.toFloat(), baseColor, neutralMin, neutralMax) } ?: baseColor
}
