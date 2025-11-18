package kr.hhplus.be.server.payment.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.reservation.entity.Reservation
import kr.hhplus.be.server.user.entity.User
import java.time.LocalDateTime

@Entity
class Payment(
    @ManyToOne
    @JoinColumn(name = "reservation_id", nullable = false)
    val reservation: Reservation,

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    val amount: Int,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING

    val paymentAt: LocalDateTime = LocalDateTime.now()

    companion object {
        fun of(reservation: Reservation, user: User, amount: Int): Payment {
            return Payment(
                reservation = reservation,
                user = user,
                amount = amount,
            )
        }
    }
}
