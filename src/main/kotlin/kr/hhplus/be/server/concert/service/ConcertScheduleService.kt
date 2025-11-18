package kr.hhplus.be.server.concert.service

import kr.hhplus.be.server.concert.domain.model.ConcertSchedule
import kr.hhplus.be.server.concert.domain.repository.ConcertScheduleRepository
import org.springframework.stereotype.Service

@Service
class ConcertScheduleService(
    private val concertScheduleRepository: ConcertScheduleRepository,
) {

    fun findByConcertId(concertId: Long): List<ConcertSchedule> {
        return concertScheduleRepository.findAllByConcertId(concertId)
    }

    fun findByConcertIdAndId(concertId: Long, scheduleId: Long): ConcertSchedule {
        return concertScheduleRepository.findByIdOrThrow(scheduleId)
    }
}
