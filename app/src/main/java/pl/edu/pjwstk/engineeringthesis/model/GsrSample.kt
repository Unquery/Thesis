package pl.edu.pjwstk.engineeringthesis.model

data class GsrSample(
    val id : Int,
    val userId : Int,
    val epoch : Long,
    val gsr : Float
)
