package kr.hhplus.be.server.application

import kr.hhplus.be.server.concert.service.SeatService
import kr.hhplus.be.server.payment.domain.model.Payment
import kr.hhplus.be.server.interfaces.dto.response.PaymentResponse
import kr.hhplus.be.server.payment.entity.TransactionType
import kr.hhplus.be.server.payment.service.PaymentService
import kr.hhplus.be.server.point.service.PointHistoryService
import kr.hhplus.be.server.point.service.PointService
import kr.hhplus.be.server.queue.service.QueueTokenService
import kr.hhplus.be.server.reservation.service.ReservationService
import kr.hhplus.be.server.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class PaymentUseCase(
    private val userService: UserService,
    private val reservationService: ReservationService,
    private val seatService: SeatService,
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService,
    private val paymentService: PaymentService,
    private val queueTokenService: QueueTokenService,
) {

    fun processPayment(userId: Long, reservationId: Long, queueToken: String): PaymentResponse {
        userService.getUser(userId)
        val reservation = reservationService.findById(reservationId)

        reservation.validateOwnership(userId)
        reservation.validatePayable()

        val seat = seatService.findById(reservation.seatId)

        pointService.usePoint(userId, seat.price)
        pointHistoryService.savePointHistory(userId, seat.price, TransactionType.USE)

        val payment = paymentService.savePayment(Payment.Companion.create(reservationId, userId, seat.price))

        seat.confirmReservation()
        reservation.confirmPayment()

        val token = queueTokenService.getQueueTokenByToken(queueToken)
        queueTokenService.expireQueueToken(token)

        return PaymentResponse.Companion.from(payment)
    }
}