package kr.hhplus.be.server.reservation.usecase

import io.mockk.*
import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.concert.entity.Concert
import kr.hhplus.be.server.concert.entity.ConcertSchedule
import kr.hhplus.be.server.concert.entity.Seat
import kr.hhplus.be.server.concert.entity.SeatStatus
import kr.hhplus.be.server.concert.service.SeatService
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

class ReservationUseCaseTest {

    private lateinit var reservationUseCase: ReservationUseCase
    private lateinit var userService: UserService
    private lateinit var seatService: SeatService
    private lateinit var reservationService: ReservationService
    private lateinit var queueTokenService: QueueTokenService

    @BeforeEach
    fun setUp() {
        userService = mockk()
        seatService = mockk()
        reservationService = mockk()
        queueTokenService = mockk()

        reservationUseCase = ReservationUseCase(
            userService = userService,
            seatService = seatService,
            reservationService = reservationService,
            queueTokenService = queueTokenService,
        )
    }

    @Test
    @DisplayName("예약 생성 성공 - 임시 예약 (TEMPORARY)")
    fun createReservation_Success() {
        // given
        val userId = 1L
        val scheduleId = 1L
        val seatId = 1L

        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val seat = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.AVAILABLE,
            price = 50000,
        )
        val reservation = Reservation.of(user, seat)
        val queueToken = "test-queue-token"

        every { userService.getUser(userId) } returns user
        every { seatService.findByIdAndConcertScheduleId(seatId, scheduleId) } returns seat
        every { queueTokenService.getQueueTokenByToken(queueToken) } returns mockk(relaxed = true)
        every { reservationService.save(any()) } returns reservation

        // when
        val result = reservationUseCase.createReservation(userId, scheduleId, seatId, queueToken)

        // then
        assertThat(result).isNotNull
        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { seatService.findByIdAndConcertScheduleId(seatId, scheduleId) }
        verify(exactly = 1) { reservationService.save(any()) }
    }

    @Test
    @DisplayName("예약 생성 실패 - 좌석이 이미 예약됨")
    fun createReservation_Fail_SeatNotAvailable() {
        // given
        val userId = 1L
        val scheduleId = 1L
        val seatId = 1L

        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val seat = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.RESERVED, // 이미 예약됨
            price = 50000,
        )
        val queueToken = "test-queue-token"

        every { userService.getUser(userId) } returns user
        every { seatService.findByIdAndConcertScheduleId(seatId, scheduleId) } returns seat
        every { queueTokenService.getQueueTokenByToken(queueToken) } returns mockk(relaxed = true)

        // when & then
        assertThatThrownBy {
            reservationUseCase.createReservation(userId, scheduleId, seatId, queueToken)
        }.isInstanceOf(BusinessException::class.java)

        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { seatService.findByIdAndConcertScheduleId(seatId, scheduleId) }
        verify(exactly = 0) { reservationService.save(any()) }
    }

    @Test
    @DisplayName("예약 생성 실패 - 콘서트 일정이 만료됨")
    fun createReservation_Fail_ScheduleExpired() {
        // given
        val userId = 1L
        val scheduleId = 1L
        val seatId = 1L

        val user = User(userName = "testUser", email = "test@test.com", password = "password")
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().minusDays(1), // 이미 지난 날짜
        )
        val seat = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.AVAILABLE,
            price = 50000,
        )
        val queueToken = "test-queue-token"

        every { userService.getUser(userId) } returns user
        every { seatService.findByIdAndConcertScheduleId(seatId, scheduleId) } returns seat
        every { queueTokenService.getQueueTokenByToken(queueToken) } returns mockk(relaxed = true)

        // when & then
        assertThatThrownBy {
            reservationUseCase.createReservation(userId, scheduleId, seatId, queueToken)
        }.isInstanceOf(BusinessException::class.java)

        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { seatService.findByIdAndConcertScheduleId(seatId, scheduleId) }
        verify(exactly = 0) { reservationService.save(any()) }
    }

    @Test
    @DisplayName("예약 조회 성공")
    fun getConcertReservations_Success() {
        // given
        val userId = 1L
        val user = User(userName = "testUser", email = "test@test.com", password = "password")
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
        val reservation = Reservation.of(user, seat)
        val reservations = listOf(reservation)

        every { userService.getUser(userId) } returns user
        every { reservationService.findAllByUserId(user.id) } returns reservations

        // when
        val result = reservationUseCase.getConcertReservations(userId)

        // then
        assertThat(result).isNotNull
        assertThat(result).hasSize(1)
        verify(exactly = 1) { userService.getUser(userId) }
        verify(exactly = 1) { reservationService.findAllByUserId(user.id) }
    }
}
