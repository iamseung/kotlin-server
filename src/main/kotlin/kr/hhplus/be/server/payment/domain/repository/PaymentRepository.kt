package kr.hhplus.be.server.payment.domain.repository

import kr.hhplus.be.server.payment.domain.model.Payment

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Payment?
    fun findByIdOrThrow(id: Long): Payment
    fun findByReservationId(reservationId: Long): Payment?
    fun findAllByUserId(userId: Long): List<Payment>
}
