package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.entity.HearthRateSampleEntity
import pl.edu.pjwstk.engineeringthesis.model.HearthRateSample
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary
import javax.inject.Inject

class HearthRateSampleRepositoryImpl @Inject constructor(
    diaryDB: DiaryDB
) : HearthRateSampleRepository {

    private val dao = diaryDB.hearthRateSamples // add this to DiaryDB

    override suspend fun upsert(sample: HearthRateSample) {
        dao.upsert(sample.toEntity())
    }

    override suspend fun update(sample: HearthRateSample) {
        dao.update(sample.toEntity())
    }

    override suspend fun getById(id: Int): HearthRateSample? =
        dao.getById(id)?.toDomain()

    override suspend fun getAll(): List<HearthRateSample> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getRange(firstEpoch: Long, lastEpoch: Long): List<HearthRateSample> =
        dao.getRange(firstEpoch, lastEpoch).map { it.toDomain() }

    override suspend fun getRangeForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): List<HearthRateSample> =
        dao.getRangeForUser(userId, firstEpoch, lastEpoch).map { it.toDomain() }

    override fun observeRangeForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): Flow<List<HearthRateSample>> =
        dao.observeRangeForUser(userId, firstEpoch, lastEpoch)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun removeById(id: Int) {
        dao.removeById(id)
    }

    override suspend fun removeByEpoch(epoch: Long) {
        dao.removeByEpoch(epoch)
    }

    override suspend fun removeAll() {
        dao.removeAll()
    }

    override fun observeHourlyAvg(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HourlyAvg>> =
        dao.observeHourlyAvg(userId, startEpoch, endEpoch)

    override fun observeLatest(userId: Int, startEpoch: Long, endEpoch: Long): Flow<HearthRateSample?> =
        dao.observeLatestInRange(userId, startEpoch, endEpoch).map { it?.toDomain() }

    override fun observeSummary(userId: Int, startEpoch: Long, endEpoch: Long): Flow<MetricSummary> =
        dao.observeSummaryInRange(userId, startEpoch, endEpoch)

    override suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean {
        return dao.existsInRange(userId, startEpoch, endEpoch)
    }

    override suspend fun clear() {
        dao.removeAll()
    }

    private fun HearthRateSample.toEntity(): HearthRateSampleEntity =
        HearthRateSampleEntity(
            id = id,
            userId = userId,
            epoch = epoch,
            hearthRate = hearthRate
        )
}