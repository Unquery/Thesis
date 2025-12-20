package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg

@Dao
interface GsrSampleDao {

    @Upsert
    suspend fun upsertGsrSample(gsrSample : GsrSampleEntity)

    @Update
    suspend fun updateGsrSample(gsrSample : GsrSampleEntity)

    @Transaction
    @Query("SELECT * FROM gsr_sample WHERE id = :id;")
    suspend fun getGsrSample(id: Int): GsrSampleEntity?

    @Transaction
    @Query("SELECT * FROM gsr_sample")
    suspend fun getAllTGsrSamples(): List<GsrSampleEntity>

    @Transaction
    @Query("SELECT * FROM gsr_sample WHERE epoch >= :firstEpoch AND epoch < :lastEpoch")
    suspend fun getGsrSamples(firstEpoch : Long, lastEpoch : Long): List<GsrSampleEntity>

    @Query("DELETE FROM gsr_sample WHERE id = :id")
    suspend fun removeGsrSample(id : Int)

    @Query("DELETE FROM gsr_sample WHERE epoch = :epoch")
    suspend fun removeGsrSamplesByEpoch(epoch : Long)

    @Query("DELETE FROM gsr_sample")
    suspend fun removeAllGsrSamples()

    @Query(
        """
        SELECT 
            CAST(strftime('%H', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
            AVG(gsr) AS avg
        FROM gsr_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
        GROUP BY hour
        ORDER BY hour
        """
    )
    fun observeHourlyAvg(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<List<HourlyAvg>>

}