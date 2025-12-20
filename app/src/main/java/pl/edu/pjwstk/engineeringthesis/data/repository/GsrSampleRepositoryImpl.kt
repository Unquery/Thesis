package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import javax.inject.Inject

class GsrSampleRepositoryImpl @Inject constructor(
    private val diaryDB : DiaryDB
) : GsrSampleRepository{

    private val dao = diaryDB.gsrSamples

    override suspend fun upsert(sample: GsrSample) {
        dao.upsertGsrSample(sample.toEntity())
    }

    override suspend fun update(sample: GsrSample) {
        dao.updateGsrSample(sample.toEntity())
    }

    override suspend fun getById(id: Int): GsrSample? {
        return dao.getGsrSample(id)?.toDomain()
    }

    override suspend fun getAll(): List<GsrSample> {
        return dao.getAllTGsrSamples().map { it.toDomain() }
    }

    override suspend fun getRange(firstEpoch: Long, lastEpoch: Long): List<GsrSample> {
        return dao.getGsrSamples(firstEpoch, lastEpoch).map { it.toDomain() }
    }

    override suspend fun getRangeForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): List<GsrSample> {
        return dao.getGsrSamplesForUser(userId, firstEpoch, lastEpoch)
            .map { it.toDomain() }
    }

    override fun observeRangeForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): Flow<List<GsrSample>> {
        return dao.observeGsrSamplesForUser(userId, firstEpoch, lastEpoch)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun removeById(id: Int) {
        dao.removeGsrSample(id)
    }

    override suspend fun removeByEpoch(epoch: Long) {
        dao.removeGsrSamplesByEpoch(epoch)
    }

    override suspend fun removeAll() {
        dao.removeAllGsrSamples()
    }

    override fun observeHourlyAvg(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<List<HourlyAvg>> {
        return dao.observeHourlyAvg(userId, startEpoch, endEpoch)
    }

    override fun observeLatest(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<GsrSample?> {
        return dao.observeLatestInRange(userId, startEpoch, endEpoch)
            .map { it?.toDomain() }
    }


    override fun observeSummary(
        userId: Int,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<MetricSummary> {
        return dao.observeSummaryInRange(userId, startEpoch, endEpoch)
    }

    override suspend fun clear() {
        dao.removeAllGsrSamples()
    }

    private fun GsrSample.toEntity(): GsrSampleEntity =
        GsrSampleEntity(
            id = id,
            userId = userId,
            epoch = epoch,
            gsr = gsr
        )

}