package pl.edu.pjwstk.engineeringthesis.data.repository

import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import javax.inject.Inject

class GsrSampleRepositoryImpl @Inject constructor(
    private val diaryDB : DiaryDB
) : GsrSampleRepository{

    private val _gsrSampleItems = mutableListOf<GsrSample>()

    val gsrItems : List<GsrSample>
        get() = _gsrSampleItems

    override suspend fun getGsrSample(id: Int): GsrSample? {
        TODO("Not yet implemented")
    }

    override suspend fun getGsrSamples(
        firstEpoch: Long,
        lastEpoch: Long
    ): List<GsrSample> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllGsrSamples(): List<GsrSample> {
        TODO("Not yet implemented")
    }

    override suspend fun addGsrSample(tempSample: TempSample) {
        TODO("Not yet implemented")
    }

    override suspend fun addGsrSample(tempSamples: List<TempSample>) {
        TODO("Not yet implemented")
    }

    override suspend fun removeGsrSample(tempSample: TempSample) {
        TODO("Not yet implemented")
    }

    override suspend fun removeGsrSamplesByEpoch(epoch: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun clear() {
        TODO("Not yet implemented")
    }
}