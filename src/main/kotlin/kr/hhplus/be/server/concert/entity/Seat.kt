package kr.hhplus.be.server.concert.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode

@Entity
class Seat(
    @ManyToOne
    @JoinColumn(name = "concert_schedule_id", nullable = false)
    val concertSchedule: ConcertSchedule,

    val seatNumber: Int,

    @Enumerated(EnumType.STRING)
    var seatStatus: SeatStatus,

    val price: Int,
) : BaseEntity() {

    val isAvailable: Boolean
        get() = seatStatus == SeatStatus.AVAILABLE

    fun validateAvailable() {
        if (!isAvailable) {
            throw BusinessException(ErrorCode.SEAT_NOT_AVAILABLE)
        }
    }

    fun temporaryReservation() {
        validateAvailable()
        this.seatStatus = SeatStatus.TEMPORARY_RESERVED
    }

    fun confirmReservation() {
        if (seatStatus != SeatStatus.TEMPORARY_RESERVED) {
            throw BusinessException(ErrorCode.SEAT_NOT_AVAILABLE)
        }
        this.seatStatus = SeatStatus.RESERVED
    }
}
