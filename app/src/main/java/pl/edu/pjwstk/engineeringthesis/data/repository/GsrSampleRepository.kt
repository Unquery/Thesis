package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.TempSample

interface GsrSampleRepository {
    suspend fun getGsrSample(id: Int) : GsrSample?
    suspend fun getGsrSamples(firstEpoch : Long, lastEpoch : Long) : List<GsrSample>
    suspend fun getAllGsrSamples() : List<GsrSample>
    suspend fun addGsrSample(tempSample : TempSample)
    suspend fun addGsrSample(tempSamples : List<TempSample>)
    suspend fun removeGsrSample(tempSample : TempSample)
    suspend fun removeGsrSamplesByEpoch(epoch : Long)
    suspend fun observeHourlyAvg(userId: Int,
                                 startEpoch: Long,
                                 endEpoch: Long): Flow<List<HourlyAvg>>
    suspend fun clear()
}