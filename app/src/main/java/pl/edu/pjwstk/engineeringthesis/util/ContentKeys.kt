package pl.edu.pjwstk.engineeringthesis.util

import kotlinx.serialization.Serializable


sealed interface Route

@Serializable data object Menu : Route

@Serializable data object ConnectBand : Route

//Todo keys for other screens