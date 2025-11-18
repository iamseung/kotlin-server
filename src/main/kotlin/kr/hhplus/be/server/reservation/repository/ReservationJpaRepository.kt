package kr.hhplus.be.server.reservation.repository

import kr.hhplus.be.server.reservation.entity.Reservation
import kr.hhplus.be.server.reservation.entity.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ReservationJpaRepository : JpaRepository<Reservation, Long> {
    fun findAllByUserId(userId: Long): List<Reservation>
    fun findByUserIdAndSeatId(userId: Long, seatId: Long): Reservation?
    fun findAllByReservationStatus(status: ReservationStatus): List<Reservation>

    @Query("SELECT r FROM Reservation r WHERE r.reservationStatus = 'TEMPORARY' AND r.temporaryExpiredAt < :now")
    fun findExpiredReservations(now: LocalDateTime): List<Reservation>
}
