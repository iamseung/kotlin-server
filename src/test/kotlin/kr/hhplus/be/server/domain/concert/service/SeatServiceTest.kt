package kr.hhplus.be.server.domain.concert.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.domain.concert.model.SeatModel
import kr.hhplus.be.server.domain.concert.model.SeatStatus
import kr.hhplus.be.server.domain.concert.repository.SeatRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SeatServiceTest {

    private val seatRepository = mockk<SeatRepository>()
    private val seatService = SeatService(seatRepository)

    @Test
    @DisplayName("만료된 임시 좌석을 AVAILABLE 상태로 복원한다")
    fun restoreExpiredTemporaryReservations_success() {
        // given
        val now = LocalDateTime.now()
        val expiredSeat1 = createSeat(1L, SeatStatus.TEMPORARY_RESERVED, now.minusMinutes(10))
        val expiredSeat2 = createSeat(2L, SeatStatus.TEMPORARY_RESERVED, now.minusMinutes(6))
        val validSeat = createSeat(3L, SeatStatus.TEMPORARY_RESERVED, now.minusMinutes(3))

        every { seatRepository.findAllByStatus(SeatStatus.TEMPORARY_RESERVED) } returns
            listOf(expiredSeat1, expiredSeat2, validSeat)

        val updatedSeats = mutableListOf<SeatModel>()
        every { seatRepository.update(capture(updatedSeats)) } answers { firstArg() }

        // when
        val restoredCount = seatService.restoreExpiredTemporaryReservations(5L)

        // then
        assertThat(restoredCount).isEqualTo(2)
        assertThat(updatedSeats).hasSize(2)
        assertThat(updatedSeats.all { it.seatStatus == SeatStatus.AVAILABLE }).isTrue()
    }

    @Test
    @DisplayName("만료되지 않은 임시 좌석은 복원하지 않는다")
    fun restoreExpiredTemporaryReservations_notExpired() {
        // given
        val now = LocalDateTime.now()
        val validSeat1 = createSeat(1L, SeatStatus.TEMPORARY_RESERVED, now.minusMinutes(3))
        val validSeat2 = createSeat(2L, SeatStatus.TEMPORARY_RESERVED, now.minusMinutes(1))

        every { seatRepository.findAllByStatus(SeatStatus.TEMPORARY_RESERVED) } returns
            listOf(validSeat1, validSeat2)

        // when
        val restoredCount = seatService.restoreExpiredTemporaryReservations(5L)

        // then
        assertThat(restoredCount).isEqualTo(0)
        verify(exactly = 0) { seatRepository.update(any()) }
    }

    @Test
    @DisplayName("임시 좌석이 없으면 복원 작업을 수행하지 않는다")
    fun restoreExpiredTemporaryReservations_noSeats() {
        // given
        every { seatRepository.findAllByStatus(SeatStatus.TEMPORARY_RESERVED) } returns emptyList()

        // when
        val restoredCount = seatService.restoreExpiredTemporaryReservations(5L)

        // then
        assertThat(restoredCount).isEqualTo(0)
        verify(exactly = 0) { seatRepository.update(any()) }
    }

    private fun createSeat(
        id: Long,
        status: SeatStatus,
        updatedAt: LocalDateTime,
    ): SeatModel {
        return SeatModel.reconstitute(
            id = id,
            concertScheduleId = 1L,
            seatNumber = id.toInt(),
            seatStatus = status,
            price = 10000,
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = updatedAt,
        )
    }
}
