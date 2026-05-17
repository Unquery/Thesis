package pl.edu.pjwstk.engineeringthesis.util

const val GSR_INVALID_SENTINEL_US = 0f
const val GSR_OVER_RANGE_US = 100f
const val GSR_SENSOR_MIN_US = 0.0001f
const val GSR_SENSOR_MAX_US = 99.9999f
const val GSR_VERY_LOW_THRESHOLD = 1f
const val GSR_NEUTRAL_MIN = 5f
const val GSR_NEUTRAL_MAX = 10f
const val GSR_MENU_MAX_VALUE = GSR_OVER_RANGE_US

fun isValidGsrMeasurement(value: Float): Boolean =
    value.isFinite() && value > GSR_INVALID_SENTINEL_US && value < GSR_OVER_RANGE_US

fun isGsrOverRange(value: Float): Boolean =
    value.isFinite() && value >= GSR_OVER_RANGE_US

fun normalizeGsrForDisplay(value: Float): Float? =
    when {
        isValidGsrMeasurement(value) -> value
        isGsrOverRange(value) -> GSR_OVER_RANGE_US
        else -> null
    }

fun coerceGsrToSensorRange(value: Float): Float =
    if (value.isFinite()) value.coerceIn(GSR_SENSOR_MIN_US, GSR_SENSOR_MAX_US) else GSR_SENSOR_MIN_US

fun coerceGsrNormalRange(low: Float, high: Float): Pair<Float, Float> {
    val finiteLow = low.takeIf { it.isFinite() } ?: GSR_NEUTRAL_MIN
    val finiteHigh = high.takeIf { it.isFinite() } ?: GSR_NEUTRAL_MAX
    val safeLow = finiteLow.coerceIn(GSR_SENSOR_MIN_US, GSR_SENSOR_MAX_US - 0.0001f)
    val safeHigh = finiteHigh.coerceIn(safeLow + 0.0001f, GSR_SENSOR_MAX_US)
    return safeLow to safeHigh
}
