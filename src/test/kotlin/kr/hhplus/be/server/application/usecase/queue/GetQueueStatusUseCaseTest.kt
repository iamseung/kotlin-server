package kr.hhplus.be.server.application.usecase.queue

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.domain.queue.model.QueueTokenModel
import kr.hhplus.be.server.domain.queue.service.QueueTokenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetQueueStatusUseCaseTest {

    private lateinit var getQueueStatusUseCase: GetQueueStatusUseCase
    private lateinit var queueTokenService: QueueTokenService

    @BeforeEach
    fun setUp() {
        queueTokenService = mockk()

        getQueueStatusUseCase = GetQueueStatusUseCase(
            queueTokenService = queueTokenService
        )
    }

    @Test
    @DisplayName("대기열 상태 조회 성공")
    fun execute_Success() {
        // given
        val token = "test-token"
        val command = GetQueueStatusCommand(token = token)

        val queueToken = QueueTokenModel.create(1L, 10)

        every { queueTokenService.getQueueTokenByToken(token) } returns queueToken

        // when
        val result = getQueueStatusUseCase.execute(command)

        // then
        assertThat(result.token).isEqualTo(queueToken.token)
        assertThat(result.position).isEqualTo(10L)
        verify(exactly = 1) { queueTokenService.getQueueTokenByToken(token) }
    }
}
