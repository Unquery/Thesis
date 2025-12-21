package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class ProfileGateViewModel @Inject constructor(
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _showOnboarding = MutableStateFlow<Boolean?>(null)
    val showOnboarding: StateFlow<Boolean?> = _showOnboarding

    init {
        viewModelScope.launch {
            _showOnboarding.value = !profileRepo.existsAny()
        }
    }

    fun markDone() { _showOnboarding.value = false }
}