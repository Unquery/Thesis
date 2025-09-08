package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.TempSample

@Entity(tableName = "gsr_sample")
data class TempSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,

    val epoch : Long,

    val temperature : Float
){
    fun toDomain() : TempSample{
        return TempSample(
            id,
            epoch,
            temperature
        )
    }
}
