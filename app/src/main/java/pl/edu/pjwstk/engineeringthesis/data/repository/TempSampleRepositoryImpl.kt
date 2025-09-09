package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import javax.inject.Inject

class TempSampleRepositoryImpl @Inject constructor(
    private val diaryDB : DiaryDB
) : TempSampleRepository{

    private val _tempSampleItems = mutableListOf<TempSample>()

    val noteItems : List<TempSample>
        get() = _tempSampleItems

    override suspend fun getTempSample(id: Int): TempSample? = withContext(Dispatchers.IO) {
        return@withContext diaryDB.tempSamples.getTempSample(id)?.toDomain()
    }

    override suspend fun getTempSample (
        firstEpoch: Long,
        lastEpoch: Long
    ): List<TempSample> = withContext(Dispatchers.IO){
        return@withContext diaryDB.tempSamples.getTempSamples(firstEpoch, lastEpoch).map {
            it.toDomain()
        }
    }

    override suspend fun getAllTempSamples(): List<TempSample> = withContext(Dispatchers.IO) {
        return@withContext diaryDB.tempSamples.getAllTempSample().map{
            it.toDomain()
        }
    }

    override suspend fun addTempSample(tempSample: TempSample) = withContext(Dispatchers.IO) {
        val tempSampleItem = diaryDB.tempSamples.getTempSample(tempSample.id)
        if (tempSampleItem == null){
            diaryDB.tempSamples.addTempSample(
                TempSampleEntity(tempSample.id, tempSample.epoch, tempSample.temperature)
            )
        }else{
            diaryDB.tempSamples.updateTempSample(
                TempSampleEntity(tempSampleItem.id, tempSample.epoch, tempSample.temperature)
            )
        }
    }

    override suspend fun addTempSamples(tempSamples: List<TempSample>) = withContext(Dispatchers.IO) {
        tempSamples.forEach {
            val tempSampleItem = diaryDB.tempSamples.getTempSample(it.id)
            if (tempSampleItem == null){
                diaryDB.tempSamples.addTempSample(
                    TempSampleEntity(it.id, it.epoch, it.temperature)
                )
            }else{
                diaryDB.tempSamples.updateTempSample(
                    TempSampleEntity(tempSampleItem.id, it.epoch, it.temperature)
                )
            }
        }
    }

    override suspend fun removeTempSample(tempSample: TempSample) = withContext(Dispatchers.IO) {
        val tempSampleItem = diaryDB.tempSamples.getTempSample(tempSample.id)
        if(tempSampleItem != null){
            diaryDB.tempSamples.removeTempSample(tempSampleItem.id)
        }
    }

    override suspend fun removeTempSamplesByEpoch(epoch: Long) = withContext(Dispatchers.IO) {
        diaryDB.tempSamples.removeTempSamplesByEpoch(epoch)
    }

    override suspend fun clear() = withContext(Dispatchers.IO){
        diaryDB.tempSamples.removeAllTempSamples()
    }
}