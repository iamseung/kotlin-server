package kr.hhplus.be.server.queue.usecase

import io.mockk.*
import kr.hhplus.be.server.common.exception.AuthenticationException
import kr.hhplus.be.server.common.exception.AuthorizationException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.queue.dto.response.QueueStatusResponse
import kr.hhplus.be.server.queue.dto.response.QueueTokenResponse
import kr.hhplus.be.server.queue.entity.QueueStatus
import kr.hhplus.be.server.queue.entity.QueueToken
import kr.hhplus.be.server.queue.service.QueueTokenService
import kr.hhplus.be.server.user.entity.User
import kr.hhplus.be.server.user.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class QueueUseCaseTest {

    private lateinit var queueUseCase: QueueUseCase
    private lateinit var userService: UserService
    private lateinit var queueTokenService: QueueTokenService

    @BeforeEach
    fun setUp() {
        userService = mockk()
        queueTokenService = mockk()

        queueUseCase = QueueUseCase(
            userService = userService,
            queueTokenService = queueTokenService,
        )
    }

    @Test
    @DisplayName("대기열 토큰 발급 성공 - WAITING 상태로 생성")
    fun issueQueueToken_Success() {
        // given
        val userId = 1L
        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val queueToken = QueueToken(
            user = user,
            token = "test-token-uuid",
            queueStatus = QueueStatus.WAITING,
            queuePosition = 5
        )

        every { userService.getUser(userId) } returns user
        every { queueTokenService.createQueueToken(user) } returns queueToken

        // when
        val result = queueUseCase.issueQueueToken(userId)

        // then
        assertThat(result).isNotNull
        assertThat(result.token).isEqualTo("test-token-uuid")
        assertThat(result.queueStatus).isEqualTo(QueueStatus.WAITING)
        assertThat(result.queuePosition).isEqualTo(5)

        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { queueTokenService.createQueueToken(user) }
    }

    @Test
    @DisplayName("대기 상태 조회 성공 - WAITING 상태")
    fun getQueueStatus_Success_Waiting() {
        // given
        val token = "test-token-uuid"
        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val queueToken = QueueToken(
            user = user,
            token = token,
            queueStatus = QueueStatus.WAITING,
            queuePosition = 10
        )

        every { queueTokenService.getQueueTokenByToken(token) } returns queueToken

        // when
        val result = queueUseCase.getQueueStatus(token)

        // then
        assertThat(result).isNotNull
        assertThat(result.queueStatus).isEqualTo(QueueStatus.WAITING)
        assertThat(result.queuePosition).isEqualTo(10)
        assertThat(result.estimatedWaitTimeMinutes).isEqualTo(10) // 10명 * 1분

        verify(exactly = 1) { queueTokenService.getQueueTokenByToken(token) }
    }

    @Test
    @DisplayName("대기 상태 조회 성공 - ACTIVE 상태")
    fun getQueueStatus_Success_Active() {
        // given
        val token = "test-token-uuid"
        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val queueToken = QueueToken(
            user = user,
            token = token,
            queueStatus = QueueStatus.ACTIVE,
            queuePosition = 0
        )
        queueToken.activate()

        every { queueTokenService.getQueueTokenByToken(token) } returns queueToken

        // when
        val result = queueUseCase.getQueueStatus(token)

        // then
        assertThat(result).isNotNull
        assertThat(result.queueStatus).isEqualTo(QueueStatus.ACTIVE)
        assertThat(result.queuePosition).isEqualTo(0)
        assertThat(result.estimatedWaitTimeMinutes).isEqualTo(0)

        verify(exactly = 1) { queueTokenService.getQueueTokenByToken(token) }
    }

    @Test
    @DisplayName("대기 상태 조회 실패 - 유효하지 않은 토큰")
    fun getQueueStatus_Fail_InvalidToken() {
        // given
        val token = "invalid-token"

        every { queueTokenService.getQueueTokenByToken(token) } throws AuthenticationException(ErrorCode.INVALID_TOKEN)

        // when & then
        assertThatThrownBy {
            queueUseCase.getQueueStatus(token)
        }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessage(ErrorCode.INVALID_TOKEN.message)

        verify(exactly = 1) { queueTokenService.getQueueTokenByToken(token) }
    }

    @Test
    @DisplayName("대기열 토큰 검증 성공 - ACTIVE 상태")
    fun validateQueueToken_Success() {
        // given
        val token = "test-token-uuid"
        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val queueToken = spyk(
            QueueToken(
                user = user,
                token = token,
                queueStatus = QueueStatus.ACTIVE,
                queuePosition = 0,
                activatedAt = LocalDateTime.now(),
                expiresAt = LocalDateTime.now().plusHours(1)
            )
        )

        every { queueTokenService.getQueueTokenByToken(token) } returns queueToken
        every { queueToken.validateActive() } just Runs

        // when & then
        queueUseCase.validateQueueToken(token)

        verify(exactly = 1) { queueTokenService.getQueueTokenByToken(token) }
        verify(exactly = 1) { queueToken.validateActive() }
    }

    @Test
    @DisplayName("대기열 토큰 검증 실패 - WAITING 상태 (아직 활성화 안됨)")
    fun validateQueueToken_Fail_NotActive() {
        // given
        val token = "test-token-uuid"
        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val queueToken = spyk(
            QueueToken(
                user = user,
                token = token,
                queueStatus = QueueStatus.WAITING,
                queuePosition = 5
            )
        )

        every { queueTokenService.getQueueTokenByToken(token) } returns queueToken
        every { queueToken.validateActive() } throws AuthorizationException(ErrorCode.QUEUE_TOKEN_NOT_ACTIVE)

        // when & then
        assertThatThrownBy {
            queueUseCase.validateQueueToken(token)
        }
            .isInstanceOf(AuthorizationException::class.java)
            .hasMessage(ErrorCode.QUEUE_TOKEN_NOT_ACTIVE.message)

        verify(exactly = 1) { queueTokenService.getQueueTokenByToken(token) }
        verify(exactly = 1) { queueToken.validateActive() }
    }

    @Test
    @DisplayName("대기열 토큰 검증 실패 - 만료된 토큰")
    fun validateQueueToken_Fail_Expired() {
        // given
        val token = "test-token-uuid"
        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val queueToken = spyk(
            QueueToken(
                user = user,
                token = token,
                queueStatus = QueueStatus.ACTIVE,
                queuePosition = 0,
                activatedAt = LocalDateTime.now().minusHours(2),
                expiresAt = LocalDateTime.now().minusHours(1)
            )
        )

        every { queueTokenService.getQueueTokenByToken(token) } returns queueToken
        every { queueToken.validateActive() } throws AuthorizationException(ErrorCode.TOKEN_EXPIRED)

        // when & then
        assertThatThrownBy {
            queueUseCase.validateQueueToken(token)
        }
            .isInstanceOf(AuthorizationException::class.java)
            .hasMessage(ErrorCode.TOKEN_EXPIRED.message)

        verify(exactly = 1) { queueTokenService.getQueueTokenByToken(token) }
        verify(exactly = 1) { queueToken.validateActive() }
    }

    @Test
    @DisplayName("대기열 토큰 검증 실패 - 유효하지 않은 토큰")
    fun validateQueueToken_Fail_InvalidToken() {
        // given
        val token = "invalid-token"

        every { queueTokenService.getQueueTokenByToken(token) } throws AuthenticationException(ErrorCode.INVALID_TOKEN)

        // when & then
        assertThatThrownBy {
            queueUseCase.validateQueueToken(token)
        }
            .isInstanceOf(AuthenticationException::class.java)
            .hasMessage(ErrorCode.INVALID_TOKEN.message)

        verify(exactly = 1) { queueTokenService.getQueueTokenByToken(token) }
    }
}
