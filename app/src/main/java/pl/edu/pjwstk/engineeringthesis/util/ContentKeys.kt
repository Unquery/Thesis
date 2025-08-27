package pl.edu.pjwstk.engineeringthesis.util

import kotlinx.serialization.Serializable


sealed interface Route

@Serializable data object Menu : Route

//Todo keys for other screens