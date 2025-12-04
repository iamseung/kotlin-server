package kr.hhplus.be.server.domain.concert.service

import kr.hhplus.be.server.domain.concert.model.SeatModel
import kr.hhplus.be.server.domain.concert.model.SeatStatus
import kr.hhplus.be.server.domain.concert.repository.SeatRepository
import org.springframework.stereotype.Service

@Service
class SeatService(
    private val seatRepository: SeatRepository,
) {
    fun findByIdAndConcertScheduleIdWithLock(seatId: Long, scheduleId: Long): SeatModel {
        val seat = seatRepository.findByIdWithLock(seatId)
        seat.validateMatch(scheduleId)

        return seat
    }

    fun findById(seatId: Long): SeatModel {
        return seatRepository.findByIdOrThrow(seatId)
    }

    fun findAllByConcertScheduleId(scheduleId: Long): List<SeatModel> {
        return seatRepository.findAllByConcertScheduleId(scheduleId)
    }

    fun update(seatModel: SeatModel): SeatModel {
        return seatRepository.update(seatModel)
    }

    fun restoreExpiredTemporaryReservations(expirationMinutes: Long = 5): Int {
        val temporarySeats = seatRepository.findAllByStatus(SeatStatus.TEMPORARY_RESERVED)

        val expiredSeats = temporarySeats.filter { it.isExpiredTemporaryReservation(expirationMinutes) }

        expiredSeats.forEach { seat ->
            seat.restoreToAvailable()
            seatRepository.update(seat)
        }

        return expiredSeats.size
    }
}
