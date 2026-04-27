package pl.edu.pjwstk.engineeringthesis.model

data class MeasurementPacket(
    val id: Int,
    val userId: Int,
    val epoch: Long,
    val receivedAt: Long,
    val temperature: Float? = null,
    val heartRate: Float? = null,
    val spo2: Int? = null,
    val gsr: Float? = null
)
