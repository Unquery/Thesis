package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.data.db.entity.MeasurementPacketEntity

@Dao
interface MeasurementPacketDao {

    @Insert
    suspend fun insert(packet: MeasurementPacketEntity)

    @Query(
        """
        SELECT * FROM measurement_packet
        WHERE userId = :userId
        ORDER BY receivedAt DESC, id DESC
        LIMIT 2
        """
    )
    fun observeLatestTwoForUser(userId: Int): Flow<List<MeasurementPacketEntity>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM measurement_packet
            WHERE userId = :userId
              AND epoch >= :startEpoch
              AND epoch < :endEpoch
        )
        """
    )
    suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean
}
