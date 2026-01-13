package pl.edu.pjwstk.engineeringthesis.util

import kotlinx.serialization.Serializable


sealed interface Route

@Serializable data object Menu : Route

@Serializable data object ConnectBand : Route

@Serializable
enum class ChartMetric {
    Temperature,
    HeartRate,
    SpO2,
    Gsr
}

@Serializable data class Charts(val metric: ChartMetric) : Route

@Serializable data object Profile : Route

//Todo keys for other screens
