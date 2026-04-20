package pl.edu.pjwstk.engineeringthesis.view

import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.widget.NumberPicker
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.yml.charts.axis.AxisData
import co.yml.charts.axis.Gravity
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.LineType
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import pl.edu.pjwstk.engineeringthesis.util.ChartMetric
import pl.edu.pjwstk.engineeringthesis.util.ChartRange
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_HEART_RATE_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_HEART_RATE_LOW
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SKIN_CONDUCTANCE_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SKIN_CONDUCTANCE_LOW
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SPO2_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_SPO2_LOW
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_TEMPERATURE_HIGH
import pl.edu.pjwstk.engineeringthesis.util.PROFILE_DEFAULT_TEMPERATURE_LOW
import pl.edu.pjwstk.engineeringthesis.viewmodel.ChartBucketRange
import pl.edu.pjwstk.engineeringthesis.viewmodel.ChartLinePoint
import pl.edu.pjwstk.engineeringthesis.viewmodel.ChartSummary
import pl.edu.pjwstk.engineeringthesis.viewmodel.ChartsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    vm: ChartsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val chartState by vm.chartState.collectAsStateWithLifecycle()
    val activeProfile by vm.activeProfile.collectAsStateWithLifecycle()
    val metric = vm.metric
    val ui = remember(metric, activeProfile) { metricUi(metric, activeProfile) }
    val uiTitle = stringResource(ui.titleRes)
    val uiUnit = stringResource(ui.unitRes)

    val range = chartState.range
    val rawBuckets = chartState.points
    val rawLinePoints = chartState.linePoints
    val rangeDates = chartState.dates
    val summary = chartState.summary
    val baseDate = chartState.baseDate
    val selectedHour = chartState.selectedHour

    val buckets = remember(rawBuckets) {
        rawBuckets.sortedBy { it.x }
    }
    val actualLinePoints = remember(rawLinePoints) {
        rawLinePoints.sortedBy { it.x }
    }
    val linePoints = remember(actualLinePoints) {
        withMinutePoints(actualLinePoints)
    }

    val density = LocalDensity.current
    val zone = remember { ZoneId.systemDefault() }

    val last7DaysLabel = stringResource(R.string.charts_last_7_days)
    val rangeLabel = remember(range, rangeDates, baseDate, last7DaysLabel, selectedHour) {
        formatRangeLabel(range, rangeDates, baseDate, last7DaysLabel, selectedHour)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showHourPicker by remember { mutableStateOf(false) }
    val baseDateMillis = remember(baseDate, zone) {
        baseDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = baseDateMillis)
    LaunchedEffect(baseDateMillis) {
        datePickerState.selectedDateMillis = baseDateMillis
    }

    val emptyMessage = run {
        val today = LocalDate.now(zone)
        when (range) {
            ChartRange.Hour -> stringResource(R.string.charts_no_data_hour)
            ChartRange.Day -> if (baseDate == today) {
                stringResource(R.string.charts_no_data_today)
            } else {
                stringResource(R.string.charts_no_data_day)
            }
            ChartRange.Week -> stringResource(R.string.charts_no_data_week)
            ChartRange.Month -> stringResource(R.string.charts_no_data_month)
        }
    }

    val xAxisSteps = remember(range, rangeDates) {
        when (range) {
            ChartRange.Hour -> MINUTES_PER_HOUR
            ChartRange.Day -> X_AXIS_STEPS
            else -> (rangeDates.size - 1).coerceAtLeast(1)
        }
    }

    val hourAxisLabels = remember {
        (0 until MINUTES_PER_HOUR step 5).associateWith { minute ->
            String.format(Locale.US, "%02d", minute)
        }
    }
    val dayAxisLabels = mapOf(
        2 to stringResource(R.string.time_01_00),
        12 to stringResource(R.string.time_06_00),
        24 to stringResource(R.string.time_12_00),
        36 to stringResource(R.string.time_18_00),
        48 to stringResource(R.string.time_24_00)
    )
    val xAxisLabels = when (range) {
        ChartRange.Hour -> hourAxisLabels
        ChartRange.Day -> dayAxisLabels
        ChartRange.Week -> weekLabelMap(rangeDates)
        ChartRange.Month -> monthLabelMap(rangeDates)
    }
    val selectionDayFormat = stringResource(R.string.charts_selection_day_format)
    val selectionRangeFormat = stringResource(R.string.charts_selection_range_format)
    val selectionValueOnlyFormat = stringResource(R.string.charts_selection_value_only_format)
    val time24Label = stringResource(R.string.time_24_00)
    val selectionLabel = remember(
        range,
        rangeDates,
        ui.decimals,
        uiUnit,
        selectionDayFormat,
        selectionRangeFormat,
        selectionValueOnlyFormat,
        time24Label
    ) {
        buildSelectionLabel(
            range = range,
            dates = rangeDates,
            decimals = ui.decimals,
            unit = uiUnit,
            dayFormat = selectionDayFormat,
            rangeFormat = selectionRangeFormat,
            valueOnlyFormat = selectionValueOnlyFormat,
            time24Label = time24Label
        )
    }

    Scaffold(
        containerColor = Color(0xFF05070C),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(uiTitle, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.charts_back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { vm.setSelectedDate(LocalDate.now(zone)) }) {
                        Text(text = stringResource(R.string.charts_today), color = Color.White)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    val normalSummary = formatNormalSummary(ui, uiUnit)
                    if (normalSummary.isNotEmpty()) {
                        Text(
                            text = normalSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }
                }
                Text(
                    text = formatSummaryAvg(summary, ui.decimals, uiUnit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatSummaryMinMax(summary, ui.decimals, uiUnit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                color = CHART_BACKGROUND_COLOR,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    val yValues = remember(range, buckets, linePoints) {
                        when (range) {
                            ChartRange.Hour -> linePoints.map { it.y }
                            ChartRange.Day,
                            ChartRange.Week,
                            ChartRange.Month -> buckets.flatMap { listOf(it.minY, it.maxY) }
                        }
                    }
                    val yAxisSpec = remember(yValues, ui) {
                        buildYAxisSpec(yValues, ui)
                    }
                    val yAxisInset = remember(density, yAxisSpec) {
                        computeYAxisInset(density, yAxisSpec)
                    }

                    val plotWidth = remember(maxWidth, yAxisInset) {
                        (maxWidth - yAxisInset - LINE_CHART_END_PADDING).coerceAtLeast(160.dp)
                    }

                    val axisStepSize = remember(plotWidth, xAxisSteps) { plotWidth / xAxisSteps }

                    val xMax = remember(range, rangeDates) {
                        when (range) {
                            ChartRange.Hour -> MINUTES_PER_HOUR.toFloat()
                            ChartRange.Day -> X_AXIS_STEPS.toFloat()
                            ChartRange.Week,
                            ChartRange.Month -> (rangeDates.size - 1).coerceAtLeast(1).toFloat()
                        }
                    }

                    val chartData = remember(
                        range,
                        buckets,
                        linePoints,
                        ui,
                        axisStepSize,
                        xAxisSteps,
                        xAxisLabels,
                        xMax,
                        yAxisSpec
                    ) {
                        buildLineChartData(
                            range = range,
                            buckets = buckets,
                            actualLinePoints = actualLinePoints,
                            linePoints = linePoints,
                            ui = ui,
                            axisStepSize = axisStepSize,
                            xAxisSteps = xAxisSteps,
                            xAxisLabels = xAxisLabels,
                            xMax = xMax,
                            yAxisSpec = yAxisSpec
                        )
                    }
                    val xAxisHeight = remember(density, chartData, xAxisLabels) {
                        computeXAxisHeight(
                            density = density,
                            axisData = chartData.xAxisData,
                            fallbackLabel = when (range) {
                                ChartRange.Hour,
                                ChartRange.Day -> "00:00"
                                ChartRange.Week -> "Wed"
                                ChartRange.Month -> "30"
                            },
                            labels = xAxisLabels.values
                        )
                    }
                    val chartPlotGeometry = remember(
                        density,
                        yAxisInset,
                        plotWidth,
                        xAxisHeight,
                        xMax,
                        yAxisSpec
                    ) {
                        ChartPlotGeometry(
                            plotStartX = with(density) { yAxisInset.toPx() },
                            plotWidth = with(density) { plotWidth.toPx() },
                            plotTopY = with(density) { chartData.paddingTop.toPx() },
                            plotBottomY = with(density) { CHART_HEIGHT.toPx() - xAxisHeight.toPx() },
                            xMax = xMax.coerceAtLeast(1f),
                            yMin = yAxisSpec.range.min,
                            yMax = yAxisSpec.range.max
                        )
                    }
                    val selectablePoints = remember(range, buckets, actualLinePoints, selectionLabel) {
                        buildSelectableChartPoints(
                            range = range,
                            buckets = buckets,
                            linePoints = actualLinePoints,
                            selectionLabel = selectionLabel
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CHART_HEIGHT)
                    ) {
                        LineChart(
                            modifier = Modifier.matchParentSize(),
                            lineChartData = chartData
                        )
                        ChartSelectionOverlay(
                            modifier = Modifier.matchParentSize(),
                            selectablePoints = selectablePoints,
                            plotGeometry = chartPlotGeometry,
                            color = ui.color
                        )
                        if (buckets.isEmpty() && linePoints.isEmpty()) {
                            Box(
                                modifier = Modifier.matchParentSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emptyMessage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RangeButton(
                    label = stringResource(R.string.charts_range_hour),
                    selected = range == ChartRange.Hour,
                    onClick = { showHourPicker = true },
                    modifier = Modifier.weight(1f)
                )
                RangeButton(
                    label = stringResource(R.string.charts_range_day),
                    selected = range == ChartRange.Day,
                    onClick = { vm.setRange(ChartRange.Day) },
                    modifier = Modifier.weight(1f)
                )
                RangeButton(
                    label = stringResource(R.string.charts_range_week),
                    selected = range == ChartRange.Week,
                    onClick = { vm.setRange(ChartRange.Week) },
                    modifier = Modifier.weight(1f)
                )
                RangeButton(
                    label = stringResource(R.string.charts_range_month),
                    selected = range == ChartRange.Month,
                    onClick = { vm.setRange(ChartRange.Month) },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, RANGE_BORDER_COLOR),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(text = stringResource(R.string.charts_choose_day))
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val picked = Instant.ofEpochMilli(selectedMillis).atZone(zone).toLocalDate()
                        vm.setSelectedDate(picked)
                    }
                    showDatePicker = false
                }) {
                    Text(text = stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showHourPicker) {
        HourPickerDialog(
            initialHour = selectedHour,
            onDismiss = { showHourPicker = false },
            onPick = { pickedHour ->
                vm.setSelectedHour(pickedHour)
                vm.setRange(ChartRange.Hour)
                showHourPicker = false
            }
        )
    }
}

private data class MetricUi(
    @StringRes val titleRes: Int,
    @StringRes val unitRes: Int,
    val decimals: Int,
    val color: Color,
    val axisPaddingBottom: Float = 0f,
    val axisPaddingTop: Float = 0f,
    val axisRoundTo: Float? = null,
    val axisMinClamp: Float? = null,
    val axisMaxClamp: Float? = null,
    val normalCenter: Float? = null,
    val normalMin: Float? = null,
    val normalMax: Float? = null
)

private const val TARGET_Y_AXIS_STEPS = 5f
private const val MAX_Y_AXIS_STEPS = 8
private const val MINUTES_PER_HOUR = 60
private const val X_AXIS_STEPS = 48
private const val HALF_HOUR_STEP = 0.5f
private const val CHART_POINT_EQUALITY_THRESHOLD = 0.0001f
private val Y_AXIS_LABEL_PADDING = 10.dp
private val Y_AXIS_OFFSET = 14.dp
private val Y_AXIS_START_PADDING = 8.dp
private val Y_AXIS_LABEL_FONT_SIZE = 12.sp
private val SMALL_Y_AXIS_LABEL_FONT_SIZE = 8.sp
private const val LARGE_Y_LABEL_THRESHOLD = 1000f
private val BULLET_RADIUS = 2.dp
private val AXIS_LABEL_COLOR = Color.White.copy(alpha = 0.72f)
private val Y_AXIS_LINE_COLOR = Color.White.copy(alpha = 0.2f)
private val CHART_BACKGROUND_COLOR = Color.Black
private val CHART_HEIGHT = 320.dp
private val LINE_CHART_PADDING_RIGHT = 10.dp
private val LINE_CHART_CONTAINER_PADDING_END = 15.dp
private val LINE_CHART_END_PADDING = LINE_CHART_PADDING_RIGHT + LINE_CHART_CONTAINER_PADDING_END
private val CHART_SELECTION_TOUCH_RADIUS = 24.dp
private val CHART_SELECTION_HALO_RADIUS = 9.dp
private val CHART_SELECTION_HALO_STROKE = 2.5.dp
private val CHART_SELECTION_DOT_RADIUS = 5.dp
private val CHART_SELECTION_TOOLTIP_TOP_PADDING = 10.dp
private val CHART_SELECTION_TOOLTIP_HORIZONTAL_PADDING = 12.dp
private val CHART_SELECTION_TOOLTIP_VERTICAL_PADDING = 6.dp
private val CHART_SELECTION_TOOLTIP_BG = Color(0xE6111827)
private val RANGE_SELECTED_BG = Color(0xFF111827)
private val RANGE_BORDER_COLOR = Color.White.copy(alpha = 0.28f)
private val RANGE_BUTTON_CONTENT_PADDING = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
private val DIALOG_ACTION_COLOR = Color(0xFF0F172A)

private data class AxisRange(
    val min: Float,
    val max: Float,
    val step: Float,
    val steps: Int
)

private data class YAxisSpec(
    val range: AxisRange,
    val labelFontSize: TextUnit,
    val labels: List<String>
)

private data class ChartSelectablePoint(
    val id: String,
    val x: Float,
    val y: Float,
    val label: String
)

private data class ChartSelectablePointLayout(
    val point: ChartSelectablePoint,
    val offset: Offset
)

private data class ChartPlotGeometry(
    val plotStartX: Float,
    val plotWidth: Float,
    val plotTopY: Float,
    val plotBottomY: Float,
    val xMax: Float,
    val yMin: Float,
    val yMax: Float
) {
    val plotEndX: Float
        get() = plotStartX + plotWidth

    fun toOffset(point: ChartSelectablePoint): Offset {
        val safeXMax = xMax.coerceAtLeast(1f)
        val xRatio = (point.x / safeXMax).coerceIn(0f, 1f)
        val yRange = (yMax - yMin).coerceAtLeast(CHART_POINT_EQUALITY_THRESHOLD)
        val yRatio = ((point.y - yMin) / yRange).coerceIn(0f, 1f)
        return Offset(
            x = plotStartX + (plotWidth * xRatio),
            y = plotBottomY - ((plotBottomY - plotTopY) * yRatio)
        )
    }
}

@Composable
private fun RangeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            contentPadding = RANGE_BUTTON_CONTENT_PADDING,
            colors = ButtonDefaults.buttonColors(
                containerColor = RANGE_SELECTED_BG,
                contentColor = Color.White
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            border = BorderStroke(1.dp, RANGE_BORDER_COLOR),
            contentPadding = RANGE_BUTTON_CONTENT_PADDING,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HourPickerDialog(
    initialHour: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit
) {
    var selectedHour by remember(initialHour) { mutableStateOf(initialHour) }
    val displayedHours = remember {
        Array(24) { index -> String.format(Locale.US, "%02d", index) }
    }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.charts_pick_hour_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            NumberPicker(context).apply {
                                minValue = 0
                                maxValue = 23
                                wrapSelectorWheel = true
                                value = initialHour.coerceIn(0, 23)
                                displayedValues = displayedHours
                                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                                setOnValueChangedListener { _, _, newValue ->
                                    selectedHour = newValue
                                }
                            }
                        },
                        update = { picker ->
                            if (picker.value != selectedHour) {
                                picker.value = selectedHour
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.width(124.dp),
                        border = BorderStroke(2.dp, DIALOG_ACTION_COLOR),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Button(
                        onClick = { onPick(selectedHour) },
                        modifier = Modifier.width(124.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DIALOG_ACTION_COLOR,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = stringResource(R.string.action_pick))
                    }
                }
            }
        }
    }
}

private fun computeYAxisInset(
    density: Density,
    yAxisSpec: YAxisSpec
): Dp {
    val textPaint = buildAxisTextPaint(
        density = density,
        fontSize = yAxisSpec.labelFontSize
    )
    val maxLabelWidthPx = yAxisSpec.labels.maxOfOrNull { label ->
        label.measureTextWidth(textPaint)
    } ?: 0f
    val labelWidthDp = with(density) { maxLabelWidthPx.toDp() }

    return labelWidthDp + Y_AXIS_LABEL_PADDING + Y_AXIS_OFFSET
}

private fun computeXAxisHeight(
    density: Density,
    axisData: AxisData,
    fallbackLabel: String,
    labels: Collection<String>
): Dp {
    val sampleLabel = labels.firstOrNull { it.isNotBlank() } ?: fallbackLabel
    val textPaint = buildAxisTextPaint(
        density = density,
        fontSize = axisData.axisLabelFontSize,
        typeface = axisData.typeface,
        textAlign = Paint.Align.LEFT
    )
    val labelHeightPx = sampleLabel.measureTextHeight(textPaint)
    return with(density) { labelHeightPx.toDp() } +
        axisData.labelAndAxisLinePadding +
        axisData.axisLineThickness +
        axisData.indicatorLineWidth +
        axisData.axisBottomPadding
}

private fun buildAxisTextPaint(
    density: Density,
    fontSize: TextUnit,
    typeface: android.graphics.Typeface = android.graphics.Typeface.DEFAULT,
    textAlign: Paint.Align = Paint.Align.LEFT
): TextPaint {
    return TextPaint().apply {
        textSize = with(density) { fontSize.toPx() }
        this.typeface = typeface
        this.textAlign = textAlign
        isAntiAlias = true
    }
}

private fun String.measureTextWidth(paint: Paint): Float = paint.measureText(this)

private fun String.measureTextHeight(paint: Paint): Int {
    val bounds = Rect()
    paint.getTextBounds(this, 0, length, bounds)
    return bounds.height()
}

@Composable
private fun ChartSelectionOverlay(
    selectablePoints: List<ChartSelectablePoint>,
    plotGeometry: ChartPlotGeometry,
    color: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { CHART_SELECTION_TOUCH_RADIUS.toPx() }
    val pointLayouts = remember(selectablePoints, plotGeometry) {
        selectablePoints.map { point ->
            ChartSelectablePointLayout(
                point = point,
                offset = plotGeometry.toOffset(point)
            )
        }
    }
    var selectedPointId by remember(selectablePoints) { mutableStateOf<String?>(null) }
    val selectedPointLayout = remember(selectedPointId, pointLayouts) {
        pointLayouts.firstOrNull { it.point.id == selectedPointId }
    }

    Box(
        modifier = modifier.pointerInput(pointLayouts, plotGeometry, touchRadiusPx) {
            detectTapGestures { tapOffset ->
                selectedPointId = findSelectedChartPoint(
                    tapOffset = tapOffset,
                    points = pointLayouts,
                    plotGeometry = plotGeometry,
                    touchRadiusPx = touchRadiusPx
                )?.point?.id
            }
        }
    ) {
        selectedPointLayout?.let { selected ->
            Surface(
                color = CHART_SELECTION_TOOLTIP_BG,
                shape = MaterialTheme.shapes.small,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        start = CHART_SELECTION_TOOLTIP_HORIZONTAL_PADDING,
                        end = CHART_SELECTION_TOOLTIP_HORIZONTAL_PADDING,
                        top = CHART_SELECTION_TOOLTIP_TOP_PADDING
                    )
            ) {
                Text(
                    text = selected.point.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        horizontal = CHART_SELECTION_TOOLTIP_HORIZONTAL_PADDING,
                        vertical = CHART_SELECTION_TOOLTIP_VERTICAL_PADDING
                    )
                )
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = CHART_SELECTION_HALO_RADIUS.toPx() + CHART_SELECTION_HALO_STROKE.toPx(),
                    center = selected.offset
                )
                drawCircle(
                    color = Color.White,
                    radius = CHART_SELECTION_HALO_RADIUS.toPx(),
                    center = selected.offset,
                    style = Stroke(width = CHART_SELECTION_HALO_STROKE.toPx())
                )
                drawCircle(
                    color = color,
                    radius = CHART_SELECTION_DOT_RADIUS.toPx(),
                    center = selected.offset
                )
            }
        }
    }
}

private fun buildSelectableChartPoints(
    range: ChartRange,
    buckets: List<ChartBucketRange>,
    linePoints: List<ChartLinePoint>,
    selectionLabel: (Float, Float) -> String
): List<ChartSelectablePoint> {
    return when (range) {
        ChartRange.Hour -> linePoints.map { point ->
            ChartSelectablePoint(
                id = "hour-${point.x}",
                x = point.x,
                y = point.y,
                label = selectionLabel(point.x, point.y)
            )
        }

        ChartRange.Day,
        ChartRange.Week,
        ChartRange.Month -> buckets.flatMap { bucket ->
            val lowPoint = ChartSelectablePoint(
                id = "bucket-${bucket.x}-low",
                x = bucket.x,
                y = bucket.minY,
                label = selectionLabel(bucket.x, bucket.minY)
            )
            if (abs(bucket.maxY - bucket.minY) < CHART_POINT_EQUALITY_THRESHOLD) {
                listOf(lowPoint.copy(id = "bucket-${bucket.x}-single"))
            } else {
                listOf(
                    lowPoint,
                    ChartSelectablePoint(
                        id = "bucket-${bucket.x}-high",
                        x = bucket.x,
                        y = bucket.maxY,
                        label = selectionLabel(bucket.x, bucket.maxY)
                    )
                )
            }
        }
    }
}

private fun findSelectedChartPoint(
    tapOffset: Offset,
    points: List<ChartSelectablePointLayout>,
    plotGeometry: ChartPlotGeometry,
    touchRadiusPx: Float
): ChartSelectablePointLayout? {
    if (points.isEmpty()) return null
    val isNearPlot = tapOffset.x in (plotGeometry.plotStartX - touchRadiusPx)..(plotGeometry.plotEndX + touchRadiusPx) &&
        tapOffset.y in (plotGeometry.plotTopY - touchRadiusPx)..(plotGeometry.plotBottomY + touchRadiusPx)
    if (!isNearPlot) return null

    val maxDistanceSquared = touchRadiusPx * touchRadiusPx
    return points
        .map { point ->
            val dx = tapOffset.x - point.offset.x
            val dy = tapOffset.y - point.offset.y
            point to ((dx * dx) + (dy * dy))
        }
        .minByOrNull { it.second }
        ?.takeIf { it.second <= maxDistanceSquared }
        ?.first
}

private fun metricUi(metric: ChartMetric, profile: UserProfile?): MetricUi =
    when (metric) {
        ChartMetric.Temperature -> {
            val normalMin = profile?.temperatureNormalLow ?: PROFILE_DEFAULT_TEMPERATURE_LOW
            val normalMax = profile?.temperatureNormalHigh ?: PROFILE_DEFAULT_TEMPERATURE_HIGH
            MetricUi(
                titleRes = R.string.metric_body_temperature,
                unitRes = R.string.unit_celsius,
                decimals = 1,
                color = Color(0xFFF59E0B),
                axisPaddingBottom = 0.5f,
                axisPaddingTop = 0.5f,
                axisRoundTo = 0.5f,
                normalCenter = (normalMin + normalMax) / 2f,
                normalMin = normalMin,
                normalMax = normalMax
            )
        }

        ChartMetric.HeartRate -> {
            val normalMin = profile?.heartRateNormalLow ?: PROFILE_DEFAULT_HEART_RATE_LOW
            val normalMax = profile?.heartRateNormalHigh ?: PROFILE_DEFAULT_HEART_RATE_HIGH
            MetricUi(
                titleRes = R.string.metric_heart_rate,
                unitRes = R.string.unit_bpm,
                decimals = 0,
                color = Color(0xFFE53935),
                axisPaddingBottom = 10f,
                axisPaddingTop = 10f,
                axisRoundTo = 5f,
                normalCenter = (normalMin + normalMax) / 2f,
                normalMin = normalMin,
                normalMax = normalMax
            )
        }

        ChartMetric.SpO2 -> {
            val normalMin = profile?.spO2NormalLow ?: PROFILE_DEFAULT_SPO2_LOW
            val normalMax = profile?.spO2NormalHigh ?: PROFILE_DEFAULT_SPO2_HIGH
            MetricUi(
                titleRes = R.string.metric_blood_oxygen,
                unitRes = R.string.unit_percent,
                decimals = 0,
                color = Color(0xFF0284C7),
                axisPaddingBottom = 3f,
                axisRoundTo = 1f,
                axisMaxClamp = 100f,
                normalMin = normalMin,
                normalMax = normalMax
            )
        }

        ChartMetric.Gsr -> {
            val normalMin = profile?.skinConductanceNormalLow ?: PROFILE_DEFAULT_SKIN_CONDUCTANCE_LOW
            val normalMax = profile?.skinConductanceNormalHigh ?: PROFILE_DEFAULT_SKIN_CONDUCTANCE_HIGH
            MetricUi(
                titleRes = R.string.metric_skin_conductance,
                unitRes = R.string.unit_us,
                decimals = 1,
                color = Color(0xFF6366F1),
                axisPaddingBottom = 2f,
                axisPaddingTop = 2f,
                axisRoundTo = 1f,
                axisMinClamp = 0f,
                normalMin = normalMin,
                normalMax = normalMax
            )
        }
    }

private fun formatRangeLabel(
    range: ChartRange,
    dates: List<LocalDate>,
    baseDate: LocalDate,
    last7DaysLabel: String,
    selectedHour: Int
): String {
    val locale = Locale.ENGLISH
    return when (range) {
        ChartRange.Hour -> {
            val dateLabel = baseDate.format(DateTimeFormatter.ofPattern("d MMMM", locale))
            val hourLabel = String.format(Locale.US, "%02d:00-%02d:59", selectedHour, selectedHour)
            "$dateLabel, $hourLabel"
        }

        ChartRange.Day -> {
            baseDate.format(DateTimeFormatter.ofPattern("d MMMM", locale))
        }

        ChartRange.Week -> {
            if (dates.isEmpty()) {
                last7DaysLabel
            } else {
                val fmt = DateTimeFormatter.ofPattern("d MMM", locale)
                "${dates.first().format(fmt)} - ${dates.last().format(fmt)}"
            }
        }

        ChartRange.Month -> {
            if (dates.isEmpty()) {
                baseDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            } else {
                dates.first().format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            }
        }
    }
}

private fun buildSelectionLabel(
    range: ChartRange,
    dates: List<LocalDate>,
    decimals: Int,
    unit: String,
    dayFormat: String,
    rangeFormat: String,
    valueOnlyFormat: String,
    time24Label: String
): (Float, Float) -> String {
    return when (range) {
        ChartRange.Hour -> { x, y ->
            val time = formatHourMinute(x / MINUTES_PER_HOUR.toFloat(), time24Label)
            String.format(Locale.getDefault(), dayFormat, time, formatValue(y, decimals), unit)
        }

        ChartRange.Day -> { x, y ->
            val time = formatHourMinute(x / 2f, time24Label)
            String.format(Locale.getDefault(), dayFormat, time, formatValue(y, decimals), unit)
        }

        ChartRange.Week,
        ChartRange.Month -> { x, y ->
            val maxIndex = (dates.size - 1).coerceAtLeast(0)
            val index = x.roundToInt().coerceIn(0, maxIndex)
            val dateLabel = dates.getOrNull(index)?.format(
                DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
            )
            val value = formatValue(y, decimals)
            if (dateLabel.isNullOrEmpty()) {
                String.format(Locale.getDefault(), valueOnlyFormat, value, unit)
            } else {
                String.format(Locale.getDefault(), rangeFormat, dateLabel, value, unit)
            }
        }
    }
}

private fun weekLabelMap(dates: List<LocalDate>): Map<Int, String> {
    val fmt = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
    return dates
        .mapIndexedNotNull { index, date ->
            if (index == 0) null else index to date.format(fmt)
        }
        .toMap()
}

private fun monthLabelMap(dates: List<LocalDate>): Map<Int, String> {
    if (dates.isEmpty()) return emptyMap()
    val lastIndex = dates.lastIndex
    val labelIndices = mutableSetOf(lastIndex)
    dates.forEachIndexed { index, date ->
        if (date.dayOfMonth == 2 || (date.dayOfMonth != 1 && date.dayOfMonth % 7 == 1)) {
            labelIndices.add(index)
        }
    }
    if (dates[lastIndex].dayOfMonth == 30) {
        val day29Index = dates.indexOfFirst { it.dayOfMonth == 29 }
        if (day29Index >= 0) {
            labelIndices.remove(day29Index)
        }
    }
    return labelIndices.associateWith { index -> dates[index].dayOfMonth.toString() }
}

@Composable
private fun formatSummaryAvg(summary: ChartSummary, decimals: Int, unit: String): String {
    val value = summary.avg?.let { formatValue(it, decimals) }
        ?: stringResource(R.string.value_placeholder)
    return stringResource(R.string.charts_avg_format, value, unit)
}

@Composable
private fun formatSummaryMinMax(summary: ChartSummary, decimals: Int, unit: String): String {
    val min = summary.min?.let { formatValue(it, decimals) }
        ?: stringResource(R.string.value_placeholder)
    val max = summary.max?.let { formatValue(it, decimals) }
        ?: stringResource(R.string.value_placeholder)
    return stringResource(R.string.charts_min_max_format, min, max)
}

@Composable
private fun formatNormalSummary(ui: MetricUi, unit: String): String {
    val min = ui.normalMin ?: return ""
    val max = ui.normalMax ?: return ""
    val minText = formatValue(min, ui.decimals)
    val maxText = formatValue(max, ui.decimals)
    val center = ui.normalCenter
    return if (center != null) {
        stringResource(
            R.string.charts_normal_center_range_format,
            formatValue(center, ui.decimals),
            minText,
            maxText,
            unit
        )
    } else {
        stringResource(R.string.charts_normal_range_format, minText, maxText, unit)
    }
}

private fun buildLineChartData(
    range: ChartRange,
    buckets: List<ChartBucketRange>,
    actualLinePoints: List<ChartLinePoint>,
    linePoints: List<ChartLinePoint>,
    ui: MetricUi,
    axisStepSize: Dp,
    xAxisSteps: Int,
    xAxisLabels: Map<Int, String>,
    xMax: Float,
    yAxisSpec: YAxisSpec
): LineChartData {
    val axisLabelColor = AXIS_LABEL_COLOR
    val xAxisLineColor = Color.Transparent
    val yAxisLineColor = Y_AXIS_LINE_COLOR
    val xAxisLabelFontSize = if (range == ChartRange.Hour) 8.sp else 10.sp

    val axisRange = yAxisSpec.range
    val yAxisLabelPadding = Y_AXIS_LABEL_PADDING

    val xAxisData = AxisData.Builder()
        .steps(xAxisSteps)
        .axisStepSize(axisStepSize)
        .labelAndAxisLinePadding(8.dp)
        .axisLabelFontSize(xAxisLabelFontSize)
        .axisLabelColor(axisLabelColor)
        .axisLineColor(xAxisLineColor)
        .axisLineThickness(0.dp)
        .indicatorLineWidth(0.dp)
        //.endPadding(4.dp)
        .axisPosition(Gravity.BOTTOM)
        .labelData { index -> xAxisLabels[index] ?: "" }
        .build()

    val yAxisData = AxisData.Builder()
        .steps(yAxisSpec.range.steps)
        .axisLabelFontSize(yAxisSpec.labelFontSize)
        .labelAndAxisLinePadding(yAxisLabelPadding)
        .axisLabelColor(axisLabelColor)
        .axisLineColor(yAxisLineColor)
        .axisLineThickness(1.dp)
        .axisOffset(Y_AXIS_OFFSET)
        .startPadding(Y_AXIS_START_PADDING)
        .axisPosition(Gravity.LEFT)
        .labelData { index -> yAxisSpec.labels.getOrElse(index) { "" } }
        .build()

    val boundsLine = Line(
        dataPoints = listOf(
            Point(x = 0f, y = axisRange.min, description = ""),
            Point(x = xMax, y = axisRange.max, description = "")
        ),
        lineStyle = LineStyle(
            lineType = LineType.Straight(),
            color = Color.Transparent,
            width = 0f,
            alpha = 0f
        )
    )

    val plotLines = when (range) {
        ChartRange.Hour -> {
            val continuousLine = buildContinuousLine(linePoints, ui)
            val markerLine = buildMeasurementMarkerLine(actualLinePoints, ui)
            if (continuousLine == null && markerLine == null) {
                listOf(boundsLine)
            } else {
                listOfNotNull(boundsLine, continuousLine, markerLine)
            }
        }

        ChartRange.Day,
        ChartRange.Week,
        ChartRange.Month -> {
            listOf(boundsLine) + buckets.map { bucket ->
                buildBucketRangeLine(bucket, ui)
            }
        }
    }

    return LineChartData(
        linePlotData = LinePlotData(
            lines = plotLines
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        isZoomAllowed = false,
        paddingRight = LINE_CHART_PADDING_RIGHT,
        containerPaddingEnd = LINE_CHART_CONTAINER_PADDING_END,
        gridLines = GridLines(
            color = Color.White.copy(alpha = 0.02f),
            lineWidth = 1.dp,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
            enableHorizontalLines = true,
            enableVerticalLines = false
        ),
        backgroundColor = Color.Transparent
    )
}

private fun buildBucketRangeLine(bucket: ChartBucketRange, ui: MetricUi): Line {
    val lowPoint = Point(x = bucket.x, y = bucket.minY, description = "")
    val highPoint = Point(x = bucket.x, y = bucket.maxY, description = "")
    val dataPoints = if (abs(bucket.maxY - bucket.minY) < CHART_POINT_EQUALITY_THRESHOLD) {
        listOf(lowPoint)
    } else {
        listOf(lowPoint, highPoint)
    }
    return Line(
        dataPoints = dataPoints,
        lineStyle = LineStyle(
            lineType = LineType.Straight(),
            color = ui.color,
            width = 2.8f
        ),
        intersectionPoint = IntersectionPoint(color = ui.color, radius = BULLET_RADIUS)
    )
}

private fun buildContinuousLine(points: List<ChartLinePoint>, ui: MetricUi): Line? {
    if (points.isEmpty()) return null
    return Line(
        dataPoints = points.map { point ->
            Point(
                x = point.x,
                y = point.y,
                description = ""
            )
        },
        lineStyle = LineStyle(
            lineType = LineType.Straight(),
            color = ui.color,
            width = 2.8f
        )
    )
}

private fun buildMeasurementMarkerLine(points: List<ChartLinePoint>, ui: MetricUi): Line? {
    if (points.isEmpty()) return null
    return Line(
        dataPoints = points.map { point ->
            Point(
                x = point.x,
                y = point.y,
                description = ""
            )
        },
        lineStyle = LineStyle(
            lineType = LineType.Straight(),
            color = Color.Transparent,
            width = 0f,
            alpha = 0f
        ),
        intersectionPoint = IntersectionPoint(color = ui.color, radius = BULLET_RADIUS)
    )
}

private fun buildYAxisSpec(values: List<Float>, ui: MetricUi): YAxisSpec {
    val range = if (values.isEmpty()) {
        val fallbackMin = ui.normalMin ?: ui.axisMinClamp ?: 0f
        val fallbackMax = ui.normalMax ?: ui.axisMaxClamp ?: (fallbackMin + 1f)
        dynamicAxisRange(fallbackMin, fallbackMax, ui)
    } else {
        dynamicAxisRange(
            minY = values.minOrNull() ?: 0f,
            maxY = values.maxOrNull() ?: 1f,
            ui = ui
        )
    }
    val labelFontSize =
        if (max(abs(range.min), abs(range.max)) >= LARGE_Y_LABEL_THRESHOLD) {
            SMALL_Y_AXIS_LABEL_FONT_SIZE
        } else {
            Y_AXIS_LABEL_FONT_SIZE
        }
    val labels = (0..range.steps).map { index ->
        formatValue(range.min + (range.step * index), ui.decimals)
    }
    return YAxisSpec(
        range = range,
        labelFontSize = labelFontSize,
        labels = labels
    )
}

private fun withMinutePoints(points: List<ChartLinePoint>): List<ChartLinePoint> {
    if (points.isEmpty()) return points
    val sorted = points.sortedBy { it.x }
    if (sorted.size == 1) return sorted

    val byExactMinute = sorted.associateBy { it.x.roundToInt() }
    val result = ArrayList<ChartLinePoint>(MINUTES_PER_HOUR + 1)
    var prev = sorted.first()
    var nextIndex = 1
    var next = sorted.getOrNull(nextIndex)
    val startMinute = sorted.first().x.roundToInt().coerceIn(0, MINUTES_PER_HOUR)
    val endMinute = sorted.last().x.roundToInt().coerceIn(0, MINUTES_PER_HOUR)

    for (minute in startMinute..endMinute) {
        while (next != null && minute.toFloat() > next.x) {
            prev = next
            nextIndex++
            next = sorted.getOrNull(nextIndex)
        }

        val exact = byExactMinute[minute]
        val y = when {
            exact != null -> exact.y
            next == null -> prev.y
            minute.toFloat() <= prev.x -> prev.y
            else -> {
                val delta = next.x - prev.x
                if (delta <= 0f) {
                    prev.y
                } else {
                    val t = (minute.toFloat() - prev.x) / delta
                    prev.y + t * (next.y - prev.y)
                }
            }
        }

        result.add(
            ChartLinePoint(
                x = minute.toFloat(),
                y = y
            )
        )
    }

    return result
}

private fun normalizeToHours0to24(points: List<Point>): List<Point> {
    if (points.isEmpty()) return points

    val xMin = points.minOf { it.x }
    val xMax = points.maxOf { it.x }

    val toHours: (Float) -> Float = when {
        xMin >= -0.5f && xMax <= 24.5f -> { x -> x }
        xMin >= -1f && xMax <= 24f * 60f + 5f -> { x -> x / 60f }
        xMin >= -1f && xMax <= 24f * 3600f + 10f -> { x -> x / 3600f }
        else -> { x -> x }
    }

    return points.map {
        val hx = toHours(it.x).coerceIn(0f, 24f)
        Point(x = hx, y = it.y, description = it.description)
    }
}

private fun withHalfHourPoints(points: List<Point>): List<Point> {
    if (points.isEmpty()) return points
    val sorted = points.sortedBy { it.x }
    if (sorted.size == 1) return sorted
    val byExactX = sorted.associateBy { it.x }
    val result = ArrayList<Point>(X_AXIS_STEPS + 1)
    var prev = sorted.first()
    var nextIndex = 1
    var next = sorted.getOrNull(nextIndex)
    val startX = sorted.first().x
    val endX = sorted.last().x
    var x = startX

    // Smooth only within the observed range. Extrapolating to 24:00 makes
    // the chart look like future hours have data when they do not.
    while (x <= endX + 0.0001f) {
        while (next != null && x > next.x) {
            prev = next
            nextIndex++
            next = sorted.getOrNull(nextIndex)
        }

        val existing = byExactX[x]
        val y = when {
            existing != null -> existing.y
            next == null -> prev.y
            x <= prev.x -> prev.y
            else -> {
                val delta = next.x - prev.x
                if (delta <= 0f) {
                    prev.y
                } else {
                    val t = (x - prev.x) / delta
                    prev.y + t * (next.y - prev.y)
                }
            }
        }

        result.add(
            Point(
                x = x,
                y = y,
                description = existing?.description ?: ""
            )
        )
        x += HALF_HOUR_STEP
    }

    if (result.lastOrNull()?.x != endX) {
        val lastPoint = sorted.last()
        result.add(
            Point(
                x = lastPoint.x,
                y = lastPoint.y,
                description = lastPoint.description
            )
        )
    }

    return result
}

private fun toHalfHourIndex(points: List<Point>): List<Point> {
    if (points.isEmpty()) return points
    return points.map { point ->
        Point(
            x = (point.x * 2f).coerceIn(0f, X_AXIS_STEPS.toFloat()),
            y = point.y,
            description = point.description
        )
    }
}

private fun formatHourMinute(hourFloat: Float, time24Label: String): String {
    val totalMinutes = (hourFloat * 60f).roundToInt().coerceIn(0, 24 * 60)
    if (totalMinutes == 24 * 60) return time24Label
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
}

private fun formatValue(value: Float, decimals: Int): String {
    val safeDecimals = decimals.coerceIn(0, 2)
    return if (safeDecimals == 0) value.roundToInt().toString()
    else String.format(Locale.US, "%.${safeDecimals}f", value)
}

private fun dynamicAxisRange(minY: Float, maxY: Float, ui: MetricUi): AxisRange {
    val minValue = min(minY, maxY)
    val maxValue = max(minY, maxY)
    val roundTo = ui.axisRoundTo

    var axisMin = minValue - ui.axisPaddingBottom
    var axisMax = maxValue + ui.axisPaddingTop

    if (roundTo != null && roundTo > 0f) {
        axisMin = floor(axisMin / roundTo) * roundTo
        axisMax = ceil(axisMax / roundTo) * roundTo
    }

    ui.axisMinClamp?.let { axisMin = max(it, axisMin) }
    ui.axisMaxClamp?.let { axisMax = min(it, axisMax) }

    if (axisMax <= axisMin) {
        val bump = roundTo ?: if (ui.decimals == 0) 1f else 0.5f
        axisMax = axisMin + bump
    }

    var axisStep = if (roundTo != null && roundTo > 0f) {
        roundToNearestMultiple(
            value = ((axisMax - axisMin) / TARGET_Y_AXIS_STEPS).coerceAtLeast(roundTo),
            multiple = roundTo
        ).coerceAtLeast(roundTo)
    } else {
        niceCeilStep((axisMax - axisMin) / TARGET_Y_AXIS_STEPS)
    }

    var steps = ceil((axisMax - axisMin) / axisStep).toInt().coerceAtLeast(1)
    if (roundTo != null && roundTo > 0f) {
        while (steps > MAX_Y_AXIS_STEPS) {
            axisStep += roundTo
            steps = ceil((axisMax - axisMin) / axisStep).toInt().coerceAtLeast(1)
        }
    }

    var finalMin = axisMin
    var finalMax = axisMin + (axisStep * steps)
    ui.axisMaxClamp?.let { clamp ->
        if (finalMax > clamp) {
            finalMax = clamp
            finalMin = finalMax - (axisStep * steps)
        }
    }
    ui.axisMinClamp?.let { clamp ->
        if (finalMin < clamp) {
            finalMin = clamp
            finalMax = finalMin + (axisStep * steps)
        }
    }

    return AxisRange(
        min = finalMin,
        max = finalMax,
        step = axisStep,
        steps = steps
    )
}

private fun niceAxisRange(minY: Float, maxY: Float, steps: Int): AxisRange {
    val minVal = min(minY, maxY)
    val maxVal = max(minY, maxY)
    val safeSteps = steps.coerceAtLeast(1)

    if (minVal == maxVal) {
        val bump = if (minVal == 0f) 1f else kotlin.math.abs(minVal) * 0.1f
        val niceStep = niceNum((2f * bump) / safeSteps, round = true).coerceAtLeast(0.0001f)
        val axisMin = floor((minVal - bump) / niceStep) * niceStep
        val axisMax = ceil((maxVal + bump) / niceStep) * niceStep
        val axisStep = ((axisMax - axisMin) / safeSteps).coerceAtLeast(0.0001f)
        return AxisRange(axisMin, axisMax, axisStep, safeSteps)
    }

    val rawRange = (maxVal - minVal).coerceAtLeast(0.0001f)
    val niceRange = niceNum(rawRange, round = false)
    val niceStep = niceNum(niceRange / safeSteps, round = true).coerceAtLeast(0.0001f)

    val axisMin = floor(minVal / niceStep) * niceStep
    val axisMax = ceil(maxVal / niceStep) * niceStep
    val axisStep = ((axisMax - axisMin) / safeSteps).coerceAtLeast(0.0001f)

    return AxisRange(axisMin, axisMax, axisStep, safeSteps)
}

private fun exactMinAxisRange(minY: Float, maxY: Float, steps: Int, decimals: Int): AxisRange {
    val safeSteps = steps.coerceAtLeast(1)
    val axisMin = min(minY, maxY)
    val rawRange = (maxY - axisMin).coerceAtLeast(0.0001f)
    val minStep = if (decimals == 0) 1f else 0.0001f
    val axisStep = max(rawRange / safeSteps, minStep)
    val axisMax = axisMin + (axisStep * safeSteps)
    return AxisRange(axisMin, axisMax, axisStep, safeSteps)
}

private fun fixedAxisRange(minY: Float, maxY: Float, steps: Int): AxisRange {
    val safeSteps = steps.coerceAtLeast(1)
    val axisMin = min(minY, maxY)
    val axisMax = max(maxY, axisMin + 0.0001f)
    val axisStep = ((axisMax - axisMin) / safeSteps).coerceAtLeast(0.0001f)
    return AxisRange(axisMin, axisMax, axisStep, safeSteps)
}

private fun floorPreservingAxisRange(minY: Float, maxY: Float, steps: Int, decimals: Int): AxisRange {
    val safeSteps = steps.coerceAtLeast(1)
    val axisMin = minY
    val rawRange = (maxY - axisMin).coerceAtLeast(0.0001f)
    val minStep = if (decimals == 0) 1f else 0.1f
    val axisStep = niceCeilStep(rawRange / safeSteps).coerceAtLeast(minStep)
    val axisMax = axisMin + (axisStep * safeSteps)
    return AxisRange(axisMin, axisMax, axisStep, safeSteps)
}

private fun niceCeilStep(value: Float): Float {
    if (value <= 0f) return 1f
    val exponent = floor(log10(value.toDouble())).toInt()
    val scale = 10f.pow(exponent)
    val fraction = value / scale
    val niceFraction = when {
        fraction <= 1f -> 1f
        fraction <= 2f -> 2f
        fraction <= 2.5f -> 2.5f
        fraction <= 5f -> 5f
        else -> 10f
    }
    return niceFraction * scale
}

private fun niceNum(range: Float, round: Boolean): Float {
    val exponent = floor(log10(range.toDouble())).toInt()
    val fraction = (range / 10f.pow(exponent))
    val niceFraction = if (round) {
        when {
            fraction < 1.5f -> 1f
            fraction < 3f -> 2f
            fraction < 7f -> 5f
            else -> 10f
        }
    } else {
        when {
            fraction <= 1f -> 1f
            fraction <= 2f -> 2f
            fraction <= 5f -> 5f
            else -> 10f
        }
    }
    return niceFraction * 10f.pow(exponent)
}

private fun roundToNearestMultiple(value: Float, multiple: Float): Float {
    if (multiple <= 0f) return value
    val scaled = (value / multiple).roundToInt().coerceAtLeast(1)
    return scaled * multiple
}
