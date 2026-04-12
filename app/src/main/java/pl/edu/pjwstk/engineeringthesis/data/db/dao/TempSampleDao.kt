package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.DailyAvg
import pl.edu.pjwstk.engineeringthesis.model.DailyMinMax
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyMinMax
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
        ORDER BY epoch ASC, id ASC
    """)
    fun observeTempSamplesForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): Flow<List<TempSampleEntity>>

    @Query("DELETE FROM temp_sample WHERE id = :id")
    suspend fun removeTempSample(id: Int)

    @Query("DELETE FROM temp_sample WHERE epoch = :epoch")
    suspend fun removeTempSamplesByEpoch(epoch: Long)

    @Query("DELETE FROM temp_sample")
    suspend fun removeAllTempSamples()

    @Query(
        """
        SELECT 
            CAST(strftime('%H', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
            AVG(CAST(temperature AS REAL)) AS avg
        FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
          AND temperature >= 30
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
            CAST(strftime('%H', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
            MIN(CAST(temperature AS REAL)) AS min,
            MAX(CAST(temperature AS REAL)) AS max
        FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
          AND temperature >= 30
        GROUP BY hour
        ORDER BY hour
        """
    )
    fun observeHourlyMinMax(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<List<HourlyMinMax>>

    @Query(
        """
        SELECT
            strftime('%Y-%m-%d', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS date,
            AVG(CAST(temperature AS REAL)) AS avg
        FROM temp_sample
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

    @Query(
        """
        SELECT
            strftime('%Y-%m-%d', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS date,
            MIN(CAST(temperature AS REAL)) AS min,
            MAX(CAST(temperature AS REAL)) AS max
        FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
        GROUP BY date
        ORDER BY date
        """
    )
    fun observeDailyMinMax(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<List<DailyMinMax>>

    @Query("""
        SELECT * FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch AND epoch < :endEpoch
        ORDER BY epoch DESC, id DESC
        LIMIT 1
    """)
    fun observeLatestInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<TempSampleEntity?>

    @Query("""
        SELECT * FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch AND epoch < :endEpoch
        ORDER BY epoch DESC, id DESC
        LIMIT 2
    """)
    fun observeLatestTwoInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<List<TempSampleEntity>>

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

    @Query("""
    SELECT EXISTS(
        SELECT 1 FROM temp_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
    )
    """)
    suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean

}
