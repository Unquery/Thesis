package pl.edu.pjwstk.engineeringthesis.data.repository

import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import javax.inject.Inject

class TempSampleRepositoryImpl @Inject constructor(
    private val diaryDB : DiaryDB
) : TempSampleRepository{

    private val _tempSampleItems = mutableListOf<TempSample>()

    val noteItems : List<TempSample>
        get() = _tempSampleItems

    override suspend fun addTempSample(tempSample: TempSample) {
        TODO("Not yet implemented")
    }

    override suspend fun addTempSamples(tempSamples: List<TempSample>) {
        TODO("Not yet implemented")
    }

    override suspend fun removeTempSample(tempSample: TempSample) {
        TODO("Not yet implemented")
    }

    override suspend fun removeTempSamplesByEpoch(epoch: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun clear() {
        TODO("Not yet implemented")
    }

    override suspend fun initDb() {
        TODO("Not yet implemented")
    }
}