package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.model.UserProfile

interface ProfileRepository {
    suspend fun upsert(profile: UserProfile)
    suspend fun insert(profile: UserProfile): Int

    suspend fun getById(id: Int): UserProfile?
    suspend fun getLatest(): UserProfile?
    suspend fun getAll(): List<UserProfile>
    suspend fun getNextId(): Int

    fun observeById(id: Int): Flow<UserProfile?>
    fun observeLatest(): Flow<UserProfile?>
    fun observeAll(): Flow<List<UserProfile>>


    suspend fun getActive(): UserProfile?
    fun observeActive(): Flow<UserProfile?>
    suspend fun setActiveProfile(id: Int)
    suspend fun insertAndActivate(profile: UserProfile): Int

    suspend fun existsAny(): Boolean

    suspend fun deleteById(id: Int)
    suspend fun clearAll()

    suspend fun setName(id: Int, name: String)
    suspend fun setGender(id: Int, gender: String)
    suspend fun setBirthDateEpochDays(id: Int, birthDateEpochDays: Long)
    suspend fun setHeightCm(id: Int, heightCm: Int)
}
