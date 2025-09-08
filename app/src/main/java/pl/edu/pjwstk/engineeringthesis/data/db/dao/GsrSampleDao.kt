package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity

@Dao
interface GsrSampleDao {

    @Insert
    suspend fun addNote(note : GsrSampleEntity)

    @Update
    suspend fun updateNote(note : GsrSampleEntity)

    @Transaction
    @Query("SELECT * FROM temp_sample WHERE id = :id;")
    suspend fun getTempSample(id: Int): GsrSampleEntity?

    @Transaction
    @Query("SELECT * FROM temp_sample")
    suspend fun getAllTempSample(): List<GsrSampleEntity>


}