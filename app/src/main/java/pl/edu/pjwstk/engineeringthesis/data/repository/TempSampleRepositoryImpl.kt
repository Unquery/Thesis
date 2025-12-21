package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import javax.inject.Inject

class TempSampleRepositoryImpl @Inject constructor(
    private val diaryDB: DiaryDB
) : TempSampleRepository {

    private val dao = diaryDB.tempSamples

    override suspend fun upsert(sample: TempSample) {
        dao.upsertTempSample(sample.toEntity())
    }

    override suspend fun update(sample: TempSample) {
        dao.updateTempSample(sample.toEntity())
    }

    override suspend fun getById(id: Int): TempSample? =
        dao.getTempSample(id)?.toDomain()

    override suspend fun getAll(): List<TempSample> =
        dao.getAllTempSamples().map { it.toDomain() }

    override suspend fun getRange(firstEpoch: Long, lastEpoch: Long): List<TempSample> =
        dao.getTempSamples(firstEpoch, lastEpoch).map { it.toDomain() }

    override suspend fun getRangeForUser(userId: Int, firstEpoch: Long, lastEpoch: Long): List<TempSample> =
        dao.getTempSamplesForUser(userId, firstEpoch, lastEpoch).map { it.toDomain() }

    override fun observeRangeForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): Flow<List<TempSample>> =
        dao.observeTempSamplesForUser(userId, firstEpoch, lastEpoch)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun removeById(id: Int) {
        dao.removeTempSample(id)
    }

    override suspend fun removeByEpoch(epoch: Long) {
        dao.removeTempSamplesByEpoch(epoch)
    }

    override suspend fun removeAll() {
        dao.removeAllTempSamples()
    }

    override fun observeHourlyAvg(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HourlyAvg>> =
        dao.observeHourlyAvg(userId, startEpoch, endEpoch)

    override fun observeLatest(userId: Int, startEpoch: Long, endEpoch: Long): Flow<TempSample?> =
        dao.observeLatestInRange(userId, startEpoch, endEpoch).map { it?.toDomain() }

    override fun observeSummary(userId: Int, startEpoch: Long, endEpoch: Long): Flow<MetricSummary> =
        dao.observeSummaryInRange(userId, startEpoch, endEpoch)

    override suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean {
        return dao.existsInRange(userId, startEpoch, endEpoch)
    }

    override suspend fun clear() {
        dao.removeAllTempSamples()
    }

    private fun TempSample.toEntity(): TempSampleEntity =
        TempSampleEntity(
            id = id,
            userId = userId,
            epoch = epoch,
            temperature = temperature
        )
}
