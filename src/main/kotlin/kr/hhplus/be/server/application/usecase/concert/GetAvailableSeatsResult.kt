package kr.hhplus.be.server.application.usecase.concert

import kr.hhplus.be.server.domain.concert.model.SeatStatus

data class GetAvailableSeatsResult(
    val seats: List<SeatInfo>,
) {
    data class SeatInfo(
        val seatId: Long,
        val concertScheduleId: Long,
        val seatNumber: Int,
        val price: Int,
        val status: SeatStatus,
    )
}
