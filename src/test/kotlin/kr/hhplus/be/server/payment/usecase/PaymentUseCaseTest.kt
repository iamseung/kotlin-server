package kr.hhplus.be.server.payment.usecase

import io.mockk.*
import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.concert.entity.Concert
import kr.hhplus.be.server.concert.entity.ConcertSchedule
import kr.hhplus.be.server.concert.entity.Seat
import kr.hhplus.be.server.concert.entity.SeatStatus
import kr.hhplus.be.server.payment.entity.Payment
import kr.hhplus.be.server.payment.entity.TransactionType
import kr.hhplus.be.server.payment.service.PaymentService
import kr.hhplus.be.server.point.entity.Point
import kr.hhplus.be.server.point.service.PointHistoryService
import kr.hhplus.be.server.point.service.PointService
import kr.hhplus.be.server.queue.service.QueueTokenService
import kr.hhplus.be.server.reservation.entity.Reservation
import kr.hhplus.be.server.reservation.service.ReservationService
import kr.hhplus.be.server.user.entity.User
import kr.hhplus.be.server.user.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PaymentUseCaseTest {

    private lateinit var paymentUseCase: PaymentUseCase
    private lateinit var userService: UserService
    private lateinit var reservationService: ReservationService
    private lateinit var pointService: PointService
    private lateinit var pointHistoryService: PointHistoryService
    private lateinit var paymentService: PaymentService
    private lateinit var queueTokenService: QueueTokenService

    @BeforeEach
    fun setUp() {
        userService = mockk()
        reservationService = mockk()
        pointService = mockk()
        pointHistoryService = mockk()
        paymentService = mockk()
        queueTokenService = mockk()

        paymentUseCase = PaymentUseCase(
            userService = userService,
            reservationService = reservationService,
            pointService = pointService,
            pointHistoryService = pointHistoryService,
            paymentService = paymentService,
            queueTokenService = queueTokenService,
        )
    }

    @Test
    @DisplayName("결제 처리 성공")
    fun processPayment_Success() {
        // given
        val userId = 1L
        val reservationId = 1L

        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val seat = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.TEMPORARY_RESERVED,
            price = 50000,
        )
        val reservation = Reservation.of(user, seat)
        val point = Point(user = user, balance = 100000)
        val payment = Payment.of(reservation, user, seat.price)
        val queueToken = "test-queue-token"

        every { userService.getUser(userId) } returns user
        every { reservationService.findById(reservationId) } returns reservation
        every { pointService.usePoint(user.id, seat.price) } returns point
        every { pointHistoryService.savePointHistory(user, seat.price, TransactionType.USE) } just Runs
        every { paymentService.savePayment(any()) } returns payment
        every { queueTokenService.getQueueTokenByToken(queueToken) } returns mockk(relaxed = true)
        every { queueTokenService.expireQueueToken(any()) } returns mockk(relaxed = true)

        // when
        val result = paymentUseCase.processPayment(userId, reservationId, queueToken)

        // then
        assertThat(result).isNotNull
        assertThat(result.amount).isEqualTo(seat.price)
        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { reservationService.findById(reservationId) }
        verify(exactly = 1) { pointService.usePoint(user.id, seat.price) }
        verify(exactly = 1) { pointHistoryService.savePointHistory(user, seat.price, TransactionType.USE) }
        verify(exactly = 1) { paymentService.savePayment(any()) }
    }

    @Test
    @DisplayName("결제 처리 실패 - 다른 사용자의 예약")
    fun processPayment_Fail_InvalidUser() {
        // given
        val userId = 1L
        val reservationId = 1L
        val otherUserId = 2L

        val user = spyk(User(userName = "testUser", email = "test@test.com", password = "password"))
        val otherUser = spyk(User(userName = "otherUser", email = "other@test.com", password = "password"))
        every { user.id } returns userId
        every { otherUser.id } returns otherUserId

        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val seat = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.TEMPORARY_RESERVED,
            price = 50000,
        )
        val reservation = spyk(Reservation.of(otherUser, seat))
        every { reservation.user } returns otherUser
        val queueToken = "test-queue-token"

        every { userService.getUser(userId) } returns user
        every { reservationService.findById(reservationId) } returns reservation

        // when & then
        assertThatThrownBy {
            paymentUseCase.processPayment(userId, reservationId, queueToken)
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION)

        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { reservationService.findById(reservationId) }
        verify(exactly = 0) { pointService.usePoint(any(), any()) }
        verify(exactly = 0) { paymentService.savePayment(any()) }
    }

    @Test
    @DisplayName("결제 처리 실패 - 이미 확정된 예약")
    fun processPayment_Fail_AlreadyConfirmed() {
        // given
        val userId = 1L
        val reservationId = 1L

        val user = spyk(User(userName = "testUser", email = "test@test.com", password = "password"))
        every { user.id } returns userId

        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val seat = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.RESERVED,
            price = 50000,
        )
        val reservation = mockk<Reservation>(relaxed = true)
        every { reservation.user } returns user
        every { reservation.seat } returns seat
        every { reservation.validateOwnership(userId) } just Runs
        every { reservation.validatePayable() } throws BusinessException(ErrorCode.INVALID_RESERVATION_STATUS)
        val queueToken = "test-queue-token"

        every { userService.getUser(userId) } returns user
        every { reservationService.findById(reservationId) } returns reservation

        // when & then
        assertThatThrownBy {
            paymentUseCase.processPayment(userId, reservationId, queueToken)
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS)

        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { reservationService.findById(reservationId) }
        verify(exactly = 1) { reservation.validateOwnership(userId) }
        verify(exactly = 1) { reservation.validatePayable() }
        verify(exactly = 0) { pointService.usePoint(any(), any()) }
        verify(exactly = 0) { paymentService.savePayment(any()) }
    }

    @Test
    @DisplayName("결제 처리 실패 - 포인트 부족")
    fun processPayment_Fail_InsufficientPoints() {
        // given
        val userId = 1L
        val reservationId = 1L

        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val seat = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.TEMPORARY_RESERVED,
            price = 50000,
        )
        val reservation = Reservation.of(user, seat)
        val queueToken = "test-queue-token"

        every { userService.getUser(userId) } returns user
        every { reservationService.findById(reservationId) } returns reservation
        every { pointService.usePoint(user.id, seat.price) } throws BusinessException(ErrorCode.INSUFFICIENT_POINTS)

        // when & then
        assertThatThrownBy {
            paymentUseCase.processPayment(userId, reservationId, queueToken)
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_POINTS)

        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { reservationService.findById(reservationId) }
        verify(exactly = 1) { pointService.usePoint(user.id, seat.price) }
        verify(exactly = 0) { paymentService.savePayment(any()) }
    }
}
