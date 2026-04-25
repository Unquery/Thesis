package pl.edu.pjwstk.engineeringthesis.model

import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_HEART_RATE_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_HEART_RATE_LOW
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SKIN_CONDUCTANCE_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SKIN_CONDUCTANCE_LOW
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SPO2_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SPO2_LOW
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_TEMPERATURE_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_TEMPERATURE_LOW
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_TEMPERATURE_OFFSET_C

data class UserProfile(
    val id : Int,
    val name: String = "",
    val gender: String,
    val birthDateEpochDays: Long,
    val heightCm: Int,
    val weightKg: Float = 0f,
    val isActive: Boolean = false,
    val temperatureNormalLow: Float = PROFILE_DEFAULT_TEMPERATURE_LOW,
    val temperatureNormalHigh: Float = PROFILE_DEFAULT_TEMPERATURE_HIGH,
    val temperatureOffsetC: Float = PROFILE_DEFAULT_TEMPERATURE_OFFSET_C,
    val heartRateNormalLow: Float = PROFILE_DEFAULT_HEART_RATE_LOW,
    val heartRateNormalHigh: Float = PROFILE_DEFAULT_HEART_RATE_HIGH,
    val spO2NormalLow: Float = PROFILE_DEFAULT_SPO2_LOW,
    val spO2NormalHigh: Float = PROFILE_DEFAULT_SPO2_HIGH,
    val skinConductanceNormalLow: Float = PROFILE_DEFAULT_SKIN_CONDUCTANCE_LOW,
    val skinConductanceNormalHigh: Float = PROFILE_DEFAULT_SKIN_CONDUCTANCE_HIGH
)
