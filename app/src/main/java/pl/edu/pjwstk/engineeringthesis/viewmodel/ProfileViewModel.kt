package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

@HiltViewModel
class ProfileOnboardingViewModel @Inject constructor(
    private val repo: ProfileRepository
) : ViewModel() {

    enum class Step { Gender, BirthDate, Height, Done }

    data class UiState(
        val step: Step = Step.Gender,
        val gender: String? = null,
        val birthDateEpochDays: Long? = null,
        val heightCm: String = "",
        val canNext: Boolean = false,
        val finished: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            if (repo.exists()) {
                _state.update { it.copy(step = Step.Done, finished = true) }
            }
        }
    }

    fun selectGender(g: String) {
        _state.update { it.copy(gender = g, canNext = true) }
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

    fun next() {
        when (_state.value.step) {
            Step.Gender    -> _state.update { it.copy(step = Step.BirthDate, canNext = it.birthDateEpochDays?.let(::validAge) == true) }
            Step.BirthDate -> _state.update { it.copy(step = Step.Height,    canNext = it.heightCm.toIntOrNull()?.let { n -> n in 100..250 } == true) }
            Step.Height    -> saveAndFinish()
            Step.Done      -> Unit
        }
    }

    private fun saveAndFinish() = viewModelScope.launch {
        val s = _state.value
        val profile = UserProfile(
            gender = s.gender ?: "unspecified",
            birthDateEpochDays = s.birthDateEpochDays ?: 0L,
            heightCm = s.heightCm.toIntOrNull() ?: 0
        )
        repo.save(profile)
        _state.update { it.copy(step = Step.Done, finished = true) }
    }

    private fun validAge(epochDays: Long): Boolean {
        val birth = LocalDate.ofEpochDay(epochDays)
        val years = Period.between(birth, LocalDate.now()).years
        return years in 5..120
    }
}