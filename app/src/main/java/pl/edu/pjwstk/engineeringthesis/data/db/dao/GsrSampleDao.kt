package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.DailyAvg
import pl.edu.pjwstk.engineeringthesis.model.DailyMinMax
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyMinMax
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary

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

    @Transaction
    @Query("""
        SELECT * FROM gsr_sample
        WHERE userId = :userId
          AND epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC
    """)
    suspend fun getGsrSamplesForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): List<GsrSampleEntity>


    @Query("""
        SELECT * FROM gsr_sample
        WHERE userId = :userId
          AND epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC
    """)
    fun observeGsrSamplesForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): Flow<List<GsrSampleEntity>>

    @Query(
        """
        SELECT 
            CAST(strftime('%H', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
            AVG(CAST(gsr AS REAL)) AS avg
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

    @Query(
        """
        SELECT
            CAST(strftime('%H', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
            MIN(CAST(gsr AS REAL)) AS min,
            MAX(CAST(gsr AS REAL)) AS max
        FROM gsr_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
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
            AVG(CAST(gsr AS REAL)) AS avg
        FROM gsr_sample
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
            MIN(CAST(gsr AS REAL)) AS min,
            MAX(CAST(gsr AS REAL)) AS max
        FROM gsr_sample
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
        SELECT * FROM gsr_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
        ORDER BY epoch DESC
        LIMIT 1
    """)
    fun observeLatestInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<GsrSampleEntity?>

    @Query("""
        SELECT 
            AVG(CAST(gsr AS REAL)) AS avg,
            MIN(gsr) AS min,
            MAX(gsr) AS max,
            COUNT(*) AS count
        FROM gsr_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
    """)
    fun observeSummaryInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<MetricSummary>

    @Query("""
    SELECT EXISTS(
        SELECT 1 FROM gsr_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
    )
    """)
    suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean

}
