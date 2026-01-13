package pl.edu.pjwstk.engineeringthesis.view

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.util.ChartMetric
import pl.edu.pjwstk.engineeringthesis.util.ChartRange
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
    val metric = vm.metric
    val ui = remember(metric) { metricUi(metric) }
    val uiTitle = stringResource(ui.titleRes)
    val uiUnit = stringResource(ui.unitRes)

    val range = chartState.range
    val rawPoints = chartState.points
    val rangeDates = chartState.dates
    val summary = chartState.summary
    val baseDate = chartState.baseDate

    val points = remember(rawPoints, range) {
        if (range == ChartRange.Day) {
            toHalfHourIndex(withHalfHourPoints(normalizeToHours0to24(rawPoints)))
        } else {
            rawPoints.sortedBy { it.x }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val zone = remember { ZoneId.systemDefault() }

    val last7DaysLabel = stringResource(R.string.charts_last_7_days)
    val rangeLabel = remember(range, rangeDates, baseDate, last7DaysLabel) {
        formatRangeLabel(range, rangeDates, baseDate, last7DaysLabel)
    }

    var showDatePicker by remember { mutableStateOf(false) }
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
            ChartRange.Day -> X_AXIS_STEPS
            else -> (rangeDates.size - 1).coerceAtLeast(1)
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
        ChartRange.Day -> dayAxisLabels
        ChartRange.Week -> weekLabelMap(rangeDates)
        ChartRange.Month -> monthLabelMap(rangeDates)
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
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                )
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
                    val yAxisInset = remember(density, textMeasurer) {
                        computeYAxisInset(density, textMeasurer)
                    }

                    val plotWidth = remember(maxWidth, yAxisInset) {
                        (maxWidth - yAxisInset).coerceAtLeast(160.dp)
                    }

                    val axisStepSize = remember(plotWidth, xAxisSteps) { plotWidth / xAxisSteps }

                    val xMax = remember(range, rangeDates) {
                        if (range == ChartRange.Day) {
                            X_AXIS_STEPS.toFloat()
                        } else {
                            (rangeDates.size - 1).coerceAtLeast(1).toFloat()
                        }
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
                            range,
                            rangeDates,
                            ui.decimals,
                            uiUnit,
                            selectionDayFormat,
                            selectionRangeFormat,
                            selectionValueOnlyFormat,
                            time24Label
                        )
                    }

                    val chartData = remember(
                        points,
                        ui,
                        axisStepSize,
                        xAxisSteps,
                        xAxisLabels,
                        selectionLabel,
                        xMax
                    ) {
                        buildLineChartData(
                            points = points,
                            ui = ui,
                            axisStepSize = axisStepSize,
                            xAxisSteps = xAxisSteps,
                            xAxisLabels = xAxisLabels,
                            selectionLabel = selectionLabel,
                            xMax = xMax
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        LineChart(
                            modifier = Modifier.matchParentSize(),
                            lineChartData = chartData
                        )
                        if (rawPoints.isEmpty()) {
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
}

private data class MetricUi(
    @StringRes val titleRes: Int,
    @StringRes val unitRes: Int,
    val decimals: Int,
    val color: Color
)

private const val Y_AXIS_STEPS = 5
private const val X_AXIS_STEPS = 48
private const val HALF_HOUR_STEP = 0.5f
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
private val RANGE_SELECTED_BG = Color(0xFF111827)
private val RANGE_BORDER_COLOR = Color.White.copy(alpha = 0.28f)

private data class AxisRange(
    val min: Float,
    val max: Float,
    val step: Float
)

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
            colors = ButtonDefaults.buttonColors(
                containerColor = RANGE_SELECTED_BG,
                contentColor = Color.White
            )
        ) {
            Text(text = label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            border = BorderStroke(1.dp, RANGE_BORDER_COLOR),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            )
        ) {
            Text(text = label)
        }
    }
}

private fun computeYAxisInset(
    density: Density,
    textMeasurer: TextMeasurer
): Dp {
    val labelStyle = TextStyle(fontSize = Y_AXIS_LABEL_FONT_SIZE)
    val sampleWidthPx =
        textMeasurer.measure(AnnotatedString("100"), style = labelStyle).size.width
    val labelWidthDp = with(density) { sampleWidthPx.toDp() }

    val safety = 6.dp
    return labelWidthDp + Y_AXIS_LABEL_PADDING + Y_AXIS_OFFSET + Y_AXIS_START_PADDING + safety
}

private fun metricUi(metric: ChartMetric): MetricUi = when (metric) {
    ChartMetric.Temperature -> MetricUi(
        titleRes = R.string.metric_body_temperature,
        unitRes = R.string.unit_celsius,
        decimals = 1,
        color = Color(0xFFF59E0B)
    )

    ChartMetric.HeartRate -> MetricUi(
        titleRes = R.string.metric_heart_rate,
        unitRes = R.string.unit_bpm,
        decimals = 0,
        color = Color(0xFFE53935)
    )

    ChartMetric.SpO2 -> MetricUi(
        titleRes = R.string.metric_blood_oxygen,
        unitRes = R.string.unit_percent,
        decimals = 0,
        color = Color(0xFF0284C7)
    )

    ChartMetric.Gsr -> MetricUi(
        titleRes = R.string.metric_skin_conductance,
        unitRes = R.string.unit_us,
        decimals = 0,
        color = Color(0xFF6366F1)
    )
}

private fun formatRangeLabel(
    range: ChartRange,
    dates: List<LocalDate>,
    baseDate: LocalDate,
    last7DaysLabel: String
): String {
    val locale = Locale.ENGLISH
    return when (range) {
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

private fun buildLineChartData(
    points: List<Point>,
    ui: MetricUi,
    axisStepSize: Dp,
    xAxisSteps: Int,
    xAxisLabels: Map<Int, String>,
    selectionLabel: (Float, Float) -> String,
    xMax: Float
): LineChartData {
    val axisLabelColor = AXIS_LABEL_COLOR
    val xAxisLineColor = Color.Transparent
    val yAxisLineColor = Y_AXIS_LINE_COLOR

    val range = if (points.isEmpty()) {
        AxisRange(min = 0f, max = 1f, step = 0.2f)
    } else {
        val yMin = points.minOf { it.y }
        val yMax = points.maxOf { it.y }
        niceAxisRange(yMin, yMax, Y_AXIS_STEPS)
    }
    val yAxisLabelPadding = Y_AXIS_LABEL_PADDING
    val yAxisLabelFontSize =
        if (max(abs(range.min), abs(range.max)) >= LARGE_Y_LABEL_THRESHOLD) {
            SMALL_Y_AXIS_LABEL_FONT_SIZE
        } else {
            Y_AXIS_LABEL_FONT_SIZE
        }

    val xAxisData = AxisData.Builder()
        .steps(xAxisSteps)
        .axisStepSize(axisStepSize)
        .labelAndAxisLinePadding(8.dp)
        .axisLabelFontSize(10.sp)
        .axisLabelColor(axisLabelColor)
        .axisLineColor(xAxisLineColor)
        .axisLineThickness(0.dp)
        .indicatorLineWidth(0.dp)
        //.endPadding(4.dp)
        .axisPosition(Gravity.BOTTOM)
        .labelData { index -> xAxisLabels[index] ?: "" }
        .build()

    val yAxisData = AxisData.Builder()
        .steps(Y_AXIS_STEPS)
        .axisLabelFontSize(yAxisLabelFontSize)
        .labelAndAxisLinePadding(yAxisLabelPadding)
        .axisLabelColor(axisLabelColor)
        .axisLineColor(yAxisLineColor)
        .axisLineThickness(1.dp)
        .axisOffset(Y_AXIS_OFFSET)
        .startPadding(Y_AXIS_START_PADDING)
        .axisPosition(Gravity.LEFT)
        .labelData { index ->
            val v = range.min + (range.step * index)
            formatValue(v, ui.decimals)
        }
        .build()

    val boundsLine = Line(
        dataPoints = listOf(
            Point(x = 0f, y = range.min, description = ""),
            Point(x = xMax, y = range.max, description = "")
        ),
        lineStyle = LineStyle(
            lineType = LineType.Straight(),
            color = Color.Transparent,
            width = 0f,
            alpha = 0f
        )
    )

    val mainLine = if (points.isEmpty()) {
        null
    } else {
        Line(
            dataPoints = points,
            lineStyle = LineStyle(
                lineType = LineType.SmoothCurve(),
                color = ui.color,
                width = 3.2f
            ),
            intersectionPoint = IntersectionPoint(color = ui.color, radius = BULLET_RADIUS),
            selectionHighlightPoint = SelectionHighlightPoint(color = ui.color, radius = 4.dp),
            selectionHighlightPopUp = SelectionHighlightPopUp(
                backgroundColor = Color(0xFF0F172A),
                labelColor = Color.White,
                popUpLabel = selectionLabel
            ),
            shadowUnderLine = ShadowUnderLine(color = ui.color, alpha = 0.14f)
        )
    }

    return LineChartData(
        linePlotData = LinePlotData(
            lines = if (mainLine == null) listOf(boundsLine) else listOf(boundsLine, mainLine)
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        isZoomAllowed = false,
        gridLines = GridLines(
            color = Color.White.copy(alpha = 0.12f),
            lineWidth = 1.dp,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
            enableHorizontalLines = true,
            enableVerticalLines = false
        ),
        backgroundColor = Color.Transparent
    )
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
    val byExactX = sorted.associateBy { it.x }
    val result = ArrayList<Point>(X_AXIS_STEPS + 1)
    var prev = sorted.first()
    var nextIndex = 1
    var next = sorted.getOrNull(nextIndex)
    var x = 0f
    while (x <= 24f + 0.0001f) {
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


private fun niceAxisRange(minY: Float, maxY: Float, steps: Int): AxisRange {
    val minVal = min(minY, maxY)
    val maxVal = max(minY, maxY)

    if (minVal == maxVal) {
        val bump = if (minVal == 0f) 1f else kotlin.math.abs(minVal) * 0.1f
        val step = niceNum((2f * bump) / steps, round = true).coerceAtLeast(0.0001f)
        val axisMin = floor((minVal - bump) / step) * step
        val axisMax = ceil((maxVal + bump) / step) * step
        return AxisRange(axisMin, axisMax, step)
    }

    val rawRange = (maxVal - minVal).coerceAtLeast(0.0001f)
    val niceRange = niceNum(rawRange, round = false)
    val step = niceNum(niceRange / steps, round = true).coerceAtLeast(0.0001f)

    val axisMin = floor(minVal / step) * step
    val axisMax = ceil(maxVal / step) * step

    return AxisRange(axisMin, axisMax, step)
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
