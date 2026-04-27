package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.TempSample

@Entity(
    tableName = "temp_sample",
    indices = [Index(value = ["userId", "epoch"])]
)
data class TempSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,

    val userId : Int,

    val epoch : Long,

    val temperature : Float
){
    fun toDomain() : TempSample{
        return TempSample(
            id,
            userId,
            epoch,
            temperature
        )
    }
}
