package kr.hhplus.be.server.reservation.domain.repository

import kr.hhplus.be.server.reservation.domain.model.Reservation
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus

interface ReservationRepository {
    fun save(reservation: Reservation): Reservation
    fun findById(id: Long): Reservation?
    fun findByIdOrThrow(id: Long): Reservation
    fun findByUserIdAndSeatId(userId: Long, seatId: Long): Reservation?
    fun findAllByUserId(userId: Long): List<Reservation>
    fun findAllByStatus(status: ReservationStatus): List<Reservation>
    fun findExpiredReservations(): List<Reservation>
}
