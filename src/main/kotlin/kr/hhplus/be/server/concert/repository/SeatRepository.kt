package kr.hhplus.be.server.concert.repository

import kr.hhplus.be.server.concert.entity.Seat
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SeatRepository : JpaRepository<Seat, Long> {
    @EntityGraph(attributePaths = ["concertSchedule"])
    fun findByIdAndConcertScheduleId(id: Long, concertScheduleId: Long): Seat?
}
