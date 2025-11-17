package kr.hhplus.be.server.queue.repository

import kr.hhplus.be.server.queue.entity.QueueStatus
import kr.hhplus.be.server.queue.entity.QueueToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface QueueTokenRepository : JpaRepository<QueueToken, Long> {


    fun findByToken(token: String): Optional<QueueToken>

    fun findByUserIdAndQueueStatus(userId: Long, queueStatus: QueueStatus): Optional<QueueToken>

    fun countByQueueStatus(queueStatus: QueueStatus): Long

    fun findAllByQueueStatusOrderByCreatedAtAsc(queueStatus: QueueStatus): List<QueueToken>

    @Query("""
        SELECT COUNT(q)
        FROM QueueToken q
        WHERE q.queueStatus = 'WAITING'
        AND q.queuePosition < :position
    """)
    fun countWaitingTokensBeforePosition(position: Int): Long
}
