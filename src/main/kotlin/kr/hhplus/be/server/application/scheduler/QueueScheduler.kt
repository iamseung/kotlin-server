package kr.hhplus.be.server.application.scheduler

import kr.hhplus.be.server.domain.queue.service.QueueTokenService
import kr.hhplus.be.server.infrastructure.persistence.queue.repository.RedisQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class QueueScheduler(
    private val queueTokenService: QueueTokenService,
    private val redisQueueRepository: RedisQueueRepository,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val MAX_ACTIVE_USERS = 100 // 동시 접속 가능한 총 인원
        private const val BATCH_ACTIVATION_SIZE = 10 // 10초당 활성화할 증가분
    }

    /**
     * 대기열에서 ACTIVE로 활성화
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    fun activateWaitingTokens() {
        try {
            val activeCount = redisQueueRepository.getActiveQueueSize()
            val availableSlots = (MAX_ACTIVE_USERS - activeCount).toInt()

            if (availableSlots > 0) {
                val tokensToActivate = minOf(availableSlots, BATCH_ACTIVATION_SIZE)
                val activatedCount = queueTokenService.activateWaitingTokens(tokensToActivate)

                log.info("Activated $activatedCount waiting tokens. Current active: ${activeCount + activatedCount}")
            }
        } catch (e: Exception) {
            log.error("Error while activating waiting tokens", e)
        }
    }

    /**
     * 만료된 ACTIVE 토큰 정리
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    fun cleanupExpiredTokens() {
        try {
            val expiredCount = queueTokenService.cleanupExpiredTokens()

            if (expiredCount > 0) {
                log.info("Expired $expiredCount tokens")
            }
        } catch (e: Exception) {
            log.error("Error while cleaning up expired tokens", e)
        }
    }
}
