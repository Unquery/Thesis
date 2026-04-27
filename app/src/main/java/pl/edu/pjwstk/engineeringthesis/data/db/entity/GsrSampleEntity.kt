package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.GsrSample

@Entity(
    tableName = "gsr_sample",
    indices = [Index(value = ["userId", "epoch"])]
)
data class GsrSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,

    val userId : Int,

    val epoch : Long,

    val gsr : Float
){
    fun toDomain() : GsrSample{
        return GsrSample(
            id,
            userId,
            epoch,
            gsr
        )
    }
}
