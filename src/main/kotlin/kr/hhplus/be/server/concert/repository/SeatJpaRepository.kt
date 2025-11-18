package kr.hhplus.be.server.concert.repository

import kr.hhplus.be.server.concert.entity.Seat
import kr.hhplus.be.server.concert.entity.SeatStatus
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import jakarta.persistence.LockModeType

interface SeatJpaRepository : JpaRepository<Seat, Long> {
    @EntityGraph(attributePaths = ["concertSchedule"])
    fun findByIdAndConcertScheduleId(id: Long, concertScheduleId: Long): Seat?

    fun findAllByConcertScheduleId(concertScheduleId: Long): List<Seat>

    fun findAllByConcertScheduleIdAndSeatStatus(concertScheduleId: Long, status: SeatStatus): List<Seat>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    fun findByIdWithLock(id: Long): Seat?
}
