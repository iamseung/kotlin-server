package kr.hhplus.be.server.application.usecase.payment

data class ProcessPaymentCommand(
    val userId: Long,
    val reservationId: Long,
    val queueToken: String
)
