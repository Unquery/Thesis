package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.HearthRateSample

@Entity(tableName = "hearth_rate_sample")
data class HearthRateSampleEntity(
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
