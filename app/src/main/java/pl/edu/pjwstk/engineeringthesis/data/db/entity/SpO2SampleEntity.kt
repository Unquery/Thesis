package pl.edu.pjwstk.engineeringthesis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.edu.pjwstk.engineeringthesis.model.SpO2Sample

@Entity(tableName = "spo2_sample")
data class SpO2SampleEntity(
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