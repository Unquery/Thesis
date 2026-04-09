package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuStartupViewModel @Inject constructor(
    menuStartupTasks: Set<@JvmSuppressWildcards MenuStartupTask>
) : ViewModel() {

    init {
        menuStartupTasks.forEach { task ->
            viewModelScope.launch { task.run() }
        }
    }
}
