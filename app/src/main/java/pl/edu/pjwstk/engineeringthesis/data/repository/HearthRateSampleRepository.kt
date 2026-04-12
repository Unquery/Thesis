package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.model.DailyAvg
import pl.edu.pjwstk.engineeringthesis.model.DailyMinMax
import pl.edu.pjwstk.engineeringthesis.model.HearthRateSample
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyMinMax
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary

interface HearthRateSampleRepository {
    suspend fun upsert(sample: HearthRateSample)
    suspend fun update(sample: HearthRateSample)

    suspend fun getById(id: Int): HearthRateSample?
    suspend fun getAll(): List<HearthRateSample>

    suspend fun getRange(firstEpoch: Long, lastEpoch: Long): List<HearthRateSample>

    suspend fun getRangeForUser(userId: Int, firstEpoch: Long, lastEpoch: Long): List<HearthRateSample>
    fun observeRangeForUser(userId: Int, firstEpoch: Long, lastEpoch: Long): Flow<List<HearthRateSample>>
    suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean

    suspend fun removeById(id: Int)
    suspend fun removeByEpoch(epoch: Long)
    suspend fun removeAll()

    fun observeHourlyAvg(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HourlyAvg>>
    fun observeDailyAvg(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<DailyAvg>>
    fun observeHourlyMinMax(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HourlyMinMax>>
    fun observeDailyMinMax(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<DailyMinMax>>
    fun observeLatest(userId: Int, startEpoch: Long, endEpoch: Long): Flow<HearthRateSample?>
    fun observeLatestTwo(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HearthRateSample>>
    fun observeSummary(userId: Int, startEpoch: Long, endEpoch: Long): Flow<MetricSummary>

    suspend fun clear()
}
