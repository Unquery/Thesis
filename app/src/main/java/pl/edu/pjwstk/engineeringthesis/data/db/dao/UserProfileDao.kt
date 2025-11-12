package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import pl.edu.pjwstk.engineeringthesis.data.db.entity.UserProfileEntity

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Upsert
    suspend fun upsert(entity: UserProfileEntity)

    @Query("DELETE FROM user_profile WHERE id = 1")
    suspend fun clear()
}