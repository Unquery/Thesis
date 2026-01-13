package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import co.yml.charts.common.model.Point
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
import pl.edu.pjwstk.engineeringthesis.model.DailyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.util.ChartMetric
import pl.edu.pjwstk.engineeringthesis.util.ChartRange
import pl.edu.pjwstk.engineeringthesis.util.Charts
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.ZoneId
import javax.inject.Inject

data class ChartState(
    val range: ChartRange,
    val points: List<Point>,
    val dates: List<LocalDate>,
    val summary: ChartSummary,
    val baseDate: LocalDate
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
                        val pointsFlow = hourlyAvgFlow(metric, userId, start, end)
                        combine(pointsFlow, summaryFlow) { rows, summary ->
                            ChartState(
                                selectedRange,
                                toHourlyPoints(rows),
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
                        val pointsFlow = dailyAvgFlow(metric, userId, start, end)
                        combine(pointsFlow, summaryFlow) { rows, summary ->
                            ChartState(
                                selectedRange,
                                toDailyPoints(rows, dates),
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
                        val pointsFlow = dailyAvgFlow(metric, userId, start, end)
                        combine(pointsFlow, summaryFlow) { rows, summary ->
                            ChartState(
                                selectedRange,
                                toDailyPoints(rows, dates),
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

    private fun hourlyAvgFlow(
        metric: ChartMetric,
        userId: Int,
        start: Long,
        end: Long
    ): Flow<List<HourlyAvg>> = when (metric) {
        ChartMetric.Temperature -> tempRepo.observeHourlyAvg(userId, start, end)
        ChartMetric.HeartRate -> hrRepo.observeHourlyAvg(userId, start, end)
        ChartMetric.SpO2 -> spo2Repo.observeHourlyAvg(userId, start, end)
        ChartMetric.Gsr -> gsrRepo.observeHourlyAvg(userId, start, end)
    }

    private fun dailyAvgFlow(
        metric: ChartMetric,
        userId: Int,
        start: Long,
        end: Long
    ): Flow<List<DailyAvg>> = when (metric) {
        ChartMetric.Temperature -> tempRepo.observeDailyAvg(userId, start, end)
        ChartMetric.HeartRate -> hrRepo.observeDailyAvg(userId, start, end)
        ChartMetric.SpO2 -> spo2Repo.observeDailyAvg(userId, start, end)
        ChartMetric.Gsr -> gsrRepo.observeDailyAvg(userId, start, end)
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

    private fun toHourlyPoints(rows: List<HourlyAvg>): List<Point> {
        return rows
            .filter { it.avg != null }
            .sortedBy { it.hour }
            .mapNotNull { row ->
                val value = row.avg?.toFloat() ?: return@mapNotNull null
                Point(
                    x = row.hour.toFloat(),
                    y = value,
                    description = ""
                )
            }
    }

    private fun toDailyPoints(rows: List<DailyAvg>, dates: List<LocalDate>): List<Point> {
        if (rows.isEmpty()) return emptyList()
        val avgByDate = rows.associate { LocalDate.parse(it.date) to it.avg }
        return dates.mapIndexed { index, date ->
            val avg = avgByDate[date]
            val value = avg?.toFloat() ?: 0f
            Point(
                x = index.toFloat(),
                y = value,
                description = ""
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
