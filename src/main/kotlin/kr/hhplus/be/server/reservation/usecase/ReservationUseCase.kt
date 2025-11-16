package kr.hhplus.be.server.reservation.usecase

import kr.hhplus.be.server.concert.service.SeatService
import kr.hhplus.be.server.reservation.dto.response.ReservationResponse
import kr.hhplus.be.server.reservation.entity.Reservation
import kr.hhplus.be.server.reservation.service.ReservationService
import kr.hhplus.be.server.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ReservationUseCase(
    private val userService: UserService,
    private val seatService: SeatService,
    private val reservationService: ReservationService,
) {

    @Transactional(readOnly = true)
    fun getConcertReservations(userId: Long): List<ReservationResponse> {
        val user = userService.getUser(userId)
        val reservations = reservationService.findAllByUserId(user.id)

        return reservations.map { ReservationResponse.from(it) }
    }

    @Transactional
    fun createReservation(
        userId: Long,
        scheduleId: Long,
        seatId: Long,
    ): ReservationResponse {
        val user = userService.getUser(userId)
        val seat = seatService.findByIdAndConcertScheduleId(seatId, scheduleId)

        // 좌석 사용 가능 여부 검증
        seat.validateAvailable()

        // 콘서트 일정 유효성 검증
        val concertSchedule = seat.concertSchedule
        concertSchedule.validateAvailable()

        // 좌석 임시 예약 (5분간 TEMPORARY_RESERVED)
        seat.temporaryReservation()

        // 예약 생성 (기본 상태: TEMPORARY, 5분 후 만료)
        val reservation = reservationService.save(Reservation.of(user, seat))

        return ReservationResponse.from(reservation)
    }
}