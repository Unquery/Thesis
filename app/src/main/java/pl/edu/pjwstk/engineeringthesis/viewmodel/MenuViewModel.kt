package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.edu.pjwstk.engineeringthesis.data.repository.GsrSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.collections.associate
import kotlin.random.Random

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val gsrRepo: GsrSampleRepository,
    private val profileRepo: ProfileRepository
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
    val todayGsrBars: StateFlow<List<Float?>> =
        activeUserId
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(List(24) { null })
                } else {
                    val (start, end) = todayRangeMillis()
                    gsrRepo.observeHourlyAvg(userId, start, end)
                        .map(::to24Bars)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(24) { null })

    fun seedMock() {
        seedMockActiveUserAndTodayGsr(profileRepo, gsrRepo)
    }

    init{
        seedMock()
    }

    //Mock functions
    private fun seedMockActiveUserAndTodayGsr(
        profileRepo: ProfileRepository,
        gsrRepo: GsrSampleRepository
    ) = viewModelScope.launch {

        val zone = ZoneId.of("Europe/Warsaw")
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

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

        val hasTodayData = gsrRepo.existsInRange(userId, start, end)
        if (hasTodayData) return@launch

        var t = start
        while (t < end) {
            val gsrValue = 200 + Random.nextInt(600) // 200..799

            gsrRepo.upsert(
                GsrSample(
                    id = 0,      // auto-generate
                    userId = userId,
                    epoch = t,   // millis
                    gsr = gsrValue
                )
            )

            t += 10 * 60 * 1000L
        }
    }
}
