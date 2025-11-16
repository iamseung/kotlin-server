package kr.hhplus.be.server.point.usecase

import io.mockk.*
import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.payment.entity.TransactionType
import kr.hhplus.be.server.point.entity.Point
import kr.hhplus.be.server.point.service.PointHistoryService
import kr.hhplus.be.server.point.service.PointService
import kr.hhplus.be.server.user.entity.User
import kr.hhplus.be.server.user.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PointUseCaseTest {

    private lateinit var pointUseCase: PointUseCase
    private lateinit var userService: UserService
    private lateinit var pointService: PointService
    private lateinit var pointHistoryService: PointHistoryService

    @BeforeEach
    fun setUp() {
        userService = mockk()
        pointService = mockk()
        pointHistoryService = mockk()

        pointUseCase = PointUseCase(
            userService = userService,
            pointService = pointService,
            pointHistoryService = pointHistoryService,
        )
    }

    @Test
    @DisplayName("포인트 조회 성공")
    fun getPoints_Success() {
        // given
        val userId = 1L
        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val point = Point(user = user, balance = 50000)

        every { userService.getUser(userId) } returns user
        every { pointService.getPointByUserId(user.id) } returns point

        // when
        val result = pointUseCase.getPoints(userId)

        // then
        assertThat(result).isNotNull
        assertThat(result.balance).isEqualTo(50000)
        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { pointService.getPointByUserId(user.id) }
    }

    @Test
    @DisplayName("포인트 충전 성공")
    fun chargePoint_Success() {
        // given
        val userId = 1L
        val amount = 10000
        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val point = Point(user = user, balance = 50000)
        val chargedPoint = Point(user = user, balance = 60000)

        every { userService.getUser(userId) } returns user
        every { pointService.chargePoint(user.id, amount) } returns chargedPoint
        every { pointHistoryService.savePointHistory(user, amount, TransactionType.CHARGE) } just Runs

        // when
        val result = pointUseCase.chargePoint(userId, amount)

        // then
        assertThat(result).isNotNull
        assertThat(result.balance).isEqualTo(60000)
        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { pointService.chargePoint(user.id, amount) }
        verify(exactly = 1) { pointHistoryService.savePointHistory(user, amount, TransactionType.CHARGE) }
    }

    @Test
    @DisplayName("포인트 충전 실패 - 잘못된 충전 금액 (0 이하)")
    fun chargePoint_Fail_InvalidAmount() {
        // given
        val userId = 1L
        val amount = -1000
        val user = User(userName = "testUser", email = "test@test.com", password = "password")

        every { userService.getUser(userId) } returns user
        every { pointService.chargePoint(user.id, amount) } throws BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT)

        // when & then
        assertThatThrownBy {
            pointUseCase.chargePoint(userId, amount)
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CHARGE_AMOUNT)

        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { pointService.chargePoint(user.id, amount) }
        verify(exactly = 0) { pointHistoryService.savePointHistory(any(), any(), any()) }
    }

    @Test
    @DisplayName("포인트 충전 실패 - 0원 충전")
    fun chargePoint_Fail_ZeroAmount() {
        // given
        val userId = 1L
        val amount = 0
        val user = User(userName = "testUser", email = "test@test.com", password = "password")

        every { userService.getUser(userId) } returns user
        every { pointService.chargePoint(user.id, amount) } throws BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT)

        // when & then
        assertThatThrownBy {
            pointUseCase.chargePoint(userId, amount)
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CHARGE_AMOUNT)

        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { pointService.chargePoint(user.id, amount) }
        verify(exactly = 0) { pointHistoryService.savePointHistory(any(), any(), any()) }
    }
}
