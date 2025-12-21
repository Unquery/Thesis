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
import pl.edu.pjwstk.engineeringthesis.model.SpO2Sample
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.collections.associate
import kotlin.random.Random

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

    val todayGsrBars: StateFlow<List<Float?>> =
        todayBars { userId, start, end -> gsrRepo.observeHourlyAvg(userId, start, end) }

    val todayHrBars: StateFlow<List<Float?>> =
        todayBars { userId, start, end -> hrRepo.observeHourlyAvg(userId, start, end) }

    val todaySpo2Bars: StateFlow<List<Float?>> =
        todayBars { userId, start, end -> spo2Repo.observeHourlyAvg(userId, start, end) }

    val todayTempBars: StateFlow<List<Float?>> =
        todayBars { userId, start, end -> tempRepo.observeHourlyAvg(userId, start, end) }

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

        // 1) Get active user or create+activate one
        val active = profileRepo.getActive()
        val userId = active?.id ?: profileRepo.insertAndActivate(
            UserProfile(
                id = 0,
                gender = "male",
                birthDateEpochDays = 10000L,
                heightCm = 180,
                isActive = true
            )
        )

        val hasGsrToday = gsrRepo.existsInRange(userId, start, end)
        val hasHrToday = hrRepo.existsInRange(userId, start, end)
        val hasSpo2Today = spo2Repo.existsInRange(userId, start, end)
        val hasTempToday = tempRepo.existsInRange(userId, start, end)

        if (hasGsrToday && hasHrToday && hasSpo2Today && hasTempToday) return@launch

        // ---- Spike window: one hour where values are much higher ----
        val spikeHour = 14 // 14:00–15:00 (change to whatever you want)
        val spikeStart = start + spikeHour * 60 * 60 * 1000L
        val spikeEnd = (spikeStart + 60 * 60 * 1000L).coerceAtMost(end)

        val rnd = Random(System.currentTimeMillis())
        val step = 10 * 60 * 1000L // ✅ one value every 10 minutes

        var t = start
        while (t < end) {
            val isSpike = (t >= spikeStart && t < spikeEnd)

            if (!hasGsrToday) {
                val gsrValue = if (isSpike) {
                    1100 + rnd.nextInt(700)   // 1100..1799 (spike)
                } else {
                    200 + rnd.nextInt(350)    // 200..549 (normal)
                }
                gsrRepo.upsert(GsrSample(id = 0, userId = userId, epoch = t, gsr = gsrValue))
            }

            if (!hasHrToday) {
                val hrValue = if (isSpike) {
                    125f + rnd.nextInt(35)    // 125..159 bpm (spike)
                } else {
                    55f + rnd.nextInt(35)     // 55..89 bpm (normal)
                }
                hrRepo.upsert(
                    HearthRateSample(
                        id = 0,
                        userId = userId,
                        epoch = t,
                        hearthRate = hrValue
                    )
                )
            }

            if (!hasSpo2Today) {
                val spo2Value = if (isSpike) {
                    98 + rnd.nextInt(3)       // 98..100 (spike/high)
                } else {
                    92 + rnd.nextInt(6)       // 92..97 (normal)
                }
                spo2Repo.upsert(SpO2Sample(id = 0, userId = userId, epoch = t, spo2 = spo2Value))
            }

            if (!hasTempToday) {
                val tempValue = if (isSpike) {
                    38.2f + (rnd.nextInt(11) / 10f)  // 38.2..39.2 (spike)
                } else {
                    36.2f + (rnd.nextInt(8) / 10f)   // 36.2..36.9 (normal)
                }
                tempRepo.upsert(
                    TempSample(
                        id = 0,
                        userId = userId,
                        epoch = t,
                        temperature = tempValue
                    )
                )
            }

            t += step
        }
    }
}
