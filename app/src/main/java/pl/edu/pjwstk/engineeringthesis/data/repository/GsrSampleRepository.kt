package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import pl.edu.pjwstk.engineeringthesis.model.DailyAvg
import pl.edu.pjwstk.engineeringthesis.model.DailyMinMax
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyMinMax
import pl.edu.pjwstk.engineeringthesis.model.MetricSummary

interface GsrSampleRepository {
    // write
    suspend fun upsert(sample: GsrSample)
    suspend fun update(sample: GsrSample)

    // read
    suspend fun getById(id: Int): GsrSample?
    suspend fun getAll(): List<GsrSample>
    suspend fun getRange(firstEpoch: Long, lastEpoch: Long): List<GsrSample>
    suspend fun getRangeForUser(userId: Int, firstEpoch: Long, lastEpoch: Long): List<GsrSample>

    fun observeRangeForUser(userId: Int, firstEpoch: Long, lastEpoch: Long): Flow<List<GsrSample>>
    suspend fun existsInRange(userId: Int, startEpoch: Long, endEpoch: Long): Boolean

    // delete
    suspend fun removeById(id: Int)
    suspend fun removeByEpoch(epoch: Long)
    suspend fun removeAll()

    // dashboard helpers
    fun observeHourlyAvg(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HourlyAvg>>
    fun observeDailyAvg(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<DailyAvg>>
    fun observeHourlyMinMax(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<HourlyMinMax>>
    fun observeDailyMinMax(userId: Int, startEpoch: Long, endEpoch: Long): Flow<List<DailyMinMax>>
    fun observeLatest(userId: Int, startEpoch: Long, endEpoch: Long): Flow<GsrSample?>
    fun observeSummary(userId: Int, startEpoch: Long, endEpoch: Long): Flow<MetricSummary>

    suspend fun clear()
}
