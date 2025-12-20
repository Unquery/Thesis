package pl.edu.pjwstk.engineeringthesis.model

data class SpO2Sample (
    val id : Int,
    val userId : Int,
    val epoch : Long,
    val spo2 : Int
)