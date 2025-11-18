package kr.hhplus.be.server.queue.scheduler

import kr.hhplus.be.server.queue.entity.QueueStatus
import kr.hhplus.be.server.queue.repository.QueueTokenRepository
import kr.hhplus.be.server.queue.service.QueueTokenService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 대기열 관리 스케줄러
 *
 * 주기적으로 실행되어:
 * 1. 대기 중인 토큰을 활성화
 * 2. 만료된 토큰 정리
 * 3. 대기 순서 재조정
 */
@Component
class QueueScheduler(
    private val queueTokenService: QueueTokenService,
    private val queueTokenRepository: QueueTokenRepository,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        // 동시에 활성화할 수 있는 최대 사용자 수
        private const val MAX_ACTIVE_USERS = 100

        // 한 번에 활성화할 사용자 수
        private const val BATCH_ACTIVATION_SIZE = 10
    }

    /**
     * 대기 중인 토큰을 주기적으로 활성화 (10초마다)
     */
    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    @Transactional
    fun activateWaitingTokens() {
        try {
            // 현재 활성 상태인 토큰 개수 확인
            val activeCount = queueTokenRepository.countByQueueStatus(QueueStatus.ACTIVE)

            // 최대 동시 접속자 수를 초과하지 않는 범위에서 활성화
            val availableSlots = (MAX_ACTIVE_USERS - activeCount).toInt()

            if (availableSlots > 0) {
                val tokensToActivate = minOf(availableSlots, BATCH_ACTIVATION_SIZE)
                queueTokenService.activateWaitingTokens(tokensToActivate)

                log.info("Activated $tokensToActivate waiting tokens. Current active: ${activeCount + tokensToActivate}")
            }

            // 대기 순서 재조정
            queueTokenService.updateWaitingPositions()
        } catch (e: Exception) {
            log.error("Error while activating waiting tokens", e)
        }
    }

    /**
     * 만료된 토큰을 주기적으로 정리 (1분마다)
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    fun cleanupExpiredTokens() {
        try {
            val now = LocalDateTime.now()

            // ACTIVE 상태이지만 만료 시간이 지난 토큰들 조회
            val activeTokens = queueTokenRepository.findAllByQueueStatusOrderByCreatedAtAsc(QueueStatus.ACTIVE)

            var expiredCount = 0
            activeTokens.forEach { token ->
                token.expiresAt?.let { expiresAt ->
                    if (now.isAfter(expiresAt)) {
                        queueTokenService.expireQueueToken(token)
                        expiredCount++
                    }
                }
            }

            if (expiredCount > 0) {
                log.info("Expired $expiredCount tokens")
            }
        } catch (e: Exception) {
            log.error("Error while cleaning up expired tokens", e)
        }
    }
}
