package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.model.MeasurementPacket

interface MeasurementPacketRepository {
    suspend fun insert(packet: MeasurementPacket)
    fun observeLatestTwoForUser(userId: Int): Flow<List<MeasurementPacket>>
    suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean
}
