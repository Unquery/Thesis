package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 1,
    val name: String = "",
    val gender: String,
    val birthDateEpochDays: Long,
    val heightCm: Int,
    val isActive: Boolean = false
)
