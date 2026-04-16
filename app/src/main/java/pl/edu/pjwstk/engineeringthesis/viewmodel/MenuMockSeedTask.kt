package pl.edu.pjwstk.engineeringthesis.viewmodel

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoSet
import pl.edu.pjwstk.engineeringthesis.data.repository.GsrSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.HearthRateSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.SpO2SampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.TempSampleRepository
import pl.edu.pjwstk.engineeringthesis.model.GsrSample
import pl.edu.pjwstk.engineeringthesis.model.HearthRateSample
import pl.edu.pjwstk.engineeringthesis.model.SpO2Sample
import pl.edu.pjwstk.engineeringthesis.model.TempSample
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import pl.edu.pjwstk.engineeringthesis.util.GSR_MENU_MAX_VALUE
import pl.edu.pjwstk.engineeringthesis.util.GSR_NEUTRAL_MAX
import pl.edu.pjwstk.engineeringthesis.util.GSR_NEUTRAL_MIN
import pl.edu.pjwstk.engineeringthesis.util.GSR_VERY_LOW_THRESHOLD
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random

class MenuMockSeedTask @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val gsrRepo: GsrSampleRepository,
    private val hrRepo: HearthRateSampleRepository,
    private val spo2Repo: SpO2SampleRepository,
    private val tempRepo: TempSampleRepository
) : MenuStartupTask {

    override suspend fun run() {
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

        if (hasGsrToday && hasHrToday && hasSpo2Today && hasTempToday) return

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
        val step = 10 * 60 * 1000L
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
            val isHigh = t >= highStart && t < highEnd
            val isLow = !isHigh && t >= lowStart && t < lowEnd

            if (!hasGsrToday) {
                val gsrValue = when {
                    isHigh -> GSR_NEUTRAL_MAX + rnd.nextFloat() * (GSR_MENU_MAX_VALUE - GSR_NEUTRAL_MAX)
                    isLow -> rnd.nextFloat() * (GSR_NEUTRAL_MIN - GSR_VERY_LOW_THRESHOLD)
                    else -> GSR_NEUTRAL_MIN + rnd.nextFloat() * (GSR_NEUTRAL_MAX - GSR_NEUTRAL_MIN)
                }
                gsrRepo.upsert(GsrSample(id = 0, userId = userId, epoch = t, gsr = gsrValue))
            }

            if (!hasHrToday) {
                val hrValue = when {
                    isHigh -> 125f + rnd.nextInt(35)
                    isLow -> 42f + rnd.nextInt(10)
                    else -> 55f + rnd.nextInt(35)
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
                    isLow -> 35.2f + (rnd.nextInt(6) / 10f)
                    else -> 36.2f + (rnd.nextInt(8) / 10f)
                }
                tempRepo.upsert(TempSample(id = 0, userId = userId, epoch = t, temperature = tempValue))
            }

            t += step
            pointIndex++
        }
    }
}

@Module
@InstallIn(ViewModelComponent::class)
interface MenuMockSeedTaskModule {
    @Binds
    @IntoSet
    fun bindMenuStartupTask(impl: MenuMockSeedTask): MenuStartupTask
}
