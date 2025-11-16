package kr.hhplus.be.server.concert.usecase

import kr.hhplus.be.server.concert.dto.response.ConcertScheduleResponse
import kr.hhplus.be.server.concert.dto.response.SeatResponse
import kr.hhplus.be.server.concert.service.ConcertScheduleService
import kr.hhplus.be.server.concert.service.ConcertService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class ConcertScheduleUseCase(
    private val concertService: ConcertService,
    private val concertScheduleService: ConcertScheduleService,
) {

    @Transactional(readOnly = true)
    fun getAvailableSchedules(concertId: Long): List<ConcertScheduleResponse> {
        val concert = concertService.getConcert(concertId)
        val availableSchedules = concertScheduleService.findByConcertId(concert.id)
            .filter { schedule -> schedule.isAvailable }

        return availableSchedules.map { ConcertScheduleResponse.Companion.from(it) }
    }

    @Transactional(readOnly = true)
    fun getAvailableSeats(concertId: Long, scheduleId: Long): List<SeatResponse> {
        val concert = concertService.getConcert(concertId)
        val schedule = concertScheduleService.findByConcertIdAndId(concert.id, scheduleId)

        val availableSeats = schedule.seats.filter { seat -> seat.isAvailable }

        return availableSeats.map { seat -> SeatResponse.Companion.from(seat) }
    }
}