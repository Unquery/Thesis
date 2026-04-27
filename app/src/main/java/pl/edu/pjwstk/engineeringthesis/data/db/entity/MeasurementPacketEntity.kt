package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.MeasurementPacket

@Entity(
    tableName = "measurement_packet",
    indices = [
        Index(value = ["userId", "epoch"]),
        Index(value = ["userId", "receivedAt"])
    ]
)
data class MeasurementPacketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val epoch: Long,
    val receivedAt: Long,
    val temperature: Float?,
    val heartRate: Float?,
    val spo2: Int?,
    val gsr: Float?
) {
    fun toDomain(): MeasurementPacket =
        MeasurementPacket(
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
