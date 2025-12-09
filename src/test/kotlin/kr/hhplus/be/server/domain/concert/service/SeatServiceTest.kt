package kr.hhplus.be.server.domain.concert.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.domain.concert.repository.SeatRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SeatServiceTest {

    private val seatRepository = mockk<SeatRepository>()
    private val seatService = SeatService(seatRepository)

    @Test
    @DisplayName("만료된 임시 좌석을 AVAILABLE 상태로 복원한다")
    fun restoreExpiredSeats_success() {
        // given
        val expiredSeatIds = listOf(1L, 2L, 3L)
        every { seatRepository.bulkRestoreExpiredSeats(expiredSeatIds) } returns 3

        // when
        val restoredCount = seatService.restoreExpiredSeats(expiredSeatIds)

        // then
        assertThat(restoredCount).isEqualTo(3)
        verify(exactly = 1) { seatRepository.bulkRestoreExpiredSeats(expiredSeatIds) }
    }

    @Test
    @DisplayName("만료된 좌석이 없으면 0을 반환한다")
    fun restoreExpiredSeats_noSeats() {
        // given
        val emptySeatIds = emptyList<Long>()
        every { seatRepository.bulkRestoreExpiredSeats(emptySeatIds) } returns 0

        // when
        val restoredCount = seatService.restoreExpiredSeats(emptySeatIds)

        // then
        assertThat(restoredCount).isEqualTo(0)
        verify(exactly = 1) { seatRepository.bulkRestoreExpiredSeats(emptySeatIds) }
    }

    @Test
    @DisplayName("일부 좌석만 복원할 수 있다")
    fun restoreExpiredSeats_partial() {
        // given
        val seatIds = listOf(1L, 2L, 3L, 4L, 5L)
        every { seatRepository.bulkRestoreExpiredSeats(seatIds) } returns 2

        // when
        val restoredCount = seatService.restoreExpiredSeats(seatIds)

        // then
        assertThat(restoredCount).isEqualTo(2)
        verify(exactly = 1) { seatRepository.bulkRestoreExpiredSeats(seatIds) }
    }
}
