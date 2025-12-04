package kr.hhplus.be.server.domain.concert.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SeatModelTest {

    @Test
    @DisplayName("임시 예약 상태에서 5분이 경과하면 만료된 것으로 판단한다")
    fun isExpiredTemporaryReservation_expired() {
        // given
        val now = LocalDateTime.now()
        val seat = createSeat(SeatStatus.TEMPORARY_RESERVED, now.minusMinutes(6))

        // when
        val isExpired = seat.isExpiredTemporaryReservation(5L)

        // then
        assertThat(isExpired).isTrue()
    }

    @Test
    @DisplayName("임시 예약 상태에서 5분이 경과하지 않으면 만료되지 않은 것으로 판단한다")
    fun isExpiredTemporaryReservation_notExpired() {
        // given
        val now = LocalDateTime.now()
        val seat = createSeat(SeatStatus.TEMPORARY_RESERVED, now.minusMinutes(3))

        // when
        val isExpired = seat.isExpiredTemporaryReservation(5L)

        // then
        assertThat(isExpired).isFalse()
    }

    @Test
    @DisplayName("AVAILABLE 상태의 좌석은 만료된 것으로 판단하지 않는다")
    fun isExpiredTemporaryReservation_availableStatus() {
        // given
        val now = LocalDateTime.now()
        val seat = createSeat(SeatStatus.AVAILABLE, now.minusMinutes(10))

        // when
        val isExpired = seat.isExpiredTemporaryReservation(5L)

        // then
        assertThat(isExpired).isFalse()
    }

    @Test
    @DisplayName("RESERVED 상태의 좌석은 만료된 것으로 판단하지 않는다")
    fun isExpiredTemporaryReservation_reservedStatus() {
        // given
        val now = LocalDateTime.now()
        val seat = createSeat(SeatStatus.RESERVED, now.minusMinutes(10))

        // when
        val isExpired = seat.isExpiredTemporaryReservation(5L)

        // then
        assertThat(isExpired).isFalse()
    }

    @Test
    @DisplayName("임시 예약 상태의 좌석을 AVAILABLE 상태로 복원한다")
    fun restoreToAvailable_success() {
        // given
        val seat = createSeat(SeatStatus.TEMPORARY_RESERVED, LocalDateTime.now().minusMinutes(10))

        // when
        seat.restoreToAvailable()

        // then
        assertThat(seat.seatStatus).isEqualTo(SeatStatus.AVAILABLE)
    }

    @Test
    @DisplayName("AVAILABLE 상태의 좌석을 복원해도 상태가 변경되지 않는다")
    fun restoreToAvailable_alreadyAvailable() {
        // given
        val seat = createSeat(SeatStatus.AVAILABLE, LocalDateTime.now())

        // when
        seat.restoreToAvailable()

        // then
        assertThat(seat.seatStatus).isEqualTo(SeatStatus.AVAILABLE)
    }

    @Test
    @DisplayName("RESERVED 상태의 좌석은 복원할 수 없다")
    fun restoreToAvailable_reservedStatus() {
        // given
        val seat = createSeat(SeatStatus.RESERVED, LocalDateTime.now())

        // when
        seat.restoreToAvailable()

        // then
        assertThat(seat.seatStatus).isEqualTo(SeatStatus.RESERVED)
    }

    private fun createSeat(status: SeatStatus, updatedAt: LocalDateTime): SeatModel {
        return SeatModel.reconstitute(
            id = 1L,
            concertScheduleId = 1L,
            seatNumber = 1,
            seatStatus = status,
            price = 10000,
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = updatedAt,
        )
    }
}
