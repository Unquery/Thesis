package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity

@Dao
interface TempSampleDao {

    @Insert
    suspend fun addTempSample(tempSample : TempSampleEntity)

    @Update
    suspend fun updateTempSample(tempSample : TempSampleEntity)

    @Transaction
    @Query("SELECT * FROM temp_sample WHERE id = :id;")
    suspend fun getTempSample(id: Int): TempSampleEntity?

    @Transaction
    @Query("SELECT * FROM temp_sample")
    suspend fun getAllTempSample(): List<TempSampleEntity>

    @Transaction
    @Query("SELECT * FROM temp_sample WHERE epoch >= :firstEpoch AND epoch < :lastEpoch")
    suspend fun getTempSamples(firstEpoch : Long, lastEpoch : Long): List<TempSampleEntity>

    @Query("DELETE FROM temp_sample WHERE id = :id")
    suspend fun removeTempSample(id : Int)

    @Query("DELETE FROM temp_sample WHERE epoch = :epoch")
    suspend fun removeTempSamplesByEpoch(epoch : Long)

    @Query("DELETE FROM temp_sample")
    suspend fun removeAllTempSamples()

}