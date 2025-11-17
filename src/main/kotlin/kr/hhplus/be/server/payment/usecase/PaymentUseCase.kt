package kr.hhplus.be.server.payment.usecase

import kr.hhplus.be.server.concert.entity.Seat
import kr.hhplus.be.server.payment.dto.response.PaymentResponse
import kr.hhplus.be.server.payment.entity.Payment
import kr.hhplus.be.server.payment.entity.TransactionType
import kr.hhplus.be.server.payment.service.PaymentService
import kr.hhplus.be.server.point.service.PointHistoryService
import kr.hhplus.be.server.point.service.PointService
import kr.hhplus.be.server.queue.service.QueueTokenService
import kr.hhplus.be.server.reservation.service.ReservationService
import kr.hhplus.be.server.user.entity.User
import kr.hhplus.be.server.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class PaymentUseCase(
    private val userService: UserService,
    private val reservationService: ReservationService,
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService,
    private val paymentService: PaymentService,
    private val queueTokenService: QueueTokenService,
) {

    fun processPayment(userId: Long, reservationId: Long, queueToken: String): PaymentResponse {
        val user = userService.getUser(userId)
        val reservation = reservationService.findById(reservationId)

        reservation.validateOwnership(user.id)
        reservation.validatePayable()

        val seat = reservation.seat

        // 포인트 차감
        applyPointsToPayment(user, seat)

        // 결제 처리
        val payment = paymentService.savePayment(Payment.of(reservation, user, seat.price))

        // 예약 및 좌석 확정
        seat.confirmReservation()
        reservation.confirmPayment()

        // 대기열 토큰 만료 처리
        val token = queueTokenService.getQueueTokenByToken(queueToken)
        queueTokenService.expireQueueToken(token)

        return PaymentResponse.from(payment)
    }

    private fun applyPointsToPayment(user: User, seat: Seat) {
        pointService.usePoint(user.id, seat.price)
        pointHistoryService.savePointHistory(user, seat.price, TransactionType.USE)
    }
}
