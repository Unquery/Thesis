package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.GsrSample

@Entity(tableName = "gsr_sample")
data class GsrSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,

    val epoch : Long,

    val gsr : Int
){
    fun toDomain() : GsrSample{
        return GsrSample(
            id,
            epoch,
            gsr
        )
    }
}
