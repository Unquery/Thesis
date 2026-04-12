package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.model.DailyAvg
import pl.edu.pjwstk.engineeringthesis.model.DailyMinMax
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyMinMax
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary
import pl.edu.pjwstk.engineeringthesis.model.SpO2Sample

interface SpO2SampleRepository {
    suspend fun upsert(sample: SpO2Sample)
    suspend fun update(sample: SpO2Sample)

    suspend fun getById(id: Int): SpO2Sample?
    suspend fun getAll(): List<SpO2Sample>

    suspend fun getRange(firstEpoch: Long, lastEpoch: Long): List<SpO2Sample>

    suspend fun getRangeForUser(userId: Int, firstEpoch: Long, lastEpoch: Long): List<SpO2Sample>
    fun observeRangeForUser(userId: Int, firstEpoch: Long, lastEpoch: Long): Flow<List<SpO2Sample>>
    suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean

    suspend fun removeById(id: Int)
    suspend fun removeByEpoch(epoch: Long)
    suspend fun removeAll()

    fun observeHourlyAvg(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HourlyAvg>>
    fun observeDailyAvg(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<DailyAvg>>
    fun observeHourlyMinMax(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HourlyMinMax>>
    fun observeDailyMinMax(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<DailyMinMax>>
    fun observeLatest(userId: Int, startEpoch: Long, endEpoch: Long): Flow<SpO2Sample?>
    fun observeLatestTwo(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<SpO2Sample>>
    fun observeSummary(userId: Int, startEpoch: Long, endEpoch: Long): Flow<MetricSummary>

    suspend fun clear()
}
