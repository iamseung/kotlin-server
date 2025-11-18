package kr.hhplus.be.server.payment.infrastructure

import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.payment.domain.repository.PaymentRepository
import kr.hhplus.be.server.payment.entity.Payment as PaymentEntity
import kr.hhplus.be.server.payment.entity.PaymentStatus as PaymentStatusEntity
import kr.hhplus.be.server.payment.domain.model.PaymentStatus as PaymentStatusDomain
import kr.hhplus.be.server.payment.repository.PaymentJpaRepository
import kr.hhplus.be.server.reservation.repository.ReservationJpaRepository
import kr.hhplus.be.server.user.repository.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryAdapter(
    private val paymentJpaRepository: PaymentJpaRepository,
    private val reservationJpaRepository: ReservationJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        val entity = toEntity(payment)
        val saved = paymentJpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): Payment? {
        return paymentJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findByIdOrThrow(id: Long): Payment {
        return findById(id) ?: throw BusinessException(ErrorCode.PAYMENT_NOT_FOUND)
    }

    override fun findByReservationId(reservationId: Long): Payment? {
        return paymentJpaRepository.findByReservationId(reservationId)?.let { toDomain(it) }
    }

    override fun findAllByUserId(userId: Long): List<Payment> {
        return paymentJpaRepository.findAllByUserId(userId).map { toDomain(it) }
    }

    private fun toDomain(entity: PaymentEntity): Payment {
        return Payment.reconstitute(
            id = entity.id!!,
            reservationId = entity.reservation.id!!,
            userId = entity.user.id!!,
            amount = entity.amount,
            paymentStatus = toDomainStatus(entity.paymentStatus),
            paymentAt = entity.paymentAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    private fun toEntity(domain: Payment): PaymentEntity {
        val reservation = reservationJpaRepository.findByIdOrNull(domain.reservationId)
            ?: throw BusinessException(ErrorCode.RESERVATION_NOT_FOUND)
        val user = userJpaRepository.findByIdOrNull(domain.userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        val entity = PaymentEntity.of(reservation, user, domain.amount)
        domain.getId()?.let { entity.id = it }
        return entity
    }

    private fun toDomainStatus(status: PaymentStatusEntity): PaymentStatusDomain {
        return when (status) {
            PaymentStatusEntity.PENDING -> PaymentStatusDomain.PENDING
            PaymentStatusEntity.CONFIRMED -> PaymentStatusDomain.COMPLETED
            PaymentStatusEntity.CANCELLED -> PaymentStatusDomain.FAILED
            PaymentStatusEntity.FAILED -> PaymentStatusDomain.FAILED
        }
    }
}
