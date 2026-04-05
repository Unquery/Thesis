package pl.edu.pjwstk.engineeringthesis.model

data class UserProfile(
    val id : Int,
    val name: String = "",
    val gender: String,
    val birthDateEpochDays: Long,
    val heightCm: Int,
    val weightKg: Float = 0f,
    val isActive: Boolean = false
)
