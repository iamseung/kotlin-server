package kr.hhplus.be.server.domain.queue.service

import kr.hhplus.be.server.domain.queue.model.QueueStatus
import kr.hhplus.be.server.domain.queue.model.QueueTokenModel
import kr.hhplus.be.server.infrastructure.persistence.queue.repository.RedisQueueRepository
import org.springframework.stereotype.Service

@Service
class QueueTokenService(
    private val redisQueueRepository: RedisQueueRepository,
) {

    fun createQueueToken(userId: Long): QueueTokenModel {
        // Redis에서 ACTIVE 상태 확인
        if (redisQueueRepository.isInActiveQueue(userId)) {
            return redisQueueRepository.findAllByStatus(QueueStatus.ACTIVE)
                .find { it.userId == userId }
                ?: createAndSaveToken(userId, QueueStatus.ACTIVE)
        }

        // Redis에서 WAITING 상태 확인
        if (redisQueueRepository.isInWaitingQueue(userId)) {
            return redisQueueRepository.findAllByStatus(QueueStatus.WAITING)
                .find { it.userId == userId }
                ?: createAndSaveToken(userId, QueueStatus.WAITING)
        }

        // 신규 사용자 - Redis 대기열에 추가
        redisQueueRepository.addToWaitingQueue(userId)
        return createAndSaveToken(userId, QueueStatus.WAITING)
    }

    private fun createAndSaveToken(userId: Long, status: QueueStatus): QueueTokenModel {
        val queueTokenModel = QueueTokenModel.create(userId)
        if (status == QueueStatus.ACTIVE) {
            queueTokenModel.activate()
        }
        return redisQueueRepository.save(queueTokenModel)
    }

    fun getQueueTokenByToken(token: String): QueueTokenModel {
        return redisQueueRepository.findByTokenOrThrow(token)
    }

    /**
     * 대기 순번 조회 (WAITING 상태일 때만 의미 있음)
     */
    fun getQueuePosition(userId: Long): Long {
        return redisQueueRepository.getPosition(userId) ?: 0
    }

    fun expireQueueToken(queueTokenModel: QueueTokenModel): QueueTokenModel {
        queueTokenModel.expire()
        redisQueueRepository.removeFromActiveQueue(queueTokenModel.userId)
        return redisQueueRepository.update(queueTokenModel)
    }

    fun activateWaitingTokens(count: Int): Int {
        val activatedUserIds = redisQueueRepository.activateWaitingUsers(count)
        return activatedUserIds.size
    }

    fun cleanupExpiredTokens(): Int {
        val expiredUserIds = redisQueueRepository.removeExpiredActiveTokens()
        return expiredUserIds.size
    }
}
