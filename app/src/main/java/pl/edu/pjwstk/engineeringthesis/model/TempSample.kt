package pl.edu.pjwstk.engineeringthesis.model

import android.health.connect.datatypes.units.Temperature

data class TempSample(
    val id : Int,
    val epoch : Long,
    val temperature : Float
)
