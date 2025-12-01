package kr.hhplus.be.server.application.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.domain.concert.service.SeatService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SeatSchedulerTest {

    private val seatService = mockk<SeatService>()
    private val seatScheduler = SeatScheduler(seatService)

    @Test
    @DisplayName("만료된 임시 좌석을 복원한다")
    fun restoreExpiredTemporarySeats() {
        // given
        val expectedRestoredCount = 3
        every { seatService.restoreExpiredTemporaryReservations(5L) } returns expectedRestoredCount

        // when
        seatScheduler.restoreExpiredTemporarySeats()

        // then
        verify(exactly = 1) { seatService.restoreExpiredTemporaryReservations(5L) }
    }

    @Test
    @DisplayName("복원할 임시 좌석이 없어도 에러가 발생하지 않는다")
    fun restoreExpiredTemporarySeats_noExpiredSeats() {
        // given
        every { seatService.restoreExpiredTemporaryReservations(5L) } returns 0

        // when
        seatScheduler.restoreExpiredTemporarySeats()

        // then
        verify(exactly = 1) { seatService.restoreExpiredTemporaryReservations(5L) }
    }

    @Test
    @DisplayName("스케줄러 실행 중 에러가 발생해도 다음 실행에 영향을 주지 않는다")
    fun restoreExpiredTemporarySeats_errorHandling() {
        // given
        every { seatService.restoreExpiredTemporaryReservations(5L) } throws RuntimeException("Test exception")

        // when
        seatScheduler.restoreExpiredTemporarySeats()

        // then
        verify(exactly = 1) { seatService.restoreExpiredTemporaryReservations(5L) }
    }
}
