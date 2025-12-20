package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary

@Dao
interface TempSampleDao {

    @Upsert
    suspend fun upsertTempSample(sample: TempSampleEntity)

    @Update
    suspend fun updateTempSample(sample: TempSampleEntity)

    @Transaction
    @Query("SELECT * FROM temp_sample WHERE id = :id;")
    suspend fun getTempSample(id: Int): TempSampleEntity?

    @Transaction
    @Query("SELECT * FROM temp_sample")
    suspend fun getAllTempSamples(): List<TempSampleEntity>

    @Transaction
    @Query("""
        SELECT * FROM temp_sample
        WHERE epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC
    """)
    suspend fun getTempSamples(firstEpoch: Long, lastEpoch: Long): List<TempSampleEntity>

    @Transaction
    @Query("""
        SELECT * FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC
    """)
    suspend fun getTempSamplesForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): List<TempSampleEntity>

    @Query("""
        SELECT * FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC
    """)
    fun observeTempSamplesForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): Flow<List<TempSampleEntity>>

    // delete
    @Query("DELETE FROM temp_sample WHERE id = :id")
    suspend fun removeTempSample(id: Int)

    @Query("DELETE FROM temp_sample WHERE epoch = :epoch")
    suspend fun removeTempSamplesByEpoch(epoch: Long)

    @Query("DELETE FROM temp_sample")
    suspend fun removeAllTempSamples()

    // dashboard: 24 bars avg/hour (epoch assumed millis)
    @Query(
        """
        SELECT 
            CAST(strftime('%H', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
            AVG(CAST(temperature AS REAL)) AS avg
        FROM temp_sample
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

    // dashboard: latest sample in range
    @Query("""
        SELECT * FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch AND epoch < :endEpoch
        ORDER BY epoch DESC
        LIMIT 1
    """)
    fun observeLatestInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<TempSampleEntity?>

    // dashboard: summary in range
    @Query("""
        SELECT 
            AVG(CAST(temperature AS REAL)) AS avg,
            MIN(CAST(temperature AS REAL)) AS min,
            MAX(CAST(temperature AS REAL)) AS max,
            COUNT(*) AS count
        FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch AND epoch < :endEpoch
    """)
    fun observeSummaryInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<MetricSummary>

}