package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import javax.inject.Inject

class TempSampleRepositoryImpl @Inject constructor(
    private val diaryDB: DiaryDB
) : TempSampleRepository {

    private val dao get() = diaryDB.tempSamples

    override suspend fun getTempSample(id: Int): TempSample? =
        dao.getTempSample(id)?.toDomain()

    override suspend fun getTempSample(firstEpoch: Long, lastEpoch: Long): List<TempSample> =
        dao.getTempSamples(firstEpoch, lastEpoch).map { it.toDomain() }

    override suspend fun getAllTempSamples(): List<TempSample> =
        dao.getAllTempSamples().map { it.toDomain() }

    override suspend fun addTempSample(tempSample: TempSample) {
        dao.upsertTempSample(
            TempSampleEntity(
                id = tempSample.id,
                epoch = tempSample.epoch,
                temperature = tempSample.temperature
            )
        )
    }

    override suspend fun addTempSamples(tempSamples: List<TempSample>) {
        if (tempSamples.isEmpty()) return
        dao.upsertTempSamples(
            tempSamples.map {
                TempSampleEntity(
                    id = it.id,
                    epoch = it.epoch,
                    temperature = it.temperature
                )
            }
        )
    }

    override suspend fun removeTempSample(tempSample: TempSample) {
        dao.removeTempSample(tempSample.id)
    }

    override suspend fun removeTempSamplesByEpoch(epoch: Long) {
        dao.removeTempSamplesByEpoch(epoch)
    }

    override suspend fun clear() {
        dao.removeAllTempSamples()
    }
}
