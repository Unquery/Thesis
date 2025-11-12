package pl.edu.pjwstk.engineeringthesis.data.repository

import pl.edu.pjwstk.engineeringthesis.data.db.entity.UserProfileEntity
import pl.edu.pjwstk.engineeringthesis.model.UserProfile

interface ProfileRepository {
    suspend fun get(): UserProfile?
    suspend fun save(profile: UserProfile)
    suspend fun exists(): Boolean
    suspend fun clear()
}