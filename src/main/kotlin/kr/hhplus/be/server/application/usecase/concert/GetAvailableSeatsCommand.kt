package kr.hhplus.be.server.application.usecase.concert

data class GetAvailableSeatsCommand(
    val concertId: Long,
    val scheduleId: Long,
)
