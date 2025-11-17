package kr.hhplus.be.server.queue.usecase

import kr.hhplus.be.server.queue.dto.response.QueueStatusResponse
import kr.hhplus.be.server.queue.dto.response.QueueTokenResponse
import kr.hhplus.be.server.queue.service.QueueTokenService
import kr.hhplus.be.server.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class QueueUseCase(
    private val userService: UserService,
    private val queueTokenService: QueueTokenService,
) {

    /**
     * 대기열 토큰 발급
     */
    fun issueQueueToken(userId: Long): QueueTokenResponse {
        val user = userService.getUser(userId)
        val queueToken = queueTokenService.createQueueToken(user)

        return QueueTokenResponse.from(queueToken)
    }

    /**
     * 대기 상태 조회
     */
    @Transactional(readOnly = true)
    fun getQueueStatus(token: String): QueueStatusResponse {
        val queueToken = queueTokenService.getQueueTokenByToken(token)

        return QueueStatusResponse.from(queueToken)
    }

    /**
     * 대기열 토큰 검증 (인터셉터에서 사용)
     */
    @Transactional(readOnly = true)
    fun validateQueueToken(token: String) {
        val queueToken = queueTokenService.getQueueTokenByToken(token)
        queueToken.validateActive()
    }
}
