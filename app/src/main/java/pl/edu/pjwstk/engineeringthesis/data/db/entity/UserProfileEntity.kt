package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_HEART_RATE_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_HEART_RATE_LOW
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SKIN_CONDUCTANCE_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SKIN_CONDUCTANCE_LOW
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_TEMPERATURE_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_TEMPERATURE_LOW

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 1,
    val name: String = "",
    val gender: String,
    val birthDateEpochDays: Long,
    val heightCm: Int,
    val weightKg: Float = 0f,
    val isActive: Boolean = false,
    val temperatureNormalLow: Float = PROFILE_DEFAULT_TEMPERATURE_LOW,
    val temperatureNormalHigh: Float = PROFILE_DEFAULT_TEMPERATURE_HIGH,
    val heartRateNormalLow: Float = PROFILE_DEFAULT_HEART_RATE_LOW,
    val heartRateNormalHigh: Float = PROFILE_DEFAULT_HEART_RATE_HIGH,
    val skinConductanceNormalLow: Float = PROFILE_DEFAULT_SKIN_CONDUCTANCE_LOW,
    val skinConductanceNormalHigh: Float = PROFILE_DEFAULT_SKIN_CONDUCTANCE_HIGH
)
