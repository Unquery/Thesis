package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.entity.MeasurementPacketEntity
import pl.edu.pjwstk.engineeringthesis.model.MeasurementPacket
import javax.inject.Inject

class MeasurementPacketRepositoryImpl @Inject constructor(
    diaryDB: DiaryDB
) : MeasurementPacketRepository {

    private val dao = diaryDB.measurementPackets

    override suspend fun insert(packet: MeasurementPacket) {
        dao.insert(packet.toEntity())
    }

    override fun observeLatestTwoForUser(userId: Int): Flow<List<MeasurementPacket>> =
        dao.observeLatestTwoForUser(userId)
            .map { packets -> packets.map { it.toDomain() } }

    override suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean =
        dao.existsInRange(userId, startEpoch, endEpoch)

    private fun MeasurementPacket.toEntity(): MeasurementPacketEntity =
        MeasurementPacketEntity(
            id = id,
            userId = userId,
            epoch = epoch,
            receivedAt = receivedAt,
            temperature = temperature,
            heartRate = heartRate,
            spo2 = spo2,
            gsr = gsr
        )
}
