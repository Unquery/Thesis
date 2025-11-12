package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.edu.pjwstk.engineeringthesis.viewmodel.ProfileOnboardingViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOnboardingScreen(
    vm: ProfileOnboardingViewModel = hiltViewModel(),
    onDone: () -> Unit
) {
    val s by vm.state.collectAsStateWithLifecycle()

    if (s.finished) {
        LaunchedEffect(Unit) { onDone() }
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Profile setup") }) }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            when (s.step) {
                ProfileOnboardingViewModel.Step.Gender ->
                    GenderStep(s.gender, vm::selectGender, vm::next, s.canNext)

                ProfileOnboardingViewModel.Step.BirthDate ->
                    BirthDateStep(s.birthDateEpochDays, vm::setBirthDate, vm::next, s.canNext)

                ProfileOnboardingViewModel.Step.Height ->
                    HeightStep(s.heightCm, vm::setHeight, vm::next, s.canNext)

                ProfileOnboardingViewModel.Step.Done -> {}
            }
        }
    }
}

@Composable
private fun GenderStep(selected: String?, onSelect: (String) -> Unit, onNext: () -> Unit, canNext: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("What’s your gender?", style = MaterialTheme.typography.titleLarge)
        val opts = listOf("Male", "Female", "Other", "Prefer not to say")
        opts.forEach { g ->
            FilterChip(
                selected = selected == g,
                onClick = { onSelect(g) },
                label = { Text(g) }
            )
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, enabled = canNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Next")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateStep(
    selectedEpochDays: Long?,
    onChange: (Long?) -> Unit,
    onNext: () -> Unit,
    canNext: Boolean
) {
    var open by remember { mutableStateOf(true) }
    val nowYear = remember { LocalDate.now().year }
    val initMillis = remember(selectedEpochDays) {
        selectedEpochDays?.let {
            LocalDate.ofEpochDay(it)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        }
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initMillis,
            yearRange = 1900..nowYear,
            initialDisplayMode = androidx.compose.material3.DisplayMode.Picker
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        val days = state.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate().toEpochDay()
                        }
                        onChange(days)
                        open = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton({ open = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your date of birth", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { open = true }) { Text("Pick date") }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, enabled = canNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Next")
        }
    }
}


@Composable
private fun HeightStep(value: String, onChange: (String) -> Unit, onNext: () -> Unit, canNext: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your height (cm)", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            suffix = { Text("cm") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, enabled = canNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Save")
        }
    }
}
