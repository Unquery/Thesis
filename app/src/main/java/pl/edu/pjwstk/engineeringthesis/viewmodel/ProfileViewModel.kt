package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.edu.pjwstk.engineeringthesis.R
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
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_HEART_RATE_MAX
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_HEART_RATE_MIN
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_SKIN_CONDUCTANCE_MAX
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_SKIN_CONDUCTANCE_MIN
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_SPO2_MAX
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_SPO2_MIN
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_TEMPERATURE_MAX
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_TEMPERATURE_MIN
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_TEMPERATURE_OFFSET_MAX
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_CALIBRATION_TEMPERATURE_OFFSET_MIN
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_TEMPERATURE_OFFSET_C
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ProfileOnboardingViewModel @Inject constructor(
    private val repo: ProfileRepository
) : ViewModel() {

    enum class Step { Name, Gender, BirthDate, Height, Weight, Done }

    data class UiState(
        val step: Step = Step.Name,
        val name: String = "",
        val gender: String? = null,
        val birthDateEpochDays: Long? = null,
        val heightCm: String = "",
        val weightKg: String = "",
        val canNext: Boolean = false,
        val finished: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            if (repo.existsAny()) {
                _state.update { it.copy(step = Step.Done, finished = true) }
            }
        }
    }

    fun selectGender(g: String) {
        _state.update { it.copy(gender = g, canNext = true) }
    }

    fun setName(text: String) {
        val trimmed = text.trim()
        val ok = trimmed.isNotEmpty() && trimmed.length <= 40
        _state.update { it.copy(name = text, canNext = ok) }
    }

    fun setBirthDate(epochDays: Long?) {
        val ok = epochDays?.let { validAge(it) } ?: false
        _state.update { it.copy(birthDateEpochDays = epochDays, canNext = ok) }
    }

    fun setHeight(text: String) {
        val n = text.toIntOrNull()
        val ok = n != null && n in 100..250
        _state.update { it.copy(heightCm = text, canNext = ok) }
    }

    fun setWeight(text: String) {
        val n = text.toFloatOrNull()
        val ok = n != null && validWeightKg(n)
        _state.update { it.copy(weightKg = text, canNext = ok) }
    }

    fun next() {
        when (_state.value.step) {
            Step.Name      -> _state.update { it.copy(step = Step.Gender,    canNext = it.gender != null) }
            Step.Gender    -> _state.update { it.copy(step = Step.BirthDate, canNext = it.birthDateEpochDays?.let(::validAge) == true) }
            Step.BirthDate -> _state.update { it.copy(step = Step.Height,    canNext = it.heightCm.toIntOrNull()?.let { n -> n in 100..250 } == true) }
            Step.Height    -> _state.update { it.copy(step = Step.Weight,    canNext = it.weightKg.toFloatOrNull()?.let(::validWeightKg) == true) }
            Step.Weight    -> saveAndFinish()
            Step.Done      -> Unit
        }
    }

    fun previous() {
        _state.update {
            when (it.step) {
                Step.Name -> it
                Step.Gender -> it.copy(
                    step = Step.Name,
                    canNext = it.name.trim().isNotEmpty() && it.name.trim().length <= 40
                )
                Step.BirthDate -> it.copy(
                    step = Step.Gender,
                    canNext = it.gender != null
                )
                Step.Height -> it.copy(
                    step = Step.BirthDate,
                    canNext = it.birthDateEpochDays?.let(::validAge) == true
                )
                Step.Weight -> it.copy(
                    step = Step.Height,
                    canNext = it.heightCm.toIntOrNull()?.let { value -> value in 100..250 } == true
                )
                Step.Done -> it
            }
        }
    }

    private fun saveAndFinish() = viewModelScope.launch {
        val s = _state.value

        val profile = UserProfile(
            id = 0,
            name = s.name.trim(),
            gender = s.gender ?: "unspecified",
            birthDateEpochDays = s.birthDateEpochDays ?: 0L,
            heightCm = s.heightCm.toIntOrNull() ?: 0,
            weightKg = s.weightKg.toFloatOrNull() ?: 0f,
            isActive = true
        )
        repo.insertAndActivate(profile)
        _state.update { it.copy(step = Step.Done, finished = true) }
    }

    private fun validAge(epochDays: Long): Boolean {
        val birth = LocalDate.ofEpochDay(epochDays)
        val years = Period.between(birth, LocalDate.now()).years
        return years in 5..120
    }

    private fun validWeightKg(weightKg: Float): Boolean = weightKg in 20f..300f
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val tempRepo: TempSampleRepository,
    private val hrRepo: HearthRateSampleRepository,
    private val gsrRepo: GsrSampleRepository,
    private val spo2Repo: SpO2SampleRepository
) : ViewModel() {

    val activeProfile: StateFlow<UserProfile?> =
        repo.observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @StringRes
    fun updateName(input: String): Int? {
        val name = input.trim()
        if (name.isEmpty()) return R.string.error_name_empty
        if (name.length > 40) return R.string.error_name_too_long
        val id = activeProfile.value?.id ?: return R.string.error_no_active_profile
        viewModelScope.launch { repo.setName(id, name) }
        return null
    }

    @StringRes
    fun updateGender(input: String): Int? {
        val gender = input.trim()
        if (gender.isEmpty()) return R.string.error_gender_empty
        val id = activeProfile.value?.id ?: return R.string.error_no_active_profile
        viewModelScope.launch { repo.setGender(id, gender) }
        return null
    }

    @StringRes
    fun updateBirthDate(epochDays: Long): Int? {
        val error = validateAge(epochDays)
        if (error != null) return error
        val id = activeProfile.value?.id ?: return R.string.error_no_active_profile
        viewModelScope.launch { repo.setBirthDateEpochDays(id, epochDays) }
        return null
    }

    @StringRes
    fun updateHeight(heightCm: Int): Int? {
        if (heightCm !in 100..250) return R.string.error_height_range
        val id = activeProfile.value?.id ?: return R.string.error_no_active_profile
        viewModelScope.launch { repo.setHeightCm(id, heightCm) }
        return null
    }

    @StringRes
    fun updateWeight(weightKg: Float): Int? {
        if (weightKg !in 20f..300f) return R.string.error_weight_range
        val id = activeProfile.value?.id ?: return R.string.error_no_active_profile
        viewModelScope.launch { repo.setWeightKg(id, weightKg) }
        return null
    }

    @StringRes
    fun updateMeasurementCalibration(
        temperatureNormalLow: Float,
        temperatureNormalHigh: Float,
        temperatureOffsetC: Float,
        heartRateNormalLow: Float,
        heartRateNormalHigh: Float,
        spO2NormalLow: Float,
        spO2NormalHigh: Float,
        skinConductanceNormalLow: Float,
        skinConductanceNormalHigh: Float
    ): Int? {
        if (
            temperatureNormalLow >= temperatureNormalHigh ||
            heartRateNormalLow >= heartRateNormalHigh ||
            spO2NormalLow >= spO2NormalHigh ||
            skinConductanceNormalLow >= skinConductanceNormalHigh
        ) {
            return R.string.profile_calibration_error_low_less_than_high
        }
        if (temperatureOffsetC !in PROFILE_CALIBRATION_TEMPERATURE_OFFSET_MIN..PROFILE_CALIBRATION_TEMPERATURE_OFFSET_MAX) {
            return R.string.profile_calibration_error_invalid_temperature_offset
        }
        if (!isPhysiologicallyValidSkinConductance(skinConductanceNormalLow)) {
            return R.string.profile_calibration_error_invalid_low
        }
        if (!isPhysiologicallyValidSkinConductance(skinConductanceNormalHigh)) {
            return R.string.profile_calibration_error_invalid_high
        }

        val id = activeProfile.value?.id ?: return R.string.error_no_active_profile
        viewModelScope.launch {
            repo.setMeasurementCalibration(
                id = id,
                temperatureNormalLow = temperatureNormalLow,
                temperatureNormalHigh = temperatureNormalHigh,
                temperatureOffsetC = temperatureOffsetC,
                heartRateNormalLow = heartRateNormalLow,
                heartRateNormalHigh = heartRateNormalHigh,
                spO2NormalLow = spO2NormalLow,
                spO2NormalHigh = spO2NormalHigh,
                skinConductanceNormalLow = skinConductanceNormalLow,
                skinConductanceNormalHigh = skinConductanceNormalHigh
            )
        }
        return null
    }

    fun autoCalibrateMeasurementCalibration(
        temperatureOffsetC: Float? = null,
        onResult: (Int?) -> Unit
    ) {
        val id = activeProfile.value?.id ?: run {
            onResult(R.string.error_no_active_profile)
            return
        }
        if (
            temperatureOffsetC != null &&
            temperatureOffsetC !in PROFILE_CALIBRATION_TEMPERATURE_OFFSET_MIN..PROFILE_CALIBRATION_TEMPERATURE_OFFSET_MAX
        ) {
            onResult(R.string.profile_calibration_error_invalid_temperature_offset)
            return
        }

        viewModelScope.launch {
            val result = try {
                val currentProfile = activeProfile.value ?: run {
                    onResult(R.string.error_no_active_profile)
                    return@launch
                }
                val now = System.currentTimeMillis()
                val firstEpoch = now - AUTO_CALIBRATION_LOOKBACK_MILLIS
                val calibration = withContext(Dispatchers.Default) {
                    buildAutomaticCalibration(
                        temperatures = tempRepo.getRangeForUser(id, firstEpoch, now),
                        heartRates = hrRepo.getRangeForUser(id, firstEpoch, now),
                        spO2Samples = spo2Repo.getRangeForUser(id, firstEpoch, now),
                        skinConductances = gsrRepo.getRangeForUser(id, firstEpoch, now)
                    )
                } ?: run {
                    onResult(R.string.profile_auto_calibration_error_not_enough_data)
                    return@launch
                }

                repo.setMeasurementCalibration(
                    id = id,
                    temperatureNormalLow = calibration.temperatureNormalLow,
                    temperatureNormalHigh = calibration.temperatureNormalHigh,
                    temperatureOffsetC = temperatureOffsetC ?: currentProfile.temperatureOffsetC
                        .takeIf {
                            it in PROFILE_CALIBRATION_TEMPERATURE_OFFSET_MIN..PROFILE_CALIBRATION_TEMPERATURE_OFFSET_MAX
                        } ?: PROFILE_DEFAULT_TEMPERATURE_OFFSET_C,
                    heartRateNormalLow = calibration.heartRateNormalLow,
                    heartRateNormalHigh = calibration.heartRateNormalHigh,
                    spO2NormalLow = calibration.spO2NormalLow,
                    spO2NormalHigh = calibration.spO2NormalHigh,
                    skinConductanceNormalLow = calibration.skinConductanceNormalLow,
                    skinConductanceNormalHigh = calibration.skinConductanceNormalHigh
                )
                null
            } catch (_: Throwable) {
                R.string.profile_auto_calibration_error_failed
            }

            onResult(result)
        }
    }

    @StringRes
    private fun validateAge(epochDays: Long): Int? {
        val birth = LocalDate.ofEpochDay(epochDays)
        val now = LocalDate.now()
        if (birth.isAfter(now)) return R.string.error_birth_date_future
        val years = Period.between(birth, now).years
        return if (years in 5..120) null else R.string.error_age_range
    }
}

private data class AutomaticCalibrationValues(
    val temperatureNormalLow: Float,
    val temperatureNormalHigh: Float,
    val heartRateNormalLow: Float,
    val heartRateNormalHigh: Float,
    val spO2NormalLow: Float,
    val spO2NormalHigh: Float,
    val skinConductanceNormalLow: Float,
    val skinConductanceNormalHigh: Float
)

private data class AutomaticCalibrationEpoch(
    val epoch: Long,
    val temperature: Float,
    val heartRate: Float,
    val spO2: Float,
    val skinConductance: Float
)

private fun buildAutomaticCalibration(
    temperatures: List<TempSample>,
    heartRates: List<HearthRateSample>,
    spO2Samples: List<SpO2Sample>,
    skinConductances: List<GsrSample>
): AutomaticCalibrationValues? {
    val temperatureByEpoch = temperatures.associateBy(TempSample::epoch)
    val heartRateByEpoch = heartRates.associateBy(HearthRateSample::epoch)
    val spO2ByEpoch = spO2Samples.associateBy(SpO2Sample::epoch)
    val skinConductanceByEpoch = skinConductances.associateBy(GsrSample::epoch)

    val validEpochs = temperatureByEpoch.keys
        .intersect(heartRateByEpoch.keys)
        .intersect(spO2ByEpoch.keys)
        .intersect(skinConductanceByEpoch.keys)
        .mapNotNull { epoch ->
            val temperature = temperatureByEpoch[epoch]?.temperature ?: return@mapNotNull null
            val heartRate = heartRateByEpoch[epoch]?.hearthRate ?: return@mapNotNull null
            val spO2 = spO2ByEpoch[epoch]?.spo2?.toFloat() ?: return@mapNotNull null
            val skinConductance = skinConductanceByEpoch[epoch]?.gsr ?: return@mapNotNull null

            if (!isPhysiologicallyValidTemperature(temperature)) return@mapNotNull null
            if (!isPhysiologicallyValidHeartRate(heartRate)) return@mapNotNull null
            if (!isPhysiologicallyValidSpO2(spO2)) return@mapNotNull null
            if (!isPhysiologicallyValidSkinConductance(skinConductance)) return@mapNotNull null

            AutomaticCalibrationEpoch(
                epoch = epoch,
                temperature = temperature,
                heartRate = heartRate,
                spO2 = spO2,
                skinConductance = skinConductance
            )
        }
        .sortedBy(AutomaticCalibrationEpoch::epoch)

    if (validEpochs.size < MIN_AUTOMATIC_CALIBRATION_EPOCHS) {
        return null
    }

    val validDaysCount = validEpochs
        .map { epoch ->
            Instant.ofEpochMilli(epoch.epoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
        .distinct()
        .size
    if (validDaysCount < MIN_AUTOMATIC_CALIBRATION_DAYS) {
        return null
    }

    val smoothedTemperatures = rollingMedianSmooth(validEpochs.map(AutomaticCalibrationEpoch::temperature))
    val smoothedHeartRates = rollingMedianSmooth(validEpochs.map(AutomaticCalibrationEpoch::heartRate))
    val smoothedSpO2 = rollingMedianSmooth(validEpochs.map(AutomaticCalibrationEpoch::spO2))
    val smoothedSkinConductances = rollingMedianSmooth(validEpochs.map(AutomaticCalibrationEpoch::skinConductance))

    val temperatureRange = percentileRange(smoothedTemperatures)
        ?.coerceCalibrationRange(PROFILE_CALIBRATION_TEMPERATURE_MIN, PROFILE_CALIBRATION_TEMPERATURE_MAX)
        ?: return null
    val heartRateRange = percentileRange(smoothedHeartRates)
        ?.coerceWholeNumberCalibrationRange(PROFILE_CALIBRATION_HEART_RATE_MIN, PROFILE_CALIBRATION_HEART_RATE_MAX)
        ?: return null
    val spO2Low = percentile(smoothedSpO2.sorted(), AUTO_CALIBRATION_LOW_PERCENTILE)
        .coerceCalibrationValue(PROFILE_CALIBRATION_SPO2_MIN, PROFILE_CALIBRATION_SPO2_MAX)
        .floorCalibrationValue(PROFILE_CALIBRATION_SPO2_MIN, PROFILE_CALIBRATION_SPO2_MAX)
    val spO2High = PROFILE_CALIBRATION_SPO2_MAX
    val skinConductanceRange = percentileRange(smoothedSkinConductances)
        ?.coerceCalibrationRange(PROFILE_CALIBRATION_SKIN_CONDUCTANCE_MIN, PROFILE_CALIBRATION_SKIN_CONDUCTANCE_MAX)
        ?: return null

    if (
        temperatureRange.first >= temperatureRange.second ||
        heartRateRange.first >= heartRateRange.second ||
        spO2Low >= spO2High ||
        skinConductanceRange.first >= skinConductanceRange.second
    ) {
        return null
    }

    return AutomaticCalibrationValues(
        temperatureNormalLow = temperatureRange.first,
        temperatureNormalHigh = temperatureRange.second,
        heartRateNormalLow = heartRateRange.first,
        heartRateNormalHigh = heartRateRange.second,
        spO2NormalLow = spO2Low,
        spO2NormalHigh = spO2High,
        skinConductanceNormalLow = skinConductanceRange.first,
        skinConductanceNormalHigh = skinConductanceRange.second
    )
}

private fun Pair<Float, Float>.coerceCalibrationRange(minAllowed: Float, maxAllowed: Float): Pair<Float, Float> =
    first.coerceCalibrationValue(minAllowed, maxAllowed) to
        second.coerceCalibrationValue(minAllowed, maxAllowed)

private fun Pair<Float, Float>.coerceWholeNumberCalibrationRange(
    minAllowed: Float,
    maxAllowed: Float
): Pair<Float, Float> =
    first.floorCalibrationValue(minAllowed, maxAllowed) to
        second.ceilCalibrationValue(minAllowed, maxAllowed)

private fun Float.floorCalibrationValue(minAllowed: Float, maxAllowed: Float): Float =
    kotlin.math.floor(coerceCalibrationValue(minAllowed, maxAllowed).toDouble())
        .toFloat()
        .coerceCalibrationValue(minAllowed, maxAllowed)

private fun Float.ceilCalibrationValue(minAllowed: Float, maxAllowed: Float): Float =
    kotlin.math.ceil(coerceCalibrationValue(minAllowed, maxAllowed).toDouble())
        .toFloat()
        .coerceCalibrationValue(minAllowed, maxAllowed)

private fun Float.coerceCalibrationValue(minAllowed: Float, maxAllowed: Float): Float =
    when {
        this < minAllowed -> minAllowed
        this > maxAllowed -> maxAllowed
        else -> this
    }

private fun rollingMedianSmooth(values: List<Float>): List<Float> {
    if (values.size < 3) return values

    return values.indices.map { index ->
        val start = (index - 1).coerceAtLeast(0)
        val end = (index + 1).coerceAtMost(values.lastIndex)
        median(values.subList(start, end + 1))
    }
}

private fun percentileRange(values: List<Float>): Pair<Float, Float>? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    return percentile(sorted, AUTO_CALIBRATION_LOW_PERCENTILE) to
        percentile(sorted, AUTO_CALIBRATION_HIGH_PERCENTILE)
}

private fun percentile(sortedValues: List<Float>, percentile: Float): Float {
    if (sortedValues.size == 1) return sortedValues.first()

    val rank = (percentile / 100f) * (sortedValues.lastIndex)
    val lowerIndex = rank.toInt()
    val upperIndex = kotlin.math.ceil(rank.toDouble()).toInt()
    if (lowerIndex == upperIndex) return sortedValues[lowerIndex]

    val weight = rank - lowerIndex
    return sortedValues[lowerIndex] + (sortedValues[upperIndex] - sortedValues[lowerIndex]) * weight
}

private fun median(values: List<Float>): Float {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) {
        sorted[middle]
    } else {
        (sorted[middle - 1] + sorted[middle]) / 2f
    }
}

private fun isPhysiologicallyValidTemperature(value: Float): Boolean =
    value in PROFILE_CALIBRATION_TEMPERATURE_MIN..PROFILE_CALIBRATION_TEMPERATURE_MAX

private fun isPhysiologicallyValidHeartRate(value: Float): Boolean =
    value in PROFILE_CALIBRATION_HEART_RATE_MIN..PROFILE_CALIBRATION_HEART_RATE_MAX

private fun isPhysiologicallyValidSpO2(value: Float): Boolean =
    value in PROFILE_CALIBRATION_SPO2_MIN..PROFILE_CALIBRATION_SPO2_MAX

private fun isPhysiologicallyValidSkinConductance(value: Float): Boolean =
    value in PROFILE_CALIBRATION_SKIN_CONDUCTANCE_MIN..PROFILE_CALIBRATION_SKIN_CONDUCTANCE_MAX

private const val MIN_AUTOMATIC_CALIBRATION_EPOCHS = 300
private const val MIN_AUTOMATIC_CALIBRATION_DAYS = 7
private const val AUTO_CALIBRATION_LOW_PERCENTILE = 5f
private const val AUTO_CALIBRATION_HIGH_PERCENTILE = 95f
private val AUTO_CALIBRATION_LOOKBACK_MILLIS = TimeUnit.DAYS.toMillis(14)

