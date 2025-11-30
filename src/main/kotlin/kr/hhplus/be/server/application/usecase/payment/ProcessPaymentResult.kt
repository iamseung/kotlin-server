package kr.hhplus.be.server.application.usecase.payment

import java.time.LocalDateTime

data class ProcessPaymentResult(
    val paymentId: Long,
    val reservationId: Long,
    val userId: Long,
    val amount: Int,
    val paymentDate: LocalDateTime,
)
