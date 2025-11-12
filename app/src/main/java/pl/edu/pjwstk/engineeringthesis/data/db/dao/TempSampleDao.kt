package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity

@Dao
interface TempSampleDao {

    @Query("SELECT * FROM temp_sample WHERE id = :id")
    suspend fun getTempSample(id: Int): TempSampleEntity?

    @Query("SELECT * FROM temp_sample ORDER BY epoch")
    suspend fun getAllTempSamples(): List<TempSampleEntity>

    @Query("""
        SELECT * FROM temp_sample 
        WHERE epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch
    """)
    suspend fun getTempSamples(firstEpoch: Long, lastEpoch: Long): List<TempSampleEntity>

    @Upsert
    suspend fun upsertTempSample(entity: TempSampleEntity)

    @Upsert
    suspend fun upsertTempSamples(entities: List<TempSampleEntity>)

    @Query("DELETE FROM temp_sample WHERE id = :id")
    suspend fun removeTempSample(id: Int)

    @Query("DELETE FROM temp_sample WHERE epoch = :epoch")
    suspend fun removeTempSamplesByEpoch(epoch: Long)

    @Query("DELETE FROM temp_sample")
    suspend fun removeAllTempSamples()

}