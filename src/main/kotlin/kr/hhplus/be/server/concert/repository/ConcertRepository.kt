package kr.hhplus.be.server.concert.repository

import kr.hhplus.be.server.concert.entity.Concert
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConcertRepository : JpaRepository<Concert, Long>
