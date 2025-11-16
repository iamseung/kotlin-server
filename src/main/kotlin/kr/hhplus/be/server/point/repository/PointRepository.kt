package kr.hhplus.be.server.point.repository

import kr.hhplus.be.server.point.entity.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PointRepository : JpaRepository<Point, Long> {

    fun findByUserId(userId: Long): Point?
}