package pl.edu.pjwstk.engineeringthesis.view

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.util.ChartMetric
import pl.edu.pjwstk.engineeringthesis.util.ChartRange
import pl.edu.pjwstk.engineeringthesis.viewmodel.ChartBucketRange
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
    val rawBuckets = chartState.points
    val rangeDates = chartState.dates
    val summary = chartState.summary
    val baseDate = chartState.baseDate

    val buckets = remember(rawBuckets) {
        rawBuckets.sortedBy { it.x }
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
                    val yAxisSpec = remember(buckets, ui) {
                        buildYAxisSpec(buckets, ui)
                    }
                    val yAxisInset = remember(density, textMeasurer, yAxisSpec) {
                        computeYAxisInset(density, textMeasurer, yAxisSpec)
                    }

                    val plotWidth = remember(maxWidth, yAxisInset) {
                        (maxWidth - yAxisInset - LINE_CHART_END_PADDING).coerceAtLeast(160.dp)
                    }

                    val axisStepSize = remember(plotWidth, xAxisSteps) { plotWidth / xAxisSteps }

                    val xMax = remember(range, rangeDates) {
                        if (range == ChartRange.Day) {
                            X_AXIS_STEPS.toFloat()
                        } else {
                            (rangeDates.size - 1).coerceAtLeast(1).toFloat()
                        }
                    }

                    val chartData = remember(
                        buckets,
                        ui,
                        axisStepSize,
                        xAxisSteps,
                        xAxisLabels,
                        xMax,
                        yAxisSpec
                    ) {
                        buildLineChartData(
                            buckets = buckets,
                            ui = ui,
                            axisStepSize = axisStepSize,
                            xAxisSteps = xAxisSteps,
                            xAxisLabels = xAxisLabels,
                            xMax = xMax,
                            yAxisSpec = yAxisSpec
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
                        if (rawBuckets.isEmpty()) {
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
    val color: Color,
    val yAxisMinPadding: Float = 0f
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
private val LINE_CHART_PADDING_RIGHT = 10.dp
private val LINE_CHART_CONTAINER_PADDING_END = 15.dp
private val LINE_CHART_END_PADDING = LINE_CHART_PADDING_RIGHT + LINE_CHART_CONTAINER_PADDING_END
private val RANGE_SELECTED_BG = Color(0xFF111827)
private val RANGE_BORDER_COLOR = Color.White.copy(alpha = 0.28f)

private data class AxisRange(
    val min: Float,
    val max: Float,
    val step: Float
)

private data class YAxisSpec(
    val range: AxisRange,
    val labelFontSize: TextUnit,
    val labels: List<String>
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
    textMeasurer: TextMeasurer,
    yAxisSpec: YAxisSpec
): Dp {
    val labelStyle = TextStyle(fontSize = yAxisSpec.labelFontSize)
    val maxLabelWidthPx = yAxisSpec.labels.maxOfOrNull { label ->
        textMeasurer.measure(AnnotatedString(label), style = labelStyle).size.width
    } ?: 0
    val labelWidthDp = with(density) { maxLabelWidthPx.toDp() }

    // Round up slightly to avoid fractional-width slack that would enable a tiny horizontal pan.
    return labelWidthDp + Y_AXIS_LABEL_PADDING + Y_AXIS_OFFSET + 1.dp
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
        color = Color(0xFF0284C7),
        yAxisMinPadding = 2f
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

private fun buildLineChartData(
    buckets: List<ChartBucketRange>,
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

    val range = yAxisSpec.range
    val yAxisLabelPadding = Y_AXIS_LABEL_PADDING

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

    val rangeLines = buckets.map { bucket ->
        buildBucketRangeLine(bucket, ui)
    }

    return LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(boundsLine) + rangeLines
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
    val dataPoints = if (abs(bucket.maxY - bucket.minY) < 0.0001f) {
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

private fun buildYAxisSpec(buckets: List<ChartBucketRange>, ui: MetricUi): YAxisSpec {
    val range = if (buckets.isEmpty()) {
        AxisRange(min = 0f, max = 1f, step = 0.2f)
    } else {
        val yMin = (buckets.minOf { it.minY } - ui.yAxisMinPadding).coerceAtLeast(0f)
        val yMax = buckets.maxOf { it.maxY }
        if (ui.yAxisMinPadding > 0f) {
            exactMinAxisRange(yMin, yMax, Y_AXIS_STEPS, ui.decimals)
        } else {
            niceAxisRange(yMin, yMax, Y_AXIS_STEPS)
        }
    }
    val labelFontSize =
        if (max(abs(range.min), abs(range.max)) >= LARGE_Y_LABEL_THRESHOLD) {
            SMALL_Y_AXIS_LABEL_FONT_SIZE
        } else {
            Y_AXIS_LABEL_FONT_SIZE
        }
    val labels = (0..Y_AXIS_STEPS).map { index ->
        formatValue(range.min + (range.step * index), ui.decimals)
    }
    return YAxisSpec(
        range = range,
        labelFontSize = labelFontSize,
        labels = labels
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
        return AxisRange(axisMin, axisMax, axisStep)
    }

    val rawRange = (maxVal - minVal).coerceAtLeast(0.0001f)
    val niceRange = niceNum(rawRange, round = false)
    val niceStep = niceNum(niceRange / safeSteps, round = true).coerceAtLeast(0.0001f)

    val axisMin = floor(minVal / niceStep) * niceStep
    val axisMax = ceil(maxVal / niceStep) * niceStep
    val axisStep = ((axisMax - axisMin) / safeSteps).coerceAtLeast(0.0001f)

    return AxisRange(axisMin, axisMax, axisStep)
}

private fun exactMinAxisRange(minY: Float, maxY: Float, steps: Int, decimals: Int): AxisRange {
    val safeSteps = steps.coerceAtLeast(1)
    val axisMin = min(minY, maxY)
    val rawRange = (maxY - axisMin).coerceAtLeast(0.0001f)
    val minStep = if (decimals == 0) 1f else 0.0001f
    val axisStep = max(rawRange / safeSteps, minStep)
    val axisMax = axisMin + (axisStep * safeSteps)
    return AxisRange(axisMin, axisMax, axisStep)
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
