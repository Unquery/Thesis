package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import java.time.LocalDate
import java.time.Period
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
    private val repo: ProfileRepository
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
        heartRateNormalLow: Float,
        heartRateNormalHigh: Float,
        skinConductanceNormalLow: Float,
        skinConductanceNormalHigh: Float
    ): Int? {
        if (
            temperatureNormalLow >= temperatureNormalHigh ||
            heartRateNormalLow >= heartRateNormalHigh ||
            skinConductanceNormalLow >= skinConductanceNormalHigh
        ) {
            return R.string.profile_calibration_error_low_less_than_high
        }

        val id = activeProfile.value?.id ?: return R.string.error_no_active_profile
        viewModelScope.launch {
            repo.setMeasurementCalibration(
                id = id,
                temperatureNormalLow = temperatureNormalLow,
                temperatureNormalHigh = temperatureNormalHigh,
                heartRateNormalLow = heartRateNormalLow,
                heartRateNormalHigh = heartRateNormalHigh,
                skinConductanceNormalLow = skinConductanceNormalLow,
                skinConductanceNormalHigh = skinConductanceNormalHigh
            )
        }
        return null
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

