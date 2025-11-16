package kr.hhplus.be.server.payment.service

import kr.hhplus.be.server.payment.entity.Payment
import kr.hhplus.be.server.payment.repository.PaymentRepository
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {

    fun savePayment(payment: Payment): Payment {
        return paymentRepository.save(payment)
    }
}