package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import pl.edu.pjwstk.engineeringthesis.data.db.entity.UserProfileEntity

@Dao
interface UserProfileDao {
    @Upsert
    suspend fun upsert(entity: UserProfileEntity)

    @Insert
    suspend fun insert(entity: UserProfileEntity): Long

    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun getById(id: Int): UserProfileEntity?

    @Query("SELECT * FROM user_profile ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): UserProfileEntity?

    @Query("SELECT * FROM user_profile ORDER BY id ASC")
    suspend fun getAll(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profile WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE isActive = 1 LIMIT 1")
    fun observeActive(): kotlinx.coroutines.flow.Flow<UserProfileEntity?>

    @Query("UPDATE user_profile SET isActive = 0 WHERE isActive = 1")
    suspend fun clearActiveFlag()

    @Query("UPDATE user_profile SET isActive = 1 WHERE id = :id")
    suspend fun setActiveFlag(id: Int)

    @Transaction
    suspend fun setActiveProfile(id: Int) {
        clearActiveFlag()
        setActiveFlag(id)
    }

    @Transaction
    suspend fun insertAndActivate(entity: UserProfileEntity): Int {
        val newId = insert(entity.copy(isActive = false)).toInt()
        setActiveProfile(newId)
        return newId
    }

    @Query("SELECT * FROM user_profile WHERE id = :id")
    fun observeById(id: Int): kotlinx.coroutines.flow.Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile ORDER BY id DESC LIMIT 1")
    fun observeLatest(): kotlinx.coroutines.flow.Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile ORDER BY id ASC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<UserProfileEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_profile)")
    suspend fun existsAny(): Boolean

    @Query("DELETE FROM user_profile WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM user_profile")
    suspend fun clearAll()

    @Query("UPDATE user_profile SET name = :name WHERE id = :id")
    suspend fun setName(id: Int, name: String)

    @Query("UPDATE user_profile SET gender = :gender WHERE id = :id")
    suspend fun setGender(id: Int, gender: String)

    @Query("UPDATE user_profile SET birthDateEpochDays = :birthDateEpochDays WHERE id = :id")
    suspend fun setBirthDateEpochDays(id: Int, birthDateEpochDays: Long)

    @Query("UPDATE user_profile SET heightCm = :heightCm WHERE id = :id")
    suspend fun setHeightCm(id: Int, heightCm: Int)

    @Query("UPDATE user_profile SET weightKg = :weightKg WHERE id = :id")
    suspend fun setWeightKg(id: Int, weightKg: Float)

    @Query(
        """
        UPDATE user_profile
        SET temperatureNormalLow = :temperatureNormalLow,
            temperatureNormalHigh = :temperatureNormalHigh,
            heartRateNormalLow = :heartRateNormalLow,
            heartRateNormalHigh = :heartRateNormalHigh,
            skinConductanceNormalLow = :skinConductanceNormalLow,
            skinConductanceNormalHigh = :skinConductanceNormalHigh
        WHERE id = :id
        """
    )
    suspend fun setMeasurementCalibration(
        id: Int,
        temperatureNormalLow: Float,
        temperatureNormalHigh: Float,
        heartRateNormalLow: Float,
        heartRateNormalHigh: Float,
        skinConductanceNormalLow: Float,
        skinConductanceNormalHigh: Float
    )

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM user_profile")
    suspend fun getNextId(): Int

}
