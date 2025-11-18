package kr.hhplus.be.server.concert.service

import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.common.exception.NotFoundException
import kr.hhplus.be.server.concert.entity.Seat
import kr.hhplus.be.server.concert.repository.SeatRepository
import org.springframework.stereotype.Service

@Service
class SeatService(
    private val seatRepository: SeatRepository,
) {
    fun findByIdAndConcertScheduleId(seatId: Long, scheduleId: Long): Seat {
        return seatRepository.findByIdAndConcertScheduleId(seatId, scheduleId)
            ?: throw NotFoundException(ErrorCode.ENTITY_NOT_FOUND)
    }
}
