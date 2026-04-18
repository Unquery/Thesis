package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.font.interFamily
import pl.edu.pjwstk.engineeringthesis.viewmodel.ProfileOnboardingViewModel
import pl.edu.pjwstk.engineeringthesis.viewmodel.ProfileViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Locale

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(stringResource(R.string.profile_setup_title)) })
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            when (s.step) {
                ProfileOnboardingViewModel.Step.Name ->
                    NameStep(s.name, vm::setName, vm::next, s.canNext)

                ProfileOnboardingViewModel.Step.Gender ->
                    GenderStep(s.gender, vm::selectGender, vm::next, s.canNext)

                ProfileOnboardingViewModel.Step.BirthDate ->
                    BirthDateStep(s.birthDateEpochDays, vm::setBirthDate, vm::next, s.canNext)

                ProfileOnboardingViewModel.Step.Height ->
                    HeightStep(s.heightCm, vm::setHeight, vm::next, s.canNext)

                ProfileOnboardingViewModel.Step.Weight ->
                    WeightStep(s.weightKg, vm::setWeight, vm::next, s.canNext)

                ProfileOnboardingViewModel.Step.Done -> {}
            }
        }
    }
}

@Composable
private fun NameStep(value: String, onChange: (String) -> Unit, onNext: () -> Unit, canNext: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.profile_question_name), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.take(40)) },
            label = { Text(stringResource(R.string.profile_label_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, enabled = canNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun GenderStep(selected: String?, onSelect: (String) -> Unit, onNext: () -> Unit, canNext: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.profile_question_gender), style = MaterialTheme.typography.titleLarge)
        val opts = stringArrayResource(R.array.gender_options).toList()
        opts.forEach { g ->
            FilterChip(
                selected = selected == g,
                onClick = { onSelect(g) },
                label = { Text(g) }
            )
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, enabled = canNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.action_next))
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
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton({ open = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.profile_question_birth_date), style = MaterialTheme.typography.titleLarge)
        Button(onClick = { open = true }) { Text(stringResource(R.string.profile_pick_date)) }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, enabled = canNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.action_next))
        }
    }
}


@Composable
private fun HeightStep(value: String, onChange: (String) -> Unit, onNext: () -> Unit, canNext: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.profile_question_height, stringResource(R.string.unit_cm)),
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            suffix = { Text(stringResource(R.string.unit_cm)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, enabled = canNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun WeightStep(value: String, onChange: (String) -> Unit, onNext: () -> Unit, canNext: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.profile_question_weight, stringResource(R.string.unit_kg)),
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(filterWeightInput(it)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            suffix = { Text(stringResource(R.string.unit_kg)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, enabled = canNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.action_save))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    vm: ProfileViewModel = hiltViewModel()
) {
    val profile by vm.activeProfile.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<EditField?>(null) }
    var showCalibrationConfirmDialog by remember { mutableStateOf(false) }
    var showAutomaticCalibrationDialog by remember { mutableStateOf(false) }
    var calibrationStep by remember { mutableStateOf<CalibrationStep?>(null) }
    var calibrationError by remember { mutableStateOf<Int?>(null) }
    var automaticCalibrationError by remember { mutableStateOf<Int?>(null) }
    var isAutomaticCalibrationRunning by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var genderInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var birthDateInput by remember { mutableStateOf<Long?>(null) }
    var temperatureLowInput by remember { mutableStateOf("") }
    var temperatureHighInput by remember { mutableStateOf("") }
    var heartRateLowInput by remember { mutableStateOf("") }
    var heartRateHighInput by remember { mutableStateOf("") }
    var skinConductanceLowInput by remember { mutableStateOf("") }
    var skinConductanceHighInput by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<Int?>(null) }

    val resetCalibrationInputs = {
        temperatureLowInput = ""
        temperatureHighInput = ""
        heartRateLowInput = ""
        heartRateHighInput = ""
        skinConductanceLowInput = ""
        skinConductanceHighInput = ""
    }
    val finishCalibrationFlow = {
        showCalibrationConfirmDialog = false
        showAutomaticCalibrationDialog = false
        calibrationStep = null
        calibrationError = null
        automaticCalibrationError = null
        isAutomaticCalibrationRunning = false
    }
    val discardCalibrationFlow = {
        resetCalibrationInputs()
        finishCalibrationFlow()
    }
    val goToNextCalibrationStep: (CalibrationStep) -> Unit = { step ->
        calibrationError = null
        calibrationStep = nextCalibrationStep(step)
    }
    val saveCalibrationAndClose: (
        temperatureNormalLow: Float,
        temperatureNormalHigh: Float,
        heartRateNormalLow: Float,
        heartRateNormalHigh: Float,
        skinConductanceNormalLow: Float,
        skinConductanceNormalHigh: Float
    ) -> Unit = { temperatureNormalLow,
                  temperatureNormalHigh,
                  heartRateNormalLow,
                  heartRateNormalHigh,
                  skinConductanceNormalLow,
                  skinConductanceNormalHigh ->
        val err = vm.updateMeasurementCalibration(
            temperatureNormalLow = temperatureNormalLow,
            temperatureNormalHigh = temperatureNormalHigh,
            heartRateNormalLow = heartRateNormalLow,
            heartRateNormalHigh = heartRateNormalHigh,
            skinConductanceNormalLow = skinConductanceNormalLow,
            skinConductanceNormalHigh = skinConductanceNormalHigh
        )
        if (err == null) {
            finishCalibrationFlow()
        } else {
            calibrationError = err
        }
    }
    val openNameEdit = {
        nameInput = profile?.name.orEmpty()
        editError = null
        editing = EditField.Name
    }
    val openGenderEdit = {
        genderInput = profile?.gender.orEmpty()
        editError = null
        editing = EditField.Gender
    }
    val openBirthDateEdit = {
        birthDateInput = profile?.birthDateEpochDays?.takeIf { it > 0 }
        editError = null
        editing = EditField.BirthDate
    }
    val openHeightEdit = {
        heightInput = profile?.heightCm?.takeIf { it > 0 }?.toString().orEmpty()
        editError = null
        editing = EditField.Height
    }
    val openWeightEdit = {
        weightInput = profile?.weightKg?.takeIf { it > 0f }?.let(::formatWeightInput).orEmpty()
        editError = null
        editing = EditField.Weight
    }
    val openCalibrationEdit = {
        profile?.let { currentProfile ->
            temperatureLowInput = formatCalibrationInput(currentProfile.temperatureNormalLow)
            temperatureHighInput = formatCalibrationInput(currentProfile.temperatureNormalHigh)
            heartRateLowInput = formatCalibrationInput(currentProfile.heartRateNormalLow)
            heartRateHighInput = formatCalibrationInput(currentProfile.heartRateNormalHigh)
            skinConductanceLowInput = formatCalibrationInput(currentProfile.skinConductanceNormalLow)
            skinConductanceHighInput = formatCalibrationInput(currentProfile.skinConductanceNormalHigh)
        }
        calibrationError = null
        automaticCalibrationError = null
        isAutomaticCalibrationRunning = false
        showAutomaticCalibrationDialog = false
        showCalibrationConfirmDialog = true
    }

    Scaffold(
        topBar = { TopProfileScreenBar() }
    ) { pad ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(pad)
            .padding(16.dp)

        if (profile == null) {
            Box(contentModifier, contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.profile_no_active), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val current = profile!!
            Column(
                contentModifier,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileField(
                    label = stringResource(R.string.profile_label_name),
                    value = current.name.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_not_set),
                    onEdit = openNameEdit
                )
                ProfileField(
                    label = stringResource(R.string.profile_label_gender),
                    value = current.gender.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_not_set),
                    onEdit = openGenderEdit
                )
                ProfileField(
                    label = stringResource(R.string.profile_label_birth_date),
                    value = formatBirthDate(current.birthDateEpochDays),
                    onEdit = openBirthDateEdit
                )
                ProfileField(
                    label = stringResource(R.string.profile_label_height),
                    value = formatHeight(current.heightCm),
                    onEdit = openHeightEdit
                )
                ProfileField(
                    label = stringResource(R.string.profile_label_weight),
                    value = formatWeight(current.weightKg),
                    onEdit = openWeightEdit
                )
                Button(
                    onClick = openCalibrationEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PROFILE_DIALOG_ACTION_COLOR,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.profile_calibrate_measurements))
                }
            }
        }
    }

    if (showCalibrationConfirmDialog) {
        CalibrationConfirmDialog(
            onDismiss = { showCalibrationConfirmDialog = false },
            onConfirm = {
                showCalibrationConfirmDialog = false
                calibrationError = null
                automaticCalibrationError = null
                showAutomaticCalibrationDialog = true
            }
        )
    }

    if (showAutomaticCalibrationDialog) {
        AutomaticCalibrationDialog(
            errorText = automaticCalibrationError,
            buttonsEnabled = !isAutomaticCalibrationRunning,
            onDismiss = {
                showAutomaticCalibrationDialog = false
                automaticCalibrationError = null
            },
            onConfirm = {
                automaticCalibrationError = null
                isAutomaticCalibrationRunning = true
                vm.autoCalibrateMeasurementCalibration { err ->
                    isAutomaticCalibrationRunning = false
                    if (err == null) {
                        finishCalibrationFlow()
                    } else {
                        automaticCalibrationError = err
                    }
                }
            },
            onManualCalibration = {
                showAutomaticCalibrationDialog = false
                automaticCalibrationError = null
                calibrationError = null
                calibrationStep = CalibrationStep.Temperature
            }
        )
    }

    when (calibrationStep) {
        CalibrationStep.Temperature -> MeasurementCalibrationDialog(
            title = stringResource(R.string.profile_calibration_temperature_title),
            unit = stringResource(R.string.unit_celsius),
            lowValue = temperatureLowInput,
            highValue = temperatureHighInput,
            keyboardType = KeyboardType.Decimal,
            confirmLabel = stringResource(R.string.action_next),
            errorText = calibrationError,
            onLowValueChange = {
                temperatureLowInput = filterDecimalCalibrationInput(it, maxIntegerDigits = 2)
                calibrationError = null
            },
            onHighValueChange = {
                temperatureHighInput = filterDecimalCalibrationInput(it, maxIntegerDigits = 2)
                calibrationError = null
            },
            onUseAverage = {
                temperatureLowInput = formatCalibrationInput(AVERAGE_TEMPERATURE_LOW)
                temperatureHighInput = formatCalibrationInput(AVERAGE_TEMPERATURE_HIGH)
                goToNextCalibrationStep(CalibrationStep.Temperature)
            },
            onDismiss = discardCalibrationFlow,
            onConfirm = {
                val err = validateCalibrationRange(
                    lowInput = temperatureLowInput,
                    highInput = temperatureHighInput,
                    parse = { text -> text.toFloatOrNull() },
                    minAllowed = 30f,
                    maxAllowed = 45f
                )
                if (err == null) {
                    goToNextCalibrationStep(CalibrationStep.Temperature)
                } else {
                    calibrationError = err
                }
            }
        )

        CalibrationStep.HeartRate -> MeasurementCalibrationDialog(
            title = stringResource(R.string.profile_calibration_heart_rate_title),
            unit = stringResource(R.string.unit_bpm),
            lowValue = heartRateLowInput,
            highValue = heartRateHighInput,
            keyboardType = KeyboardType.Number,
            confirmLabel = stringResource(R.string.action_next),
            errorText = calibrationError,
            onLowValueChange = {
                heartRateLowInput = it.filter(Char::isDigit).take(3)
                calibrationError = null
            },
            onHighValueChange = {
                heartRateHighInput = it.filter(Char::isDigit).take(3)
                calibrationError = null
            },
            onUseAverage = {
                heartRateLowInput = AVERAGE_HEART_RATE_LOW.toInt().toString()
                heartRateHighInput = AVERAGE_HEART_RATE_HIGH.toInt().toString()
                goToNextCalibrationStep(CalibrationStep.HeartRate)
            },
            onDismiss = discardCalibrationFlow,
            onConfirm = {
                val err = validateCalibrationRange(
                    lowInput = heartRateLowInput,
                    highInput = heartRateHighInput,
                    parse = ::parseWholeNumberAsFloat,
                    minAllowed = 20f,
                    maxAllowed = 240f
                )
                if (err == null) {
                    goToNextCalibrationStep(CalibrationStep.HeartRate)
                } else {
                    calibrationError = err
                }
            }
        )

        CalibrationStep.SkinConductance -> MeasurementCalibrationDialog(
            title = stringResource(R.string.profile_calibration_skin_conductance_title),
            unit = stringResource(R.string.unit_us),
            lowValue = skinConductanceLowInput,
            highValue = skinConductanceHighInput,
            keyboardType = KeyboardType.Decimal,
            confirmLabel = stringResource(R.string.action_save),
            errorText = calibrationError,
            onLowValueChange = {
                skinConductanceLowInput = filterDecimalCalibrationInput(it, maxIntegerDigits = 3)
                calibrationError = null
            },
            onHighValueChange = {
                skinConductanceHighInput = filterDecimalCalibrationInput(it, maxIntegerDigits = 3)
                calibrationError = null
            },
            onUseAverage = {
                skinConductanceLowInput = formatCalibrationInput(AVERAGE_SKIN_CONDUCTANCE_LOW)
                skinConductanceHighInput = formatCalibrationInput(AVERAGE_SKIN_CONDUCTANCE_HIGH)
                saveCalibrationAndClose(
                    temperatureLowInput.toFloat(),
                    temperatureHighInput.toFloat(),
                    heartRateLowInput.toFloat(),
                    heartRateHighInput.toFloat(),
                    AVERAGE_SKIN_CONDUCTANCE_LOW,
                    AVERAGE_SKIN_CONDUCTANCE_HIGH
                )
            },
            onDismiss = discardCalibrationFlow,
            onConfirm = {
                val err = validateCalibrationRange(
                    lowInput = skinConductanceLowInput,
                    highInput = skinConductanceHighInput,
                    parse = { text -> text.toFloatOrNull() },
                    minAllowed = 0f,
                    maxAllowed = 100f
                )
                if (err == null) {
                    saveCalibrationAndClose(
                        temperatureLowInput.toFloat(),
                        temperatureHighInput.toFloat(),
                        heartRateLowInput.toFloat(),
                        heartRateHighInput.toFloat(),
                        skinConductanceLowInput.toFloat(),
                        skinConductanceHighInput.toFloat()
                    )
                } else {
                    calibrationError = err
                }
            }
        )

        null -> Unit
    }

    when (editing) {
        EditField.Name -> NameEditDialog(
            value = nameInput,
            onValueChange = {
                nameInput = it
                editError = null
            },
            errorText = editError,
            onDismiss = { editing = null },
            onSave = {
                val err = vm.updateName(nameInput)
                if (err == null) {
                    editing = null
                } else {
                    editError = err
                }
            }
        )

        EditField.Gender -> GenderEditDialog(
            options = stringArrayResource(R.array.gender_options).toList(),
            selected = genderInput,
            onSelect = {
                genderInput = it
                editError = null
            },
            errorText = editError,
            onDismiss = { editing = null },
            onSave = {
                val err = vm.updateGender(genderInput)
                if (err == null) {
                    editing = null
                } else {
                    editError = err
                }
            }
        )

        EditField.BirthDate -> BirthDateEditDialog(
            selectedEpochDays = birthDateInput,
            errorText = editError,
            onDismiss = { editing = null },
            onSave = { epochDays ->
                val err = vm.updateBirthDate(epochDays)
                if (err == null) {
                    editing = null
                } else {
                    editError = err
                }
            }
        )

        EditField.Height -> HeightEditDialog(
            value = heightInput,
            onValueChange = {
                heightInput = it.filter(Char::isDigit).take(3)
                editError = null
            },
            errorText = editError,
            onDismiss = { editing = null },
            onSave = {
                val height = heightInput.toIntOrNull()
                val err = if (height == null) {
                    R.string.profile_error_enter_valid_height
                } else {
                    vm.updateHeight(height)
                }
                if (err == null) {
                    editing = null
                } else {
                    editError = err
                }
            }
        )

        EditField.Weight -> WeightEditDialog(
            value = weightInput,
            onValueChange = {
                weightInput = filterWeightInput(it)
                editError = null
            },
            errorText = editError,
            onDismiss = { editing = null },
            onSave = {
                val weight = weightInput.toFloatOrNull()
                val err = if (weight == null) {
                    R.string.profile_error_enter_valid_weight
                } else {
                    vm.updateWeight(weight)
                }
                if (err == null) {
                    editing = null
                } else {
                    editError = err
                }
            }
        )

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopProfileScreenBar() {
    Box {
        TopAppBar(
            title = {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.profile_title),
                        fontFamily = interFamily,
                        textAlign = TextAlign.Center,
                        fontSize = 40.sp
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            navigationIcon = { /* no impl */ },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0F172A),
                scrolledContainerColor = Color(0xFF0F172A),
                navigationIconContentColor = Color.White,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }
}

private enum class EditField {
    Name,
    Gender,
    BirthDate,
    Height,
    Weight
}

private enum class CalibrationStep {
    Temperature,
    HeartRate,
    SkinConductance
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onEdit: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.profile_edit_content_description, label)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CalibrationConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.profile_calibrate_confirm_title))
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onConfirm,
                    modifier = Modifier.width(124.dp),
                    border = BorderStroke(2.dp, PROFILE_DIALOG_ACTION_COLOR),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(text = stringResource(R.string.action_yes))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.width(124.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PROFILE_DIALOG_ACTION_COLOR,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = stringResource(R.string.action_no))
                }
            }
        }
    )
}

@Composable
private fun AutomaticCalibrationDialog(
    errorText: Int?,
    buttonsEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onManualCalibration: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.profile_auto_calibration_confirm_title))
        },
        text = {
            if (errorText != null) {
                Text(
                    text = stringResource(errorText),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onConfirm,
                    modifier = Modifier.width(124.dp),
                    enabled = buttonsEnabled,
                    border = BorderStroke(2.dp, PROFILE_DIALOG_ACTION_COLOR),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(text = stringResource(R.string.action_yes))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Button(
                    onClick = onManualCalibration,
                    modifier = Modifier.width(124.dp),
                    enabled = buttonsEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PROFILE_DIALOG_ACTION_COLOR,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = stringResource(R.string.action_no))
                }
            }
        }
    )
}

@Composable
private fun MeasurementCalibrationDialog(
    title: String,
    unit: String,
    lowValue: String,
    highValue: String,
    keyboardType: KeyboardType,
    confirmLabel: String,
    errorText: Int?,
    onLowValueChange: (String) -> Unit,
    onHighValueChange: (String) -> Unit,
    onUseAverage: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor
                )
                OutlinedTextField(
                    value = lowValue,
                    onValueChange = onLowValueChange,
                    label = { Text(stringResource(R.string.profile_calibration_low_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                    suffix = { Text(unit) },
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = highValue,
                    onValueChange = onHighValueChange,
                    label = { Text(stringResource(R.string.profile_calibration_high_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                    suffix = { Text(unit) },
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onUseAverage,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, PROFILE_DIALOG_ACTION_COLOR),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PROFILE_DIALOG_ACTION_COLOR,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.profile_calibration_set_average_value))
                }
                if (errorText != null) {
                    Text(stringResource(errorText), color = MaterialTheme.colorScheme.error)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.width(124.dp),
                        border = BorderStroke(2.dp, PROFILE_DIALOG_ACTION_COLOR),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.width(124.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PROFILE_DIALOG_ACTION_COLOR,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun NameEditDialog(
    value: String,
    onValueChange: (String) -> Unit,
    errorText: Int?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(stringResource(R.string.profile_label_name)) },
                    singleLine = true,
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(stringResource(errorText), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = value.trim().isNotEmpty()
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun GenderEditDialog(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    errorText: Int?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_gender_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        label = { Text(option) }
                    )
                }
                if (errorText != null) {
                    Text(stringResource(errorText), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = selected.isNotBlank()
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun HeightEditDialog(
    value: String,
    onValueChange: (String) -> Unit,
    errorText: Int?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_height_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text(stringResource(R.string.unit_cm)) },
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(stringResource(errorText), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = value.toIntOrNull() != null
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun WeightEditDialog(
    value: String,
    onValueChange: (String) -> Unit,
    errorText: Int?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_weight_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text(stringResource(R.string.unit_kg)) },
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(stringResource(errorText), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = value.toIntOrNull() != null
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

private fun filterWeightInput(input: String): String {
    val filtered = buildString {
        var hasDot = false
        var decimals = 0
        input.forEach { ch ->
            when {
                ch.isDigit() && (!hasDot || decimals < 1) -> {
                    append(ch)
                    if (hasDot) decimals++
                }

                ch == '.' && !hasDot -> {
                    if (isEmpty()) append('0')
                    append(ch)
                    hasDot = true
                }
            }
        }
    }
    return if (filtered.length > 4 && '.' !in filtered) filtered.take(3) else filtered
}

private fun formatWeightInput(weightKg: Float): String {
    val rounded = String.format(Locale.US, "%.1f", weightKg)
    return if (rounded.endsWith(".0")) {
        rounded.dropLast(2)
    } else {
        rounded
    }
}

private fun nextCalibrationStep(step: CalibrationStep): CalibrationStep? =
    when (step) {
        CalibrationStep.Temperature -> CalibrationStep.HeartRate
        CalibrationStep.HeartRate -> CalibrationStep.SkinConductance
        CalibrationStep.SkinConductance -> null
    }

private fun parseWholeNumberAsFloat(input: String): Float? =
    input.trim().toIntOrNull()?.toFloat()

private fun filterDecimalCalibrationInput(
    input: String,
    maxIntegerDigits: Int,
    maxDecimalDigits: Int = 1
): String {
    val filtered = buildString {
        var hasDot = false
        var integerDigits = 0
        var decimalDigits = 0
        input.forEach { ch ->
            when {
                ch.isDigit() && !hasDot && integerDigits < maxIntegerDigits -> {
                    append(ch)
                    integerDigits++
                }

                ch.isDigit() && hasDot && decimalDigits < maxDecimalDigits -> {
                    append(ch)
                    decimalDigits++
                }

                ch == '.' && !hasDot -> {
                    if (isEmpty()) {
                        append('0')
                        integerDigits = 1
                    }
                    append(ch)
                    hasDot = true
                }
            }
        }
    }
    return filtered
}

private fun formatCalibrationInput(value: Float): String {
    val rounded = String.format(Locale.US, "%.1f", value)
    return if (rounded.endsWith(".0")) {
        rounded.dropLast(2)
    } else {
        rounded
    }
}

private fun validateCalibrationRange(
    lowInput: String,
    highInput: String,
    parse: (String) -> Float?,
    minAllowed: Float,
    maxAllowed: Float
): Int? {
    val lowValue = parse(lowInput.trim())
    if (lowValue == null || lowValue !in minAllowed..maxAllowed) {
        return R.string.profile_calibration_error_invalid_low
    }

    val highValue = parse(highInput.trim())
    if (highValue == null || highValue !in minAllowed..maxAllowed) {
        return R.string.profile_calibration_error_invalid_high
    }

    return if (lowValue < highValue) {
        null
    } else {
        R.string.profile_calibration_error_low_less_than_high
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateEditDialog(
    selectedEpochDays: Long?,
    errorText: Int?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val nowYear = remember { LocalDate.now().year }
    val initMillis = remember(selectedEpochDays) {
        selectedEpochDays?.let {
            LocalDate.ofEpochDay(it)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initMillis,
        yearRange = 1900..nowYear,
        initialDisplayMode = androidx.compose.material3.DisplayMode.Picker
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    val days = state.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .toEpochDay()
                    }
                    if (days != null) {
                        onSave(days)
                    }
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    ) {
        Column {
            DatePicker(state = state, showModeToggle = false)
            if (errorText != null) {
                Text(
                    text = stringResource(errorText),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun formatHeight(heightCm: Int): String {
    if (heightCm <= 0) return stringResource(R.string.profile_not_set)
    val unit = stringResource(R.string.unit_cm)
    return stringResource(R.string.profile_height_value, heightCm, unit)
}

private val PROFILE_DIALOG_ACTION_COLOR = Color(0xFF0F172A)
private const val AVERAGE_HEART_RATE_LOW = 60f
private const val AVERAGE_HEART_RATE_HIGH = 100f
private const val AVERAGE_TEMPERATURE_LOW = 36.1f
private const val AVERAGE_TEMPERATURE_HIGH = 37.2f
private const val AVERAGE_SKIN_CONDUCTANCE_LOW = 1f
private const val AVERAGE_SKIN_CONDUCTANCE_HIGH = 20f

@Composable
private fun formatWeight(weightKg: Float): String {
    if (weightKg <= 0f) return stringResource(R.string.profile_not_set)
    val unit = stringResource(R.string.unit_kg)
    return stringResource(R.string.profile_weight_value, formatWeightInput(weightKg), unit)
}

@Composable
private fun formatBirthDate(epochDays: Long): String {
    if (epochDays <= 0) return stringResource(R.string.profile_not_set)
    val date = LocalDate.ofEpochDay(epochDays)
    val years = Period.between(date, LocalDate.now()).years
    return stringResource(R.string.profile_birth_date_value, date.toString(), years)
}
