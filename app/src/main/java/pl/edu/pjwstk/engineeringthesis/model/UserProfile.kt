package pl.edu.pjwstk.engineeringthesis.model

import pl.edu.pjwstk.engineeringthesis.data.db.entity.UserProfileEntity

data class UserProfile(
    val gender: String,
    val birthDateEpochDays: Long,
    val heightCm: Int
)

fun UserProfileEntity.toDomain() = UserProfile(
    gender = gender,
    birthDateEpochDays = birthDateEpochDays,
    heightCm = heightCm
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = 1,
    gender = gender,
    birthDateEpochDays = birthDateEpochDays,
    heightCm = heightCm
)
