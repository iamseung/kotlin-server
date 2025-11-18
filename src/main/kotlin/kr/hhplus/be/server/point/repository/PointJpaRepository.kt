package kr.hhplus.be.server.point.repository

import kr.hhplus.be.server.point.entity.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import jakarta.persistence.LockModeType

interface PointJpaRepository : JpaRepository<Point, Long> {
    fun findByUserId(userId: Long): Point?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Point p WHERE p.user.id = :userId")
    fun findByUserIdWithLock(userId: Long): Point?
}
