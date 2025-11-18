package kr.hhplus.be.server.concert.repository

import kr.hhplus.be.server.concert.entity.ConcertSchedule
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConcertScheduleRepository : JpaRepository<ConcertSchedule, Long> {

    // Concert ID로 모든 일정 조회
    fun findByConcertId(concertId: Long): List<ConcertSchedule>

    // Concert ID와 Schedule ID로 특정 일정 조회
    @EntityGraph(attributePaths = ["seats"])
    fun findByConcertIdAndId(concertId: Long, scheduleId: Long): ConcertSchedule?
}
