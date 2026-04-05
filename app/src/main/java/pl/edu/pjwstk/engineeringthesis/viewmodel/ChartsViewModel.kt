package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.edu.pjwstk.engineeringthesis.data.repository.GsrSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.HearthRateSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.SpO2SampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.TempSampleRepository
import pl.edu.pjwstk.engineeringthesis.model.DailyMinMax
import pl.edu.pjwstk.engineeringthesis.model.HourlyMinMax
import pl.edu.pjwstk.engineeringthesis.util.ChartMetric
import pl.edu.pjwstk.engineeringthesis.util.ChartRange
import pl.edu.pjwstk.engineeringthesis.util.Charts
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.ZoneId
import javax.inject.Inject

data class ChartState(
    val range: ChartRange,
    val points: List<ChartBucketRange>,
    val dates: List<LocalDate>,
    val summary: ChartSummary,
    val baseDate: LocalDate
)

data class ChartBucketRange(
    val x: Float,
    val minY: Float,
    val maxY: Float
)

data class ChartSummary(
    val avg: Float?,
    val min: Float?,
    val max: Float?
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val gsrRepo: GsrSampleRepository,
    private val hrRepo: HearthRateSampleRepository,
    private val spo2Repo: SpO2SampleRepository,
    private val tempRepo: TempSampleRepository,
    private val profileRepo: ProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val metric: ChartMetric = savedStateHandle.toRoute<Charts>().metric

    private val zone = ZoneId.systemDefault()

    private val activeUserId: StateFlow<Int?> =
        profileRepo.observeActive()
            .map { it?.id }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val range = MutableStateFlow(ChartRange.Day)
    private val selectedDate = MutableStateFlow(LocalDate.now(zone))

    fun setRange(newRange: ChartRange) {
        range.value = newRange
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDate.value = date
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val chartState: StateFlow<ChartState> =
        combine(activeUserId, range, selectedDate) { userId, selectedRange, baseDate ->
            Triple(userId, selectedRange, baseDate)
        }.flatMapLatest { (userId, selectedRange, baseDate) ->
            if (userId == null) {
                flowOf(
                    ChartState(
                        selectedRange,
                        emptyList(),
                        emptyList(),
                        ChartSummary(null, null, null),
                        baseDate
                    )
                )
            } else {
                when (selectedRange) {
                    ChartRange.Day -> {
                        val (start, end) = rangeMillis(baseDate, baseDate.plusDays(1))
                        val summaryFlow = rawSummaryFlow(metric, userId, start, end)
                        val pointsFlow = hourlyMinMaxFlow(metric, userId, start, end)
                        combine(pointsFlow, summaryFlow) { rows, summary ->
                            ChartState(
                                selectedRange,
                                toHourlyBuckets(rows),
                                emptyList(),
                                summary,
                                baseDate
                            )
                        }
                    }

                    ChartRange.Week -> {
                        val dates = weekDates(baseDate)
                        val (start, end) = rangeMillis(dates.first(), dates.last().plusDays(1))
                        val summaryFlow = rawSummaryFlow(metric, userId, start, end)
                        val pointsFlow = dailyMinMaxFlow(metric, userId, start, end)
                        combine(pointsFlow, summaryFlow) { rows, summary ->
                            ChartState(
                                selectedRange,
                                toDailyBuckets(rows, dates),
                                dates,
                                summary,
                                baseDate
                            )
                        }
                    }

                    ChartRange.Month -> {
                        val dates = monthDates(baseDate)
                        val (start, end) = rangeMillis(dates.first(), dates.last().plusDays(1))
                        val summaryFlow = rawSummaryFlow(metric, userId, start, end)
                        val pointsFlow = dailyMinMaxFlow(metric, userId, start, end)
                        combine(pointsFlow, summaryFlow) { rows, summary ->
                            ChartState(
                                selectedRange,
                                toDailyBuckets(rows, dates),
                                dates,
                                summary,
                                baseDate
                            )
                        }
                    }
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ChartState(
                ChartRange.Day,
                emptyList(),
                emptyList(),
                ChartSummary(null, null, null),
                LocalDate.now(zone)
            )
        )

    private fun hourlyMinMaxFlow(
        metric: ChartMetric,
        userId: Int,
        start: Long,
        end: Long
    ): Flow<List<HourlyMinMax>> = when (metric) {
        ChartMetric.Temperature -> tempRepo.observeHourlyMinMax(userId, start, end)
        ChartMetric.HeartRate -> hrRepo.observeHourlyMinMax(userId, start, end)
        ChartMetric.SpO2 -> spo2Repo.observeHourlyMinMax(userId, start, end)
        ChartMetric.Gsr -> gsrRepo.observeHourlyMinMax(userId, start, end)
    }

    private fun dailyMinMaxFlow(
        metric: ChartMetric,
        userId: Int,
        start: Long,
        end: Long
    ): Flow<List<DailyMinMax>> = when (metric) {
        ChartMetric.Temperature -> tempRepo.observeDailyMinMax(userId, start, end)
        ChartMetric.HeartRate -> hrRepo.observeDailyMinMax(userId, start, end)
        ChartMetric.SpO2 -> spo2Repo.observeDailyMinMax(userId, start, end)
        ChartMetric.Gsr -> gsrRepo.observeDailyMinMax(userId, start, end)
    }

    private fun rawSummaryFlow(
        metric: ChartMetric,
        userId: Int,
        start: Long,
        end: Long
    ): Flow<ChartSummary> = when (metric) {
        ChartMetric.Temperature -> tempRepo.observeRangeForUser(userId, start, end)
            .map { rows -> summaryFromValues(rows.map { it.temperature }) }
        ChartMetric.HeartRate -> hrRepo.observeRangeForUser(userId, start, end)
            .map { rows -> summaryFromValues(rows.map { it.hearthRate }) }
        ChartMetric.SpO2 -> spo2Repo.observeRangeForUser(userId, start, end)
            .map { rows -> summaryFromValues(rows.map { it.spo2.toFloat() }) }
        ChartMetric.Gsr -> gsrRepo.observeRangeForUser(userId, start, end)
            .map { rows -> summaryFromValues(rows.map { it.gsr.toFloat() }) }
    }

    private fun toHourlyBuckets(rows: List<HourlyMinMax>): List<ChartBucketRange> {
        return rows
            .sortedBy { it.hour }
            .mapNotNull { row ->
                val min = row.min?.toFloat() ?: return@mapNotNull null
                val max = row.max?.toFloat() ?: return@mapNotNull null
                ChartBucketRange(
                    x = row.hour * 2f,
                    minY = min,
                    maxY = max
                )
            }
    }

    private fun toDailyBuckets(rows: List<DailyMinMax>, dates: List<LocalDate>): List<ChartBucketRange> {
        if (rows.isEmpty()) return emptyList()
        val rowsByDate = rows.associateBy { LocalDate.parse(it.date) }
        return dates.mapIndexedNotNull { index, date ->
            val row = rowsByDate[date] ?: return@mapIndexedNotNull null
            val min = row.min?.toFloat() ?: return@mapIndexedNotNull null
            val max = row.max?.toFloat() ?: return@mapIndexedNotNull null
            ChartBucketRange(
                x = index.toFloat(),
                minY = min,
                maxY = max
            )
        }
    }

    private fun summaryFromValues(values: List<Float>): ChartSummary {
        if (values.isEmpty()) return ChartSummary(null, null, null)
        val avg = values.average().toFloat()
        val min = values.minOrNull()
        val max = values.maxOrNull()
        return ChartSummary(avg, min, max)
    }

    private fun weekDates(baseDate: LocalDate): List<LocalDate> {
        val start = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { start.plusDays(it.toLong()) }
    }

    private fun monthDates(baseDate: LocalDate): List<LocalDate> {
        val start = baseDate.withDayOfMonth(1)
        val days = start.lengthOfMonth()
        return (0 until days).map { start.plusDays(it.toLong()) }
    }

    private fun rangeMillis(start: LocalDate, endExclusive: LocalDate): Pair<Long, Long> {
        val startEpoch = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endEpoch = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        return startEpoch to endEpoch
    }

}
