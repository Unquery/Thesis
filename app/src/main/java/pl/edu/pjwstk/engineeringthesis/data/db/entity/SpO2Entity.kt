package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.SpO2Sample

@Entity(tableName = "gsr_sample")
data class SpO2Entity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,

    val userId : Int,

    val epoch : Long,

    val spo2 : Int
){
    fun toDomain() : SpO2Sample{
        return SpO2Sample(
            id,
            userId,
            epoch,
            spo2,
        )
    }
}