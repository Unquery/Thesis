package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.model.UserProfile

interface ProfileRepository {
    suspend fun upsert(profile: UserProfile)

    suspend fun getById(id: Int): UserProfile?
    suspend fun getLatest(): UserProfile?
    suspend fun getAll(): List<UserProfile>

    fun observeById(id: Int): Flow<UserProfile?>
    fun observeLatest(): Flow<UserProfile?>
    fun observeAll(): Flow<List<UserProfile>>

    suspend fun existsAny(): Boolean

    suspend fun deleteById(id: Int)
    suspend fun clearAll()

    suspend fun setGender(id: Int, gender: String)
    suspend fun setBirthDateEpochDays(id: Int, birthDateEpochDays: Long)
    suspend fun setHeightCm(id: Int, heightCm: Int)
}