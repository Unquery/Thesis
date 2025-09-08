package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity

@Dao
interface TempSampleDao {

    @Insert
    suspend fun addNote(note : TempSampleEntity)

    @Update
    suspend fun updateNote(note : TempSampleEntity)

    @Transaction
    @Query("SELECT * FROM temp_sample WHERE id = :id;")
    suspend fun getTempSample(id: Int): TempSampleEntity?

    @Transaction
    @Query("SELECT * FROM temp_sample")
    suspend fun getAllTempSample(): List<TempSampleEntity>

}