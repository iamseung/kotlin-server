package kr.hhplus.be.server.concert.domain.repository

import kr.hhplus.be.server.concert.domain.model.Seat
import kr.hhplus.be.server.concert.domain.model.SeatStatus

interface SeatRepository {
    fun save(seat: Seat): Seat
    fun findById(id: Long): Seat?
    fun findByIdOrThrow(id: Long): Seat
    fun findByIdWithLock(id: Long): Seat?
    fun findAllByConcertScheduleId(concertScheduleId: Long): List<Seat>
    fun findAllByConcertScheduleIdAndStatus(concertScheduleId: Long, status: SeatStatus): List<Seat>
}
