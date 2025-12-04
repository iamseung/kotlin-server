package kr.hhplus.be.server.application.usecase.reservation

data class CreateReservationCommand(
    val userId: Long,
    val scheduleId: Long,
    val seatId: Long,
    val queueToken: String,
)
