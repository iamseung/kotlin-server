package kr.hhplus.be.server.reservation.service

import kr.hhplus.be.server.reservation.domain.model.Reservation
import kr.hhplus.be.server.reservation.domain.repository.ReservationRepository
import org.springframework.stereotype.Service

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
) {

    fun findById(reservationId: Long): Reservation {
        return reservationRepository.findByIdOrThrow(reservationId)
    }

    fun findAllByUserId(userId: Long): List<Reservation> {
        return reservationRepository.findAllByUserId(userId)
    }

    fun save(reservation: Reservation): Reservation {
        return reservationRepository.save(reservation)
    }
}
