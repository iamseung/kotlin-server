package kr.hhplus.be.server.reservation.repository

import kr.hhplus.be.server.reservation.entity.Reservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReservationRepository : JpaRepository<Reservation, Long> {

    fun findAllByUserId(userId: Long): List<Reservation>
}
