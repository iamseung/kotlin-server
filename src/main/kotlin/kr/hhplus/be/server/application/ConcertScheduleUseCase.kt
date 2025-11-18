package kr.hhplus.be.server.application

import kr.hhplus.be.server.interfaces.dto.response.ConcertScheduleResponse
import kr.hhplus.be.server.interfaces.dto.response.SeatResponse
import kr.hhplus.be.server.concert.service.ConcertScheduleService
import kr.hhplus.be.server.concert.service.ConcertService
import kr.hhplus.be.server.concert.service.SeatService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class ConcertScheduleUseCase(
    private val concertService: ConcertService,
    private val concertScheduleService: ConcertScheduleService,
    private val seatService: SeatService,
) {

    @Transactional(readOnly = true)
    fun getAvailableSchedules(concertId: Long): List<ConcertScheduleResponse> {
        concertService.getConcert(concertId)
        val availableSchedules = concertScheduleService.findByConcertId(concertId)
            .filter { schedule -> schedule.isAvailable }

        return availableSchedules.map { ConcertScheduleResponse.Companion.from(it) }
    }

    @Transactional(readOnly = true)
    fun getAvailableSeats(concertId: Long, scheduleId: Long): List<SeatResponse> {
        concertService.getConcert(concertId)
        concertScheduleService.findByConcertIdAndId(concertId, scheduleId)

        val availableSeats = seatService.findAllByConcertScheduleId(scheduleId)
            .filter { seat -> seat.isAvailable }

        return availableSeats.map { seat -> SeatResponse.Companion.from(seat) }
    }
}