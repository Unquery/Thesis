package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.HearthRateSample

@Entity(
    tableName = "hearth_rate_sample",
    indices = [Index(value = ["userId", "epoch"])]
)
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
