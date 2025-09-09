package pl.edu.pjwstk.engineeringthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity

@Dao
interface GsrSampleDao {

    @Insert
    suspend fun addGsrSample(gsrSample : GsrSampleEntity)

    @Update
    suspend fun updateGsrSample(gsrSample : GsrSampleEntity)

    @Transaction
    @Query("SELECT * FROM gsr_sample WHERE id = :id;")
    suspend fun getGsrSample(id: Int): GsrSampleEntity?

    @Transaction
    @Query("SELECT * FROM gsr_sample")
    suspend fun getAllTGsrSample(): List<GsrSampleEntity>

    @Transaction
    @Query("SELECT * FROM gsr_sample WHERE epoch >= :firstEpoch AND epoch < :lastEpoch")
    suspend fun getGsrSamples(firstEpoch : Long, lastEpoch : Long): List<GsrSampleEntity>

    @Query("DELETE FROM gsr_sample WHERE id = :id")
    suspend fun removeGsrSample(id : Int)

    @Query("DELETE FROM gsr_sample WHERE epoch = :epoch")
    suspend fun removeGsrSamplesByEpoch(epoch : Long)

    @Query("DELETE FROM gsr_sample")
    suspend fun removeAllGsrSamples()


}