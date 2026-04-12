package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.data.db.entity.HearthRateSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.DailyAvg
import pl.edu.pjwstk.engineeringthesis.model.DailyMinMax
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyMinMax
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary

@Dao
interface HearthRateSampleDao {
    @Upsert
    suspend fun upsert(sample: HearthRateSampleEntity)

    @Update
    suspend fun update(sample: HearthRateSampleEntity)

    @Transaction
    @Query("SELECT * FROM hearth_rate_sample WHERE id = :id")
    suspend fun getById(id: Int): HearthRateSampleEntity?

    @Transaction
    @Query("SELECT * FROM hearth_rate_sample")
    suspend fun getAll(): List<HearthRateSampleEntity>

    @Transaction
    @Query("""
        SELECT * FROM hearth_rate_sample
        WHERE epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC
    """)
    suspend fun getRange(firstEpoch: Long, lastEpoch: Long): List<HearthRateSampleEntity>

    @Transaction
    @Query("""
        SELECT * FROM hearth_rate_sample
        WHERE userId = :userId
          AND epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC
    """)
    suspend fun getRangeForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): List<HearthRateSampleEntity>

    @Query("""
        SELECT * FROM hearth_rate_sample
        WHERE userId = :userId
          AND epoch >= :firstEpoch AND epoch < :lastEpoch
        ORDER BY epoch ASC, id ASC
    """)
    fun observeRangeForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): Flow<List<HearthRateSampleEntity>>

    @Query("DELETE FROM hearth_rate_sample WHERE id = :id")
    suspend fun removeById(id: Int)

    @Query("DELETE FROM hearth_rate_sample WHERE epoch = :epoch")
    suspend fun removeByEpoch(epoch: Long)

    @Query("DELETE FROM hearth_rate_sample")
    suspend fun removeAll()

    @Query(
        """
        SELECT 
            CAST(strftime('%H', datetime(epoch / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
            AVG(CAST(hearthRate AS REAL)) AS avg
        FROM hearth_rate_sample
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
            MIN(CAST(hearthRate AS REAL)) AS min,
            MAX(CAST(hearthRate AS REAL)) AS max
        FROM hearth_rate_sample
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
            AVG(CAST(hearthRate AS REAL)) AS avg
        FROM hearth_rate_sample
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
            MIN(CAST(hearthRate AS REAL)) AS min,
            MAX(CAST(hearthRate AS REAL)) AS max
        FROM hearth_rate_sample
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
        SELECT * FROM hearth_rate_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch AND epoch < :endEpoch
        ORDER BY epoch DESC, id DESC
        LIMIT 1
    """)
    fun observeLatestInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<HearthRateSampleEntity?>

    @Query("""
        SELECT * FROM hearth_rate_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch AND epoch < :endEpoch
        ORDER BY epoch DESC, id DESC
        LIMIT 2
    """)
    fun observeLatestTwoInRange(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<List<HearthRateSampleEntity>>

    @Query("""
        SELECT 
            AVG(CAST(hearthRate AS REAL)) AS avg,
            MIN(hearthRate) AS min,
            MAX(hearthRate) AS max,
            COUNT(*) AS count
        FROM hearth_rate_sample
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
        SELECT 1 FROM hearth_rate_sample
        WHERE userId = :userId
          AND epoch >= :startEpoch
          AND epoch < :endEpoch
    )
    """)
    suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean

}
