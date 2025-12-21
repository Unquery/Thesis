package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.entity.SpO2SampleEntity
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary
import pl.edu.pjwstk.engineeringthesis.model.SpO2Sample
import javax.inject.Inject

class SpO2SampleRepositoryImpl @Inject constructor(
    diaryDB: DiaryDB
) : SpO2SampleRepository {

    private val dao = diaryDB.spO2Samples

    override suspend fun upsert(sample: SpO2Sample) {
        dao.upsertSpO2Sample(sample.toEntity())
    }

    override suspend fun update(sample: SpO2Sample) {
        dao.updateSpO2Sample(sample.toEntity())
    }

    override suspend fun getById(id: Int): SpO2Sample? {
        return dao.getSpO2Sample(id)?.toDomain()
    }

    override suspend fun getAll(): List<SpO2Sample> {
        return dao.getAllSpO2Samples().map { it.toDomain() }
    }

    override suspend fun getRange(firstEpoch: Long, lastEpoch: Long): List<SpO2Sample> {
        return dao.getSpO2Samples(firstEpoch, lastEpoch).map { it.toDomain() }
    }

    override suspend fun getRangeForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): List<SpO2Sample> {
        return dao.getSpO2SamplesForUser(userId, firstEpoch, lastEpoch).map { it.toDomain() }
    }

    override fun observeRangeForUser(
        userId: Int,
        firstEpoch: Long,
        lastEpoch: Long
    ): Flow<List<SpO2Sample>> {
        return dao.observeSpO2SamplesForUser(userId, firstEpoch, lastEpoch)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun removeById(id: Int) {
        dao.removeSpO2Sample(id)
    }

    override suspend fun removeByEpoch(epoch: Long) {
        dao.removeSpO2SamplesByEpoch(epoch)
    }

    override suspend fun removeAll() {
        dao.removeAllSpO2Samples()
    }

    override fun observeHourlyAvg(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HourlyAvg>> {
        return dao.observeHourlyAvg(userId, startEpoch, endEpoch)
    }

    override fun observeLatest(userId: Int, startEpoch: Long, endEpoch: Long): Flow<SpO2Sample?> {
        return dao.observeLatestInRange(userId, startEpoch, endEpoch).map { it?.toDomain() }
    }

    override fun observeSummary(userId: Int, startEpoch: Long, endEpoch: Long): Flow<MetricSummary> {
        return dao.observeSummaryInRange(userId, startEpoch, endEpoch)
    }

    override suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean {
        return dao.existsInRange(userId, startEpoch, endEpoch)
    }

    override suspend fun clear() {
        dao.removeAllSpO2Samples()
    }

    private fun SpO2Sample.toEntity(): SpO2SampleEntity =
        SpO2SampleEntity(
            id = id,
            userId = userId,
            epoch = epoch,
            spo2 = spo2
        )
}
