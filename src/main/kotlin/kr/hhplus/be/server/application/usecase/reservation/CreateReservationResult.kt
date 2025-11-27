package kr.hhplus.be.server.application.usecase.reservation

import kr.hhplus.be.server.domain.reservation.model.ReservationStatus
import java.time.LocalDateTime

data class CreateReservationResult(
    val reservationId: Long,
    val userId: Long,
    val seatId: Long,
    val status: ReservationStatus,
    val reservedAt: LocalDateTime
)
