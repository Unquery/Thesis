package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import pl.edu.pjwstk.engineeringthesis.data.repository.GsrSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.HearthRateSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.SpO2SampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.TempSampleRepository
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.HourlyMinMax
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import pl.edu.pjwstk.engineeringthesis.util.GSR_NEUTRAL_MAX
import pl.edu.pjwstk.engineeringthesis.util.GSR_NEUTRAL_MIN
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.collections.associate
import kotlin.math.abs

data class MenuUiState(
    val gsrBars: List<Float?> = List(24) { null },
    val hrBars: List<Float?> = List(24) { null },
    val spo2Bars: List<Float?> = List(24) { null },
    val tempBars: List<Float?> = List(24) { null },
    val gsrCardBars: List<Float?> = List(24) { null },
    val hrCardBars: List<Float?> = List(24) { null },
    val spo2CardBars: List<Float?> = List(24) { null },
    val tempCardBars: List<Float?> = List(24) { null },
    val latestGsr: Float? = null,
    val previousGsr: Float? = null,
    val latestHr: Float? = null,
    val previousHr: Float? = null,
    val latestSpo2: Float? = null,
    val previousSpo2: Float? = null,
    val latestTemp: Float? = null,
    val previousTemp: Float? = null,
    val lastUpdatedEpoch: Long? = null
)

private data class MenuPrimaryBars(
    val gsrBars: List<Float?>,
    val hrBars: List<Float?>,
    val spo2Bars: List<Float?>,
    val tempBars: List<Float?>
)

private data class MenuCardBars(
    val gsrCardBars: List<Float?>,
    val hrCardBars: List<Float?>,
    val spo2CardBars: List<Float?>,
    val tempCardBars: List<Float?>
)

private data class LatestMeasurementPair(
    val current: Float? = null,
    val previous: Float? = null
)

private data class MenuLatestMeasurements(
    val gsr: LatestMeasurementPair = LatestMeasurementPair(),
    val hr: LatestMeasurementPair = LatestMeasurementPair(),
    val spo2: LatestMeasurementPair = LatestMeasurementPair(),
    val temp: LatestMeasurementPair = LatestMeasurementPair()
)

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

    val activeProfile: StateFlow<UserProfile?> =
        profileRepo.observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeUserId: StateFlow<Int?> =
        activeProfile
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
        normalRangeProvider: (UserProfile) -> Pair<Float, Float>
    ): StateFlow<List<Float?>> =
        activeProfile
            .flatMapLatest { profile ->
                if (profile == null) {
                    flowOf(List(24) { null })
                } else {
                    val userId = profile.id
                    val (start, end) = todayRangeMillis()
                    val (normalMin, normalMax) = normalRangeProvider(profile)
                    hourlyMinMaxProvider(userId, start, end)
                        .map { rows -> to24ExtremeBars(rows, normalMin, normalMax) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(24) { null })

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> todayLatestPair(
        latestTwoProvider: (userId: Int, start: Long, end: Long) -> Flow<List<T>>,
        valueSelector: (T) -> Float
    ): StateFlow<LatestMeasurementPair> =
        activeUserId
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(LatestMeasurementPair())
                } else {
                    val (start, end) = todayRangeMillis()
                    latestTwoProvider(userId, start, end).map { rows ->
                        LatestMeasurementPair(
                            current = rows.getOrNull(0)?.let(valueSelector),
                            previous = rows.getOrNull(1)?.let(valueSelector)
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LatestMeasurementPair())

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
            normalRangeProvider = { profile ->
                profile.skinConductanceNormalLow to profile.skinConductanceNormalHigh
            }
        )

    val todayHrCardBars: StateFlow<List<Float?>> =
        todayExtremeBars(
            hourlyMinMaxProvider = { userId, start, end -> hrRepo.observeHourlyMinMax(userId, start, end) },
            normalRangeProvider = { profile ->
                profile.heartRateNormalLow to profile.heartRateNormalHigh
            }
        )

    val todaySpo2CardBars: StateFlow<List<Float?>> =
        todayExtremeBars(
            hourlyMinMaxProvider = { userId, start, end -> spo2Repo.observeHourlyMinMax(userId, start, end) },
            normalRangeProvider = { profile ->
                profile.spO2NormalLow to profile.spO2NormalHigh
            }
        )

    val todayTempCardBars: StateFlow<List<Float?>> =
        todayExtremeBars(
            hourlyMinMaxProvider = { userId, start, end -> tempRepo.observeHourlyMinMax(userId, start, end) },
            normalRangeProvider = { profile ->
                profile.temperatureNormalLow to profile.temperatureNormalHigh
            }
        )

    private val latestGsrValues: StateFlow<LatestMeasurementPair> =
        todayLatestPair(
            latestTwoProvider = { userId, start, end -> gsrRepo.observeLatestTwo(userId, start, end) },
            valueSelector = { it.gsr }
        )

    private val latestHrValues: StateFlow<LatestMeasurementPair> =
        todayLatestPair(
            latestTwoProvider = { userId, start, end -> hrRepo.observeLatestTwo(userId, start, end) },
            valueSelector = { it.hearthRate }
        )

    private val latestSpo2Values: StateFlow<LatestMeasurementPair> =
        todayLatestPair(
            latestTwoProvider = { userId, start, end -> spo2Repo.observeLatestTwo(userId, start, end) },
            valueSelector = { it.spo2.toFloat() }
        )

    private val latestTempValues: StateFlow<LatestMeasurementPair> =
        todayLatestPair(
            latestTwoProvider = { userId, start, end -> tempRepo.observeLatestTwo(userId, start, end) },
            valueSelector = { it.temperature }
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

    private val primaryBars: StateFlow<MenuPrimaryBars> =
        combine(
            todayGsrBars,
            todayHrBars,
            todaySpo2Bars,
            todayTempBars
        ) { gsrBars, hrBars, spo2Bars, tempBars ->
            MenuPrimaryBars(
                gsrBars = gsrBars,
                hrBars = hrBars,
                spo2Bars = spo2Bars,
                tempBars = tempBars
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MenuPrimaryBars(
                gsrBars = List(24) { null },
                hrBars = List(24) { null },
                spo2Bars = List(24) { null },
                tempBars = List(24) { null }
            )
        )

    private val cardBars: StateFlow<MenuCardBars> =
        combine(
            todayGsrCardBars,
            todayHrCardBars,
            todaySpo2CardBars,
            todayTempCardBars
        ) { gsrCardBars, hrCardBars, spo2CardBars, tempCardBars ->
            MenuCardBars(
                gsrCardBars = gsrCardBars,
                hrCardBars = hrCardBars,
                spo2CardBars = spo2CardBars,
                tempCardBars = tempCardBars
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MenuCardBars(
                gsrCardBars = List(24) { null },
                hrCardBars = List(24) { null },
                spo2CardBars = List(24) { null },
                tempCardBars = List(24) { null }
            )
        )

    private val latestMeasurements: StateFlow<MenuLatestMeasurements> =
        combine(
            latestGsrValues,
            latestHrValues,
            latestSpo2Values,
            latestTempValues
        ) { gsr, hr, spo2, temp ->
            MenuLatestMeasurements(
                gsr = gsr,
                hr = hr,
                spo2 = spo2,
                temp = temp
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MenuLatestMeasurements()
        )

    val uiState: StateFlow<MenuUiState> =
        combine(
            primaryBars,
            cardBars,
            latestMeasurements,
            todayLatestEpoch
        ) { primaryBars, cardBars, latestMeasurements, lastUpdatedEpoch ->
            MenuUiState(
                gsrBars = primaryBars.gsrBars,
                hrBars = primaryBars.hrBars,
                spo2Bars = primaryBars.spo2Bars,
                tempBars = primaryBars.tempBars,
                gsrCardBars = cardBars.gsrCardBars,
                hrCardBars = cardBars.hrCardBars,
                spo2CardBars = cardBars.spo2CardBars,
                tempCardBars = cardBars.tempCardBars,
                latestGsr = latestMeasurements.gsr.current,
                previousGsr = latestMeasurements.gsr.previous,
                latestHr = latestMeasurements.hr.current,
                previousHr = latestMeasurements.hr.previous,
                latestSpo2 = latestMeasurements.spo2.current,
                previousSpo2 = latestMeasurements.spo2.previous,
                latestTemp = latestMeasurements.temp.current,
                previousTemp = latestMeasurements.temp.previous,
                lastUpdatedEpoch = lastUpdatedEpoch
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState())
}
