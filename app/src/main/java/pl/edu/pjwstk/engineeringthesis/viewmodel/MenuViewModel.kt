package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.edu.pjwstk.engineeringthesis.data.repository.GsrSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.HearthRateSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.SpO2SampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.TempSampleRepository
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.HearthRateSample
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyMinMax
import pl.edu.pjwstk.engineeringthesis.model.SpO2Sample
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.collections.associate
import kotlin.random.Random
import kotlin.math.abs

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val gsrRepo: GsrSampleRepository,
    private val profileRepo: ProfileRepository,
    private val hrRepo: HearthRateSampleRepository,
    private val spo2Repo: SpO2SampleRepository,
    private val tempRepo: TempSampleRepository
    ) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    private fun todayRangeMillis(): Pair<Long, Long> {
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    private fun to24Bars(rows: List<HourlyAvg>): List<Float?> {
        val map = rows.associate { it.hour to it.avg }
        return List(24) { hour -> map[hour]?.toFloat() }
    }

    private fun to24ExtremeBars(
        rows: List<HourlyMinMax>,
        normalMin: Float,
        normalMax: Float
    ): List<Float?> {
        val map = rows.associate { row ->
            row.hour to pickRepresentativeHourlyValue(row, normalMin, normalMax)
        }
        return List(24) { hour -> map[hour] }
    }

    private fun pickRepresentativeHourlyValue(
        row: HourlyMinMax,
        normalMin: Float,
        normalMax: Float
    ): Float? {
        val min = row.min?.toFloat()
        val max = row.max?.toFloat()
        if (min == null && max == null) return null
        if (min == null) return max
        if (max == null) return min

        val belowDeviation = (normalMin - min).coerceAtLeast(0f)
        val aboveDeviation = (max - normalMax).coerceAtLeast(0f)

        return when {
            belowDeviation > aboveDeviation -> min
            aboveDeviation > belowDeviation -> max
            else -> {
                val normalMid = (normalMin + normalMax) / 2f
                if (abs(min - normalMid) >= abs(max - normalMid)) min else max
            }
        }
    }

    val activeUserId: StateFlow<Int?> =
        profileRepo.observeActive()
            .map { it?.id }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun todayBars(
        hourlyAvgProvider: (userId: Int, start: Long, end: Long) -> Flow<List<HourlyAvg>>
    ): StateFlow<List<Float?>> =
        activeUserId
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(List(24) { null })
                } else {
                    val (start, end) = todayRangeMillis()
                    hourlyAvgProvider(userId, start, end).map(::to24Bars)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(24) { null })

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun todayExtremeBars(
        hourlyMinMaxProvider: (userId: Int, start: Long, end: Long) -> Flow<List<HourlyMinMax>>,
        normalMin: Float,
        normalMax: Float
    ): StateFlow<List<Float?>> =
        activeUserId
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(List(24) { null })
                } else {
                    val (start, end) = todayRangeMillis()
                    hourlyMinMaxProvider(userId, start, end)
                        .map { rows -> to24ExtremeBars(rows, normalMin, normalMax) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(24) { null })

    val todayGsrBars: StateFlow<List<Float?>> =
        todayBars { userId, start, end -> gsrRepo.observeHourlyAvg(userId, start, end) }

    val todayHrBars: StateFlow<List<Float?>> =
        todayBars { userId, start, end -> hrRepo.observeHourlyAvg(userId, start, end) }

    val todaySpo2Bars: StateFlow<List<Float?>> =
        todayBars { userId, start, end -> spo2Repo.observeHourlyAvg(userId, start, end) }

    val todayTempBars: StateFlow<List<Float?>> =
        todayBars { userId, start, end -> tempRepo.observeHourlyAvg(userId, start, end) }

    val todayGsrCardBars: StateFlow<List<Float?>> =
        todayExtremeBars(
            hourlyMinMaxProvider = { userId, start, end -> gsrRepo.observeHourlyMinMax(userId, start, end) },
            normalMin = 200f,
            normalMax = 900f
        )

    val todayHrCardBars: StateFlow<List<Float?>> =
        todayExtremeBars(
            hourlyMinMaxProvider = { userId, start, end -> hrRepo.observeHourlyMinMax(userId, start, end) },
            normalMin = 60f,
            normalMax = 100f
        )

    val todaySpo2CardBars: StateFlow<List<Float?>> =
        todayExtremeBars(
            hourlyMinMaxProvider = { userId, start, end -> spo2Repo.observeHourlyMinMax(userId, start, end) },
            normalMin = 97f,
            normalMax = 100f
        )

    val todayTempCardBars: StateFlow<List<Float?>> =
        todayExtremeBars(
            hourlyMinMaxProvider = { userId, start, end -> tempRepo.observeHourlyMinMax(userId, start, end) },
            normalMin = 36.5f,
            normalMax = 37.3f
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLatestEpoch: StateFlow<Long?> =
        activeUserId
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(null)
                } else {
                    val (start, end) = todayRangeMillis()
                    combine(
                        gsrRepo.observeLatest(userId, start, end).map { it?.epoch },
                        hrRepo.observeLatest(userId, start, end).map { it?.epoch },
                        spo2Repo.observeLatest(userId, start, end).map { it?.epoch },
                        tempRepo.observeLatest(userId, start, end).map { it?.epoch }
                    ) { gsr, hr, spo2, temp ->
                        listOfNotNull(gsr, hr, spo2, temp).maxOrNull()
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun seedMock() {
        seedMockActiveUserAndTodayAll(profileRepo, gsrRepo, hrRepo, spo2Repo, tempRepo)
    }

    init{
        seedMock()
    }

    //Mock functions
    private fun seedMockActiveUserAndTodayAll(
        profileRepo: ProfileRepository,
        gsrRepo: GsrSampleRepository,
        hrRepo: HearthRateSampleRepository,
        spo2Repo: SpO2SampleRepository,
        tempRepo: TempSampleRepository
    ) = viewModelScope.launch {

        val zone = ZoneId.of("Europe/Warsaw")
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val seedEnd = minOf(end, System.currentTimeMillis())

        val active = profileRepo.getActive()
        val userId = active?.id ?: profileRepo.insertAndActivate(
            UserProfile(
                id = 0,
                gender = "male",
                birthDateEpochDays = 10000L,
                heightCm = 180,
                weightKg = 75f,
                isActive = true
            )
        )

        val hasGsrToday = gsrRepo.existsInRange(userId, start, end)
        val hasHrToday = hrRepo.existsInRange(userId, start, end)
        val hasSpo2Today = spo2Repo.existsInRange(userId, start, end)
        val hasTempToday = tempRepo.existsInRange(userId, start, end)

        if (hasGsrToday && hasHrToday && hasSpo2Today && hasTempToday) return@launch

        val highHour = 14
        val lowHour = 4

        fun hourWindow(hour: Int): Pair<Long, Long> {
            val s = start + hour * 60 * 60 * 1000L
            val e = (s + 60 * 60 * 1000L).coerceAtMost(seedEnd)
            return s to e
        }

        val (highStart, highEnd) = hourWindow(highHour)
        val (lowStart, lowEnd) = hourWindow(lowHour)

        val rnd = Random(System.currentTimeMillis())
        val step = 10 * 60 * 1000L // one value every 10 minutes
        val seedPointCount = (((seedEnd - start) + step - 1) / step).toInt().coerceAtLeast(1)
        val spo2ModerateDipIndices = buildSet {
            val count = if (seedPointCount >= 18) 2 else 1
            repeat(count) {
                add(rnd.nextInt(seedPointCount))
            }
        }
        val spo2LowDipIndices = buildSet {
            val count = when {
                seedPointCount >= 36 -> 2
                seedPointCount >= 12 -> 1
                else -> 0
            }
            while (size < count) {
                val candidate = rnd.nextInt(seedPointCount)
                if (candidate !in spo2ModerateDipIndices) {
                    add(candidate)
                }
            }
        }

        var t = start
        var pointIndex = 0
        while (t < seedEnd) {
            val isHigh = (t >= highStart && t < highEnd)
            val isLow = !isHigh && (t >= lowStart && t < lowEnd)

            if (!hasGsrToday) {
                val gsrValue = when {
                    isHigh -> 1100 + rnd.nextInt(700)
                    isLow  -> 20 + rnd.nextInt(60)
                    else   -> 200 + rnd.nextInt(350)
                }
                gsrRepo.upsert(GsrSample(id = 0, userId = userId, epoch = t, gsr = gsrValue))
            }

            if (!hasHrToday) {
                val hrValue = when {
                    isHigh -> 125f + rnd.nextInt(35)
                    isLow  -> 42f + rnd.nextInt(10)
                    else   -> 55f + rnd.nextInt(35)
                }
                hrRepo.upsert(HearthRateSample(id = 0, userId = userId, epoch = t, hearthRate = hrValue))
            }

            if (!hasSpo2Today) {
                val spo2Value = when {
                    pointIndex in spo2LowDipIndices -> 90 + rnd.nextInt(5)
                    pointIndex in spo2ModerateDipIndices -> 95 + rnd.nextInt(3)
                    else -> 98 + rnd.nextInt(3)
                }.coerceIn(0, 100)
                spo2Repo.upsert(SpO2Sample(id = 0, userId = userId, epoch = t, spo2 = spo2Value))
            }

            if (!hasTempToday) {
                val tempValue = when {
                    isHigh -> 38.2f + (rnd.nextInt(11) / 10f)
                    isLow  -> 35.2f + (rnd.nextInt(6) / 10f)
                    else   -> 36.2f + (rnd.nextInt(8) / 10f)
                }
                tempRepo.upsert(TempSample(id = 0, userId = userId, epoch = t, temperature = tempValue))
            }

            t += step
            pointIndex++
        }
    }
}
