package kr.hhplus.be.server.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.api.dto.request.CreateReservationRequest
import kr.hhplus.be.server.api.dto.response.ReservationResponse
import kr.hhplus.be.server.application.usecase.reservation.CreateReservationCommand
import kr.hhplus.be.server.application.usecase.reservation.CreateReservationUseCase
import kr.hhplus.be.server.application.usecase.reservation.GetConcertReservationsCommand
import kr.hhplus.be.server.application.usecase.reservation.GetConcertReservationsUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/concerts")
@Tag(name = "Reservations", description = "좌석 예약 관리")
class ReservationController(
    private val getConcertReservationsUseCase: GetConcertReservationsUseCase,
    private val createReservationUseCase: CreateReservationUseCase,
) {

    @Operation(
        summary = "좌석 예약 조회",
        description = "예약한 좌석을 조회합니다.",
    )
    @GetMapping("/{concertId}/reservations")
    fun getConcertReservations(
        @PathVariable concertId: Long,
        @RequestHeader("User-Id") userId: Long,
    ): List<ReservationResponse> {
        val command = GetConcertReservationsCommand(userId = userId)
        val result = getConcertReservationsUseCase.execute(command)
        return result.reservations.map { ReservationResponse.from(it) }
    }

    @Operation(
        summary = "좌석 예약 요청",
        description = """날짜와 좌석 정보를 입력받아 좌석을 예약합니다.

**중요**:
- 좌석 예약 시 해당 좌석은 5분간 임시 배정됩니다.
- 5분 내 결제 미완료 시 임시 배정이 해제되어 다른 사용자가 예약 가능합니다.
- 대기열 토큰이 ACTIVE 상태여야 예약 가능합니다.""",
        operationId = "createReservation",
        security = [SecurityRequirement(name = "QueueToken")],
    )
    @PostMapping("/{concertId}/reservations")
    fun createReservation(
        @Parameter(description = "콘서트 ID", required = true)
        @PathVariable concertId: Long,
        @Parameter(description = "대기열 토큰 (UUID 형식)", required = true, `in` = ParameterIn.HEADER)
        @RequestHeader("X-Queue-Token") queueToken: String,
        @RequestBody request: CreateReservationRequest,
    ): ReservationResponse {
        val command = CreateReservationCommand(
            userId = request.userId,
            scheduleId = request.scheduleId,
            seatId = request.seatId,
            queueToken = queueToken,
        )
        val result = createReservationUseCase.execute(command)
        return ReservationResponse(
            id = result.reservationId,
            seatId = result.seatId,
            reservationStatus = result.status.name,
            temporaryReservedAt = result.reservedAt.toString(),
            temporaryExpiresAt = result.reservedAt.plusMinutes(5).toString(),
        )
    }
}
