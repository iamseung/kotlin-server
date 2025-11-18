package kr.hhplus.be.server.concert.usecase

import io.mockk.*
import kr.hhplus.be.server.concert.entity.Concert
import kr.hhplus.be.server.concert.entity.ConcertSchedule
import kr.hhplus.be.server.concert.entity.Seat
import kr.hhplus.be.server.concert.entity.SeatStatus
import kr.hhplus.be.server.concert.service.ConcertScheduleService
import kr.hhplus.be.server.concert.service.ConcertService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ConcertScheduleUseCaseTest {

    private lateinit var concertScheduleUseCase: ConcertScheduleUseCase
    private lateinit var concertService: ConcertService
    private lateinit var concertScheduleService: ConcertScheduleService

    @BeforeEach
    fun setUp() {
        concertService = mockk()
        concertScheduleService = mockk()

        concertScheduleUseCase = ConcertScheduleUseCase(
            concertService = concertService,
            concertScheduleService = concertScheduleService,
        )
    }

    @Test
    @DisplayName("예약 가능한 콘서트 일정 조회 성공")
    fun getAvailableSchedules_Success() {
        // given
        val concertId = 1L
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule1 = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val schedule2 = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(20),
        )
        val schedules = listOf(schedule1, schedule2)

        every { concertService.getConcert(concertId) } returns concert
        every { concertScheduleService.findByConcertId(concert.id) } returns schedules

        // when
        val result = concertScheduleUseCase.getAvailableSchedules(concertId)

        // then
        assertThat(result).hasSize(2)
        verify(exactly = 1) { concertService.getConcert(concertId) }
        verify(exactly = 1) { concertScheduleService.findByConcertId(concert.id) }
    }

    @Test
    @DisplayName("예약 가능한 콘서트 일정 조회 - 만료된 일정 필터링")
    fun getAvailableSchedules_FilterExpired() {
        // given
        val concertId = 1L
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val availableSchedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val expiredSchedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().minusDays(1), // 만료된 일정
        )
        val schedules = listOf(availableSchedule, expiredSchedule)

        every { concertService.getConcert(concertId) } returns concert
        every { concertScheduleService.findByConcertId(concert.id) } returns schedules

        // when
        val result = concertScheduleUseCase.getAvailableSchedules(concertId)

        // then
        assertThat(result).hasSize(1) // 만료된 일정은 제외됨
        verify(exactly = 1) { concertService.getConcert(concertId) }
        verify(exactly = 1) { concertScheduleService.findByConcertId(concert.id) }
    }

    @Test
    @DisplayName("예약 가능한 콘서트 일정 조회 - 모든 일정이 만료된 경우 빈 목록 반환")
    fun getAvailableSchedules_EmptyWhenAllExpired() {
        // given
        val concertId = 1L
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val expiredSchedule1 = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().minusDays(10),
        )
        val expiredSchedule2 = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().minusDays(5),
        )
        val schedules = listOf(expiredSchedule1, expiredSchedule2)

        every { concertService.getConcert(concertId) } returns concert
        every { concertScheduleService.findByConcertId(concert.id) } returns schedules

        // when
        val result = concertScheduleUseCase.getAvailableSchedules(concertId)

        // then
        assertThat(result).isEmpty()
        verify(exactly = 1) { concertService.getConcert(concertId) }
        verify(exactly = 1) { concertScheduleService.findByConcertId(concert.id) }
    }

    @Test
    @DisplayName("예약 가능한 좌석 조회 성공")
    fun getAvailableSeats_Success() {
        // given
        val concertId = 1L
        val scheduleId = 1L
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val seat1 = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.AVAILABLE,
            price = 50000,
        )
        val seat2 = Seat(
            concertSchedule = schedule,
            seatNumber = 2,
            seatStatus = SeatStatus.AVAILABLE,
            price = 50000,
        )
        schedule.seats.addAll(listOf(seat1, seat2))

        every { concertService.getConcert(concertId) } returns concert
        every { concertScheduleService.findByConcertIdAndId(concert.id, scheduleId) } returns schedule

        // when
        val result = concertScheduleUseCase.getAvailableSeats(concertId, scheduleId)

        // then
        assertThat(result).hasSize(2)
        verify(exactly = 1) { concertService.getConcert(concertId) }
        verify(exactly = 1) { concertScheduleService.findByConcertIdAndId(concert.id, scheduleId) }
    }

    @Test
    @DisplayName("예약 가능한 좌석 조회 - 예약된 좌석 필터링")
    fun getAvailableSeats_FilterReserved() {
        // given
        val concertId = 1L
        val scheduleId = 1L
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val availableSeat = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.AVAILABLE,
            price = 50000,
        )
        val reservedSeat = Seat(
            concertSchedule = schedule,
            seatNumber = 2,
            seatStatus = SeatStatus.RESERVED, // 예약된 좌석
            price = 50000,
        )
        val temporarySeat = Seat(
            concertSchedule = schedule,
            seatNumber = 3,
            seatStatus = SeatStatus.TEMPORARY_RESERVED, // 임시 예약 좌석
            price = 50000,
        )
        schedule.seats.addAll(listOf(availableSeat, reservedSeat, temporarySeat))

        every { concertService.getConcert(concertId) } returns concert
        every { concertScheduleService.findByConcertIdAndId(concert.id, scheduleId) } returns schedule

        // when
        val result = concertScheduleUseCase.getAvailableSeats(concertId, scheduleId)

        // then
        assertThat(result).hasSize(1) // 예약된 좌석과 임시 예약 좌석은 제외됨
        verify(exactly = 1) { concertService.getConcert(concertId) }
        verify(exactly = 1) { concertScheduleService.findByConcertIdAndId(concert.id, scheduleId) }
    }

    @Test
    @DisplayName("예약 가능한 좌석 조회 - 모든 좌석이 예약된 경우 빈 목록 반환")
    fun getAvailableSeats_EmptyWhenAllReserved() {
        // given
        val concertId = 1L
        val scheduleId = 1L
        val concert = Concert(title = "Test Concert", description = "Test Description")
        val schedule = ConcertSchedule(
            concert = concert,
            concertDate = LocalDate.now().plusDays(10),
        )
        val reservedSeat1 = Seat(
            concertSchedule = schedule,
            seatNumber = 1,
            seatStatus = SeatStatus.RESERVED,
            price = 50000,
        )
        val reservedSeat2 = Seat(
            concertSchedule = schedule,
            seatNumber = 2,
            seatStatus = SeatStatus.RESERVED,
            price = 50000,
        )
        schedule.seats.addAll(listOf(reservedSeat1, reservedSeat2))

        every { concertService.getConcert(concertId) } returns concert
        every { concertScheduleService.findByConcertIdAndId(concert.id, scheduleId) } returns schedule

        // when
        val result = concertScheduleUseCase.getAvailableSeats(concertId, scheduleId)

        // then
        assertThat(result).isEmpty()
        verify(exactly = 1) { concertService.getConcert(concertId) }
        verify(exactly = 1) { concertScheduleService.findByConcertIdAndId(concert.id, scheduleId) }
    }
}
