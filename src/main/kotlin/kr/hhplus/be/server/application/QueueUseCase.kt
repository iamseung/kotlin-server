package kr.hhplus.be.server.application

import kr.hhplus.be.server.interfaces.dto.response.QueueStatusResponse
import kr.hhplus.be.server.interfaces.dto.response.QueueTokenResponse
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

    fun issueQueueToken(userId: Long): QueueTokenResponse {
        userService.getUser(userId)
        val queueToken = queueTokenService.createQueueToken(userId)

        return QueueTokenResponse.Companion.from(queueToken)
    }

    @Transactional(readOnly = true)
    fun getQueueStatus(token: String): QueueStatusResponse {
        val queueToken = queueTokenService.getQueueTokenByToken(token)

        return QueueStatusResponse.Companion.from(queueToken)
    }

    @Transactional(readOnly = true)
    fun validateQueueToken(token: String) {
        val queueToken = queueTokenService.getQueueTokenByToken(token)
        queueToken.validateActive()
    }
}