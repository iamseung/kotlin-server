package kr.hhplus.be.server.payment.dto.response

import kr.hhplus.be.server.payment.entity.Payment
import kr.hhplus.be.server.payment.entity.PaymentStatus

data class PaymentResponse(
    val paymentId: Long,
    val reservationId: Long,
    val userId: Long,
    val amount: Int,
    val paymentStatus: PaymentStatus,
) {
    companion object {
        fun from(payment: Payment): PaymentResponse {
            return PaymentResponse(
                paymentId = payment.id,
                reservationId = payment.reservation.id,
                userId = payment.user.id,
                amount = payment.amount,
                paymentStatus = payment.paymentStatus,
            )
        }
    }
}
