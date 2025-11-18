package kr.hhplus.be.server.point.repository

import kr.hhplus.be.server.point.entity.PointHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PointHistoryRepository : JpaRepository<PointHistory, Long>
