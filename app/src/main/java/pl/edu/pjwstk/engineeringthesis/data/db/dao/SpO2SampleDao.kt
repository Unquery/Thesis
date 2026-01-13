package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.data.db.entity.SpO2SampleEntity
import pl.edu.pjwstk.engineeringthesis.model.DailyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary

@Dao
interface SpO2SampleDao {
    @Upsert
    suspend fun upsertSpO2Sample(sample: SpO2SampleEntity)

    @Update
    suspend fun updateSpO2Sample(sample: SpO2SampleEntity)

    @Transaction
    @Query("SELECT * FROM spo2_sample WHERE id = :id;")
    suspend fun getSpO2Sample(id: Int): SpO2SampleEntity?

    @Transaction
    @Query("SELECT * FROM spo2_sample")
    suspend fun getAllSpO2Samples(): List<SpO2SampleEntity>

    @Transaction
    @Query("SELECT * FROM spo2_sample WHERE epoch >= :firstEpoch AND epoch < :lastEpoch")
    suspend fun getSpO2Samples(firstEpoch: Long, lastEpoch: Long): List<SpO2SampleEntity>

    @Transaction
    @Query("""
        SELECT * FROM spo2_sample
        WHERE userId = :userId
          AND epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC
    """)
    suspend fun getSpO2SamplesForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): List<SpO2SampleEntity>

    @Query("""
        SELECT * FROM spo2_sample
        WHERE userId = :userId
          AND epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC
    """)
    fun observeSpO2SamplesForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): Flow<List<SpO2SampleEntity>>

    @Query("DELETE FROM spo2_sample WHERE id = :id")
    suspend fun removeSpO2Sample(id: Int)

    @Query("DELETE FROM spo2_sample WHERE epoch = :epoch")
    suspend fun removeSpO2SamplesByEpoch(epoch: Long)

    @Query("DELETE FROM spo2_sample")
    suspend fun removeAllSpO2Samples()

    @Query(
        """
        SELECT 
            CAST(strftime('%H', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
            AVG(CAST(spo2 AS REAL)) AS avg
        FROM spo2_sample
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

    @Query(
        """
        SELECT
            strftime('%Y-%m-%d', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS date,
            AVG(CAST(spo2 AS REAL)) AS avg
        FROM spo2_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
        GROUP BY date
        ORDER BY date
        """
    )
    fun observeDailyAvg(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<List<DailyAvg>>

    @Query("""
        SELECT * FROM spo2_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch AND epoch < :endEpoch
        ORDER BY epoch DESC
        LIMIT 1
    """)
    fun observeLatestInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<SpO2SampleEntity?>

    @Query("""
        SELECT 
            AVG(CAST(spo2 AS REAL)) AS avg,
            MIN(CAST(spo2 AS REAL)) AS min,
            MAX(CAST(spo2 AS REAL)) AS max,
            COUNT(*) AS count
        FROM spo2_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch AND epoch < :endEpoch
    """)
    fun observeSummaryInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<MetricSummary>

    @Query("""
    SELECT EXISTS(
        SELECT 1 FROM spo2_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
    )
    """)
    suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean

}
