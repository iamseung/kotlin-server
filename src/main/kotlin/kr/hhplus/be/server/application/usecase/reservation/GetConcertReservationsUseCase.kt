package kr.hhplus.be.server.application.usecase.reservation

import kr.hhplus.be.server.domain.reservation.service.ReservationService
import kr.hhplus.be.server.domain.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetConcertReservationsUseCase(
    private val userService: UserService,
    private val reservationService: ReservationService,
) {

    @Transactional(readOnly = true)
    fun execute(command: GetConcertReservationsCommand): GetConcertReservationsResult {
        // 1. 사용자 검증
        val user = userService.findById(command.userId)

        // 2. 예약 목록 조회
        val reservations = reservationService.findAllByUserId(user.id)

        // 3. 결과 반환
        return GetConcertReservationsResult(
            reservations = reservations.map { reservation ->
                GetConcertReservationsResult.ReservationInfo(
                    reservationId = reservation.id,
                    userId = reservation.userId,
                    seatId = reservation.seatId,
                    status = reservation.reservationStatus,
                    temporaryReservedAt = reservation.temporaryReservedAt,
                    temporaryExpiredAt = reservation.temporaryExpiredAt,
                )
            },
        )
    }
}
