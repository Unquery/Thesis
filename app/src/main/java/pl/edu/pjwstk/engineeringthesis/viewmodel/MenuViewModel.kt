package pl.edu.pjwstk.engineeringthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import pl.edu.pjwstk.engineeringthesis.data.db.dao.GsrSampleDao
import pl.edu.pjwstk.engineeringthesis.model.HourlyAvg
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.collections.associate

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val gsrSampleDao: GsrSampleDao,
    // add other DAOs later: tempSampleDao, hrDao, spo2Dao
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    private fun todayRangeMillis(): Pair<Long, Long> {
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    private fun to24Bars(rows: List<HourlyAvg>): List<Float?> {
        val map = rows.associate { it.hour to it.avg }
        return List(24) { hour -> map[hour]?.toFloat() }
    }

    fun observeTodayGsrBars(userId: Int): StateFlow<List<Float?>> {
        val (start, end) = todayRangeMillis()
        return gsrSampleDao.observeHourlyAvg(userId, start, end)
            .map(::to24Bars)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(24) { null })
    }
}
