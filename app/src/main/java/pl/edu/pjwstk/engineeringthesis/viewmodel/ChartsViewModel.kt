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
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import pl.edu.pjwstk.engineeringthesis.util.ChartMetric
import pl.edu.pjwstk.engineeringthesis.util.ChartRange
import pl.edu.pjwstk.engineeringthesis.util.Charts
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_HEART_RATE_MIN
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_SKIN_CONDUCTANCE_MIN
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_SPO2_MIN
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_TEMPERATURE_MIN
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.time.ZoneId
import javax.inject.Inject

data class ChartState(
    val range: ChartRange,
    val points: List<ChartBucketRange>,
    val linePoints: List<ChartLinePoint>,
    val dates: List<LocalDate>,
    val summary: ChartSummary,
    val baseDate: LocalDate,
    val selectedHour: Int
)

data class ChartBucketRange(
    val x: Float,
    val minY: Float,
    val maxY: Float
)

data class ChartLinePoint(
    val x: Float,
    val y: Float
)

data class ChartSummary(
    val avg: Float?,
    val min: Float?,
    val max: Float?
)

private data class TimedMetricValue(
    val epoch: Long,
    val value: Float
)

private data class ChartQuery(
    val userId: Int?,
    val range: ChartRange,
    val baseDate: LocalDate,
    val selectedHour: Int
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

    val activeProfile: StateFlow<UserProfile?> =
        profileRepo.observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val activeUserId: StateFlow<Int?> =
        activeProfile
            .map { it?.id }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val range = MutableStateFlow(ChartRange.Day)
    private val selectedDate = MutableStateFlow(LocalDate.now(zone))
    private val selectedHour = MutableStateFlow(LocalTime.now(zone).hour)

    fun setRange(newRange: ChartRange) {
        range.value = newRange
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun setSelectedHour(hour: Int) {
        selectedHour.value = hour.coerceIn(0, 23)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val chartState: StateFlow<ChartState> =
        combine(activeUserId, range, selectedDate, selectedHour) { userId, selectedRange, baseDate, pickedHour ->
            ChartQuery(
                userId = userId,
                range = selectedRange,
                baseDate = baseDate,
                selectedHour = pickedHour
            )
        }.flatMapLatest { query ->
            val userId = query.userId
            val selectedRange = query.range
            val baseDate = query.baseDate
            val pickedHour = query.selectedHour
            if (userId == null) {
                flowOf(
                    ChartState(
                        range = selectedRange,
                        points = emptyList(),
                        linePoints = emptyList(),
                        dates = emptyList(),
                        summary = ChartSummary(null, null, null),
                        baseDate = baseDate,
                        selectedHour = pickedHour
                    )
                )
            } else {
                when (selectedRange) {
                    ChartRange.Hour -> {
                        val (start, end) = hourMillis(baseDate, pickedHour)
                        rawChartValuesFlow(metric, userId, start, end).map { rows ->
                            ChartState(
                                range = selectedRange,
                                points = emptyList(),
                                linePoints = toMinutePoints(rows),
                                dates = emptyList(),
                                summary = summaryFromValues(rows.map { it.value }),
                                baseDate = baseDate,
                                selectedHour = pickedHour
                            )
                        }
                    }

                    ChartRange.Day -> {
                        val (start, end) = rangeMillis(baseDate, baseDate.plusDays(1))
                        rawChartValuesFlow(metric, userId, start, end).map { rows ->
                            ChartState(
                                range = selectedRange,
                                points = toHourlyBuckets(rows),
                                linePoints = emptyList(),
                                dates = emptyList(),
                                summary = summaryFromValues(rows.map { it.value }),
                                baseDate = baseDate,
                                selectedHour = pickedHour
                            )
                        }
                    }

                    ChartRange.Week -> {
                        val dates = weekDates(baseDate)
                        val (start, end) = rangeMillis(dates.first(), dates.last().plusDays(1))
                        rawChartValuesFlow(metric, userId, start, end).map { rows ->
                            ChartState(
                                range = selectedRange,
                                points = toDailyBuckets(rows, dates),
                                linePoints = emptyList(),
                                dates = dates,
                                summary = summaryFromValues(rows.map { it.value }),
                                baseDate = baseDate,
                                selectedHour = pickedHour
                            )
                        }
                    }

                    ChartRange.Month -> {
                        val dates = monthDates(baseDate)
                        val (start, end) = rangeMillis(dates.first(), dates.last().plusDays(1))
                        rawChartValuesFlow(metric, userId, start, end).map { rows ->
                            ChartState(
                                range = selectedRange,
                                points = toDailyBuckets(rows, dates),
                                linePoints = emptyList(),
                                dates = dates,
                                summary = summaryFromValues(rows.map { it.value }),
                                baseDate = baseDate,
                                selectedHour = pickedHour
                            )
                        }
                    }
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ChartState(
                range = ChartRange.Day,
                points = emptyList(),
                linePoints = emptyList(),
                dates = emptyList(),
                summary = ChartSummary(null, null, null),
                baseDate = LocalDate.now(zone),
                selectedHour = LocalTime.now(zone).hour
            )
        )

    private fun rawChartValuesFlow(
        metric: ChartMetric,
        userId: Int,
        start: Long,
        end: Long
    ): Flow<List<TimedMetricValue>> {
        val minVisibleValue = minVisibleChartValue(metric)
        return when (metric) {
        ChartMetric.Temperature -> tempRepo.observeRangeForUser(userId, start, end)
            .map { rows ->
                rows.map { TimedMetricValue(epoch = it.epoch, value = it.temperature) }
                    .filter { it.value >= minVisibleValue }
            }
        ChartMetric.HeartRate -> hrRepo.observeRangeForUser(userId, start, end)
            .map { rows ->
                rows.map { TimedMetricValue(epoch = it.epoch, value = it.hearthRate) }
                    .filter { it.value >= minVisibleValue }
            }
        ChartMetric.SpO2 -> spo2Repo.observeRangeForUser(userId, start, end)
            .map { rows ->
                rows.map { TimedMetricValue(epoch = it.epoch, value = it.spo2.toFloat()) }
                    .filter { it.value >= minVisibleValue }
            }
        ChartMetric.Gsr -> gsrRepo.observeRangeForUser(userId, start, end)
            .map { rows ->
                rows.map { TimedMetricValue(epoch = it.epoch, value = it.gsr) }
                    .filter { it.value >= minVisibleValue }
            }
        }
    }

    private fun minVisibleChartValue(metric: ChartMetric): Float = when (metric) {
        ChartMetric.Temperature -> PROFILE_CALIBRATION_TEMPERATURE_MIN
        ChartMetric.HeartRate -> PROFILE_CALIBRATION_HEART_RATE_MIN
        ChartMetric.SpO2 -> PROFILE_CALIBRATION_SPO2_MIN
        ChartMetric.Gsr -> PROFILE_CALIBRATION_SKIN_CONDUCTANCE_MIN
    }

    private fun toHourlyBuckets(rows: List<TimedMetricValue>): List<ChartBucketRange> {
        return rows
            .groupBy { timedValue ->
                Instant.ofEpochMilli(timedValue.epoch).atZone(zone).hour
            }
            .toSortedMap()
            .mapNotNull { (hour, samples) ->
                val values = samples.map { it.value }
                val min = values.minOrNull() ?: return@mapNotNull null
                val max = values.maxOrNull() ?: return@mapNotNull null
                ChartBucketRange(
                    x = hour * 2f,
                    minY = min,
                    maxY = max
                )
            }
    }

    private fun toMinutePoints(rows: List<TimedMetricValue>): List<ChartLinePoint> {
        return rows
            .groupBy { timedValue ->
                Instant.ofEpochMilli(timedValue.epoch).atZone(zone).minute
            }
            .toSortedMap()
            .mapNotNull { (minute, samples) ->
                val avg = samples.map { it.value }.average().toFloat()
                ChartLinePoint(
                    x = minute.toFloat(),
                    y = avg
                )
            }
    }

    private fun toDailyBuckets(rows: List<TimedMetricValue>, dates: List<LocalDate>): List<ChartBucketRange> {
        if (rows.isEmpty()) return emptyList()
        val rowsByDate = rows.groupBy { timedValue ->
            Instant.ofEpochMilli(timedValue.epoch).atZone(zone).toLocalDate()
        }
        return dates.mapIndexedNotNull { index, date ->
            val samples = rowsByDate[date] ?: return@mapIndexedNotNull null
            val values = samples.map { it.value }
            val min = values.minOrNull() ?: return@mapIndexedNotNull null
            val max = values.maxOrNull() ?: return@mapIndexedNotNull null
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

    private fun hourMillis(baseDate: LocalDate, hour: Int): Pair<Long, Long> {
        val start = baseDate.atStartOfDay(zone).plusHours(hour.toLong())
        return start.toInstant().toEpochMilli() to start.plusHours(1).toInstant().toEpochMilli()
    }

}
