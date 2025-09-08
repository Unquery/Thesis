package pl.edu.pjwstk.engineeringthesis.data.repository

import pl.edu.pjwstk.engineeringthesis.model.TempSample

interface TempSampleRepository {
    suspend fun addTempSample(tempSample : TempSample)
    suspend fun addTempSamples(tempSamples : List<TempSample>)
    suspend fun removeTempSample(tempSample : TempSample)
    suspend fun removeTempSamplesByEpoch(epoch : Long)
    suspend fun clear()
    suspend fun initDb()
}