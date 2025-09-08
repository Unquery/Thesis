package pl.edu.pjwstk.engineeringthesis.data.repository

import pl.edu.pjwstk.engineeringthesis.model.TempSample

interface GsrSampleRepository {
    suspend fun addGsrSample(tempSample : TempSample)
    suspend fun addGsrSample(tempSamples : List<TempSample>)
    suspend fun removeGsrSample(tempSample : TempSample)
    suspend fun removeGsrSamplesByEpoch(epoch : Long)
    suspend fun clear()
    suspend fun initDb()
}