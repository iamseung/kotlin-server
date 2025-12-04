package kr.hhplus.be.server.application.usecase.queue

import kr.hhplus.be.server.domain.queue.service.QueueTokenService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ValidateQueueTokenUseCase(
    private val queueTokenService: QueueTokenService,
) {

    @Transactional(readOnly = true)
    fun execute(command: ValidateQueueTokenCommand) {
        // 1. 대기열 토큰 조회 및 검증
        val queueToken = queueTokenService.getQueueTokenByToken(command.token)
        queueToken.validateActive()
    }
}
