package kr.hhplus.be.server.concert.service

import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.common.exception.NotFoundException
import kr.hhplus.be.server.concert.entity.ConcertSchedule
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository
import org.springframework.stereotype.Service

@Service
class ConcertScheduleService(
    private val concertScheduleRepository: ConcertScheduleRepository,
) {

    fun findByConcertId(concertId: Long): List<ConcertSchedule> {
        return concertScheduleRepository.findByConcertId(concertId)
    }

    fun findByConcertIdAndId(concertId: Long, scheduleId: Long): ConcertSchedule {
        return concertScheduleRepository.findByConcertIdAndId(concertId, scheduleId)
            ?: throw NotFoundException(ErrorCode.ENTITY_NOT_FOUND)
    }
}
