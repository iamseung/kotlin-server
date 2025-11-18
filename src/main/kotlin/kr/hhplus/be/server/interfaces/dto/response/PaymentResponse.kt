package kr.hhplus.be.server.interfaces.dto.response

import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.payment.domain.model.PaymentStatus

data class PaymentResponse(
    val paymentId: Long?,
    val reservationId: Long,
    val userId: Long,
    val amount: Int,
    val paymentStatus: PaymentStatus,
) {
    companion object {
        fun from(payment: Payment): PaymentResponse {
            return PaymentResponse(
                paymentId = payment.getId(),
                reservationId = payment.reservationId,
                userId = payment.userId,
                amount = payment.amount,
                paymentStatus = payment.paymentStatus,
            )
        }
    }
}