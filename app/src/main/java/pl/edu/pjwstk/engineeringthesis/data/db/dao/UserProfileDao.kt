package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import pl.edu.pjwstk.engineeringthesis.data.db.entity.UserProfileEntity

@Dao
interface UserProfileDao {
    // --- Create/Update ---
    @Upsert
    suspend fun upsert(entity: UserProfileEntity)

    // --- Read ---
    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun getById(id: Int): UserProfileEntity?

    @Query("SELECT * FROM user_profile ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): UserProfileEntity?

    @Query("SELECT * FROM user_profile ORDER BY id ASC")
    suspend fun getAll(): List<UserProfileEntity>

    // --- Observe (optional but very useful for UI) ---
    @Query("SELECT * FROM user_profile WHERE id = :id")
    fun observeById(id: Int): kotlinx.coroutines.flow.Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile ORDER BY id DESC LIMIT 1")
    fun observeLatest(): kotlinx.coroutines.flow.Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile ORDER BY id ASC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<UserProfileEntity>>

    // --- Exists ---
    @Query("SELECT EXISTS(SELECT 1 FROM user_profile)")
    suspend fun existsAny(): Boolean

    // --- Delete ---
    @Query("DELETE FROM user_profile WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM user_profile")
    suspend fun clearAll()

    // --- Optional partial updates (handy for settings screens) ---
    @Query("UPDATE user_profile SET gender = :gender WHERE id = :id")
    suspend fun setGender(id: Int, gender: String)

    @Query("UPDATE user_profile SET birthDateEpochDays = :birthDateEpochDays WHERE id = :id")
    suspend fun setBirthDateEpochDays(id: Int, birthDateEpochDays: Long)

    @Query("UPDATE user_profile SET heightCm = :heightCm WHERE id = :id")
    suspend fun setHeightCm(id: Int, heightCm: Int)
}