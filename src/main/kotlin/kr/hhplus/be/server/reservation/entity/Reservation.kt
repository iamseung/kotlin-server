package kr.hhplus.be.server.reservation.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.concert.entity.Seat
import kr.hhplus.be.server.user.entity.User
import java.time.LocalDateTime

@Entity
class Reservation(
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    val seat: Seat,

    @Enumerated(EnumType.STRING)
    var reservationStatus: ReservationStatus = ReservationStatus.TEMPORARY,
) : BaseEntity() {

    val temporaryReservedAt: LocalDateTime = LocalDateTime.now()
    val temporaryExpiredAt: LocalDateTime = LocalDateTime.now().plusMinutes(5)

    fun validateOwnership(userId: Long) {
        if (this.user.id != userId) {
            throw BusinessException(ErrorCode.INVALID_RESERVATION)
        }
    }

    fun validatePayable() {
        if (reservationStatus != ReservationStatus.TEMPORARY) {
            throw BusinessException(ErrorCode.INVALID_RESERVATION_STATUS)
        }
    }

    fun confirmPayment() {
        validatePayable()
        this.reservationStatus = ReservationStatus.CONFIRMED
    }

    companion object {
        fun of(user: User, seat: Seat): Reservation {
            return Reservation(
                user = user,
                seat = seat,
            )
        }
    }
}
