package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.HearthRateSample

@Entity(tableName = "gsr_sample")
data class HearthRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,

    val userId : Int,

    val epoch : Long,

    val hearthRate : Float
){
    fun toDomain() : HearthRateSample{
        return HearthRateSample(
            id,
            userId,
            epoch,
            hearthRate
        )
    }
}
