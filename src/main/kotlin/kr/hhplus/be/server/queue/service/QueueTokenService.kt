package kr.hhplus.be.server.queue.service

import kr.hhplus.be.server.common.exception.AuthenticationException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.queue.entity.QueueStatus
import kr.hhplus.be.server.queue.entity.QueueToken
import kr.hhplus.be.server.queue.repository.QueueTokenRepository
import kr.hhplus.be.server.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueueTokenService(
    private val queueTokenRepository: QueueTokenRepository,
) {

    /**
     * 대기열 토큰 생성
     */
    @Transactional
    fun createQueueToken(user: User): QueueToken {
        // 이미 활성 상태인 토큰이 있는지 확인
        val existingToken = queueTokenRepository.findByUserIdAndQueueStatus(user.id, QueueStatus.ACTIVE)
        if (existingToken.isPresent) {
            return existingToken.get()
        }

        // 대기 중인 토큰이 있는지 확인
        val waitingToken = queueTokenRepository.findByUserIdAndQueueStatus(user.id, QueueStatus.WAITING)
        if (waitingToken.isPresent) {
            return waitingToken.get()
        }

        // 현재 대기 중인 토큰 개수를 조회하여 순서 결정
        val waitingCount = queueTokenRepository.countByQueueStatus(QueueStatus.WAITING)
        val position = (waitingCount + 1).toInt()

        val queueToken = QueueToken.of(user, position)
        return queueTokenRepository.save(queueToken)
    }

    /**
     * 토큰으로 대기열 토큰 조회
     */
    @Transactional(readOnly = true)
    fun getQueueTokenByToken(token: String): QueueToken {
        return queueTokenRepository.findByToken(token)
            .orElseThrow { AuthenticationException(ErrorCode.INVALID_TOKEN) }
    }

    /**
     * 대기열 토큰 만료 처리
     */
    @Transactional
    fun expireQueueToken(queueToken: QueueToken): QueueToken {
        queueToken.expire()
        return queueTokenRepository.save(queueToken)
    }

    /**
     * 대기 중인 토큰들의 순서 재조정
     * (활성화된 토큰들이 만료되면 대기 순서가 앞당겨짐)
     */
    @Transactional
    fun updateWaitingPositions() {
        val waitingTokens = queueTokenRepository.findAllByQueueStatusOrderByCreatedAtAsc(QueueStatus.WAITING)

        waitingTokens.forEachIndexed { index, token ->
            token.updatePosition(index + 1)
        }

        queueTokenRepository.saveAll(waitingTokens)
    }

    /**
     * 대기 중인 토큰 중 일정 개수를 활성화
     */
    @Transactional
    fun activateWaitingTokens(count: Int) {
        val waitingTokens = queueTokenRepository.findAllByQueueStatusOrderByCreatedAtAsc(QueueStatus.WAITING)

        waitingTokens.take(count).forEach { token ->
            token.activate()
        }

        queueTokenRepository.saveAll(waitingTokens.take(count))
    }
}
