package pl.edu.pjwstk.engineeringthesis.model

data class UserProfile(
    val id : Int,
    val gender: String,
    val birthDateEpochDays: Long,
    val heightCm: Int,
    val isActive: Boolean = false
)
