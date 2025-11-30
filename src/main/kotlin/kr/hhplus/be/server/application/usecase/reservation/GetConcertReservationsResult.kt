package kr.hhplus.be.server.application.usecase.reservation

import kr.hhplus.be.server.domain.reservation.model.ReservationStatus
import java.time.LocalDateTime

data class GetConcertReservationsResult(
    val reservations: List<ReservationInfo>,
) {
    data class ReservationInfo(
        val reservationId: Long,
        val userId: Long,
        val seatId: Long,
        val status: ReservationStatus,
        val temporaryReservedAt: LocalDateTime,
        val temporaryExpiredAt: LocalDateTime,
    )
}
