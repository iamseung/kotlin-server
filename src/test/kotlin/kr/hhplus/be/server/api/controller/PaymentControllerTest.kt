package kr.hhplus.be.server.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.api.dto.request.ChargePointRequest
import kr.hhplus.be.server.api.dto.request.CreateReservationRequest
import kr.hhplus.be.server.api.dto.request.IssueQueueTokenRequest
import kr.hhplus.be.server.api.dto.request.ProcessPaymentRequest
import kr.hhplus.be.server.domain.concert.model.SeatStatus
import kr.hhplus.be.server.infrastructure.persistence.concert.entity.Concert
import kr.hhplus.be.server.infrastructure.persistence.point.entity.Point
import kr.hhplus.be.server.infrastructure.persistence.point.repository.PointJpaRepository
import kr.hhplus.be.server.infrastructure.persistence.concert.entity.ConcertSchedule
import kr.hhplus.be.server.infrastructure.persistence.concert.entity.Seat
import kr.hhplus.be.server.infrastructure.persistence.concert.repository.ConcertJpaRepository
import kr.hhplus.be.server.infrastructure.persistence.concert.repository.ConcertScheduleJpaRepository
import kr.hhplus.be.server.infrastructure.persistence.concert.repository.SeatJpaRepository
import kr.hhplus.be.server.infrastructure.persistence.payment.repository.PaymentJpaRepository
import kr.hhplus.be.server.infrastructure.persistence.queue.repository.RedisQueueRepository
import kr.hhplus.be.server.infrastructure.persistence.reservation.repository.ReservationJpaRepository
import kr.hhplus.be.server.infrastructure.persistence.user.entity.User
import kr.hhplus.be.server.infrastructure.persistence.user.repository.UserJpaRepository
import kr.hhplus.be.server.support.AbstractIntegrationContainerBaseTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@AutoConfigureMockMvc
@DisplayName("결제 컨트롤러 API 통합 테스트")
class PaymentControllerTest : AbstractIntegrationContainerBaseTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var concertJpaRepository: ConcertJpaRepository

    @Autowired
    private lateinit var concertScheduleJpaRepository: ConcertScheduleJpaRepository

    @Autowired
    private lateinit var seatJpaRepository: SeatJpaRepository

    @Autowired
    private lateinit var reservationJpaRepository: ReservationJpaRepository

    @Autowired
    private lateinit var paymentJpaRepository: PaymentJpaRepository

    @Autowired
    private lateinit var pointJpaRepository: PointJpaRepository

    @Autowired
    private lateinit var redisQueueRepository: RedisQueueRepository

    @BeforeEach
    fun setUp() {
        cleanupDatabase()
    }

    @AfterEach
    fun tearDown() {
        cleanupDatabase()
    }

    private fun cleanupDatabase() {
        paymentJpaRepository.deleteAll()
        reservationJpaRepository.deleteAll()
        seatJpaRepository.deleteAll()
        concertScheduleJpaRepository.deleteAll()
        concertJpaRepository.deleteAll()
        userJpaRepository.deleteAll()
        // Redis cleanup
        redisQueueRepository.getAllWaitingUsers().forEach { userId ->
            redisQueueRepository.removeFromActiveQueue(userId)
        }
        redisQueueRepository.getAllActiveUsers().forEach { userId ->
            redisQueueRepository.removeFromActiveQueue(userId)
        }
    }

    private fun issueAndActivateToken(userId: Long): String {
        val request = IssueQueueTokenRequest(userId = userId)
        val tokenResponse = mockMvc.perform(
            post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val token = objectMapper.readTree(tokenResponse.response.contentAsString).get("token").asText()
        // 토큰을 ACTIVE 상태로 변경
        redisQueueRepository.activateWaitingUsers(1)
        // Allow Redis to propagate changes
        Thread.sleep(500)
        return token
    }

    private fun chargePoints(userId: Long, amount: Int) {
        pointJpaRepository.save(Point(userId, 0))
        val chargeRequest = ChargePointRequest(userId = userId, amount = amount)
        mockMvc.perform(
            post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeRequest)),
        )
    }

    private fun createReservation(userId: Long, concertId: Long, scheduleId: Long, seatId: Long, token: String): Long {
        val reservationRequest = CreateReservationRequest(
            userId = userId,
            scheduleId = scheduleId,
            seatId = seatId,
        )
        val response = mockMvc.perform(
            post("/api/v1/concerts/{concertId}/reservations", concertId)
                .header("X-Queue-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reservationRequest)),
        )
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readTree(response.response.contentAsString).get("id").asLong()
    }

    @Test
    @DisplayName("POST /api/payments - 결제 처리 성공")
    fun processPaymentSuccess() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        val concert = concertJpaRepository.save(Concert("테스트 콘서트", null))
        val schedule = concertScheduleJpaRepository.save(
            ConcertSchedule(concert.id, LocalDate.now().plusDays(7)),
        )
        val seat = seatJpaRepository.save(Seat(schedule.id, 1, SeatStatus.AVAILABLE, 50000))

        // 포인트 충전
        chargePoints(user.id, 100000)

        // 토큰 발급 및 활성화
        val token = issueAndActivateToken(user.id)

        // 좌석 예약
        val reservationId = createReservation(user.id, concert.id, schedule.id, seat.id, token)

        val request = ProcessPaymentRequest(reservationId = reservationId)

        // when & then
        mockMvc.perform(
            post("/api/payments")
                .header("User-Id", user.id)
                .header("X-Queue-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paymentId").isNumber)
            .andExpect(jsonPath("$.reservationId").value(reservationId))
            .andExpect(jsonPath("$.userId").value(user.id))
            .andExpect(jsonPath("$.amount").value(50000))
            .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
    }

    @Test
    @DisplayName("POST /api/payments - 잘못된 예약 ID로 결제 시도 시 실패")
    fun processPaymentWithInvalidReservationId() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))

        // 토큰 발급 및 활성화
        val token = issueAndActivateToken(user.id)

        val request = ProcessPaymentRequest(reservationId = 999L)

        // when & then
        mockMvc.perform(
            post("/api/payments")
                .header("User-Id", user.id)
                .header("X-Queue-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DisplayName("POST /api/payments - 포인트 부족 시 결제 실패")
    fun processPaymentWithInsufficientPoints() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        val concert = concertJpaRepository.save(Concert("테스트 콘서트", null))
        val schedule = concertScheduleJpaRepository.save(
            ConcertSchedule(concert.id, LocalDate.now().plusDays(7)),
        )
        val seat = seatJpaRepository.save(Seat(schedule.id, 1, SeatStatus.AVAILABLE, 50000))

        // 포인트 충전 (부족한 금액)
        chargePoints(user.id, 10000)

        // 토큰 발급 및 활성화
        val token = issueAndActivateToken(user.id)

        // 좌석 예약
        val reservationId = createReservation(user.id, concert.id, schedule.id, seat.id, token)

        val request = ProcessPaymentRequest(reservationId = reservationId)

        // when & then
        mockMvc.perform(
            post("/api/payments")
                .header("User-Id", user.id)
                .header("X-Queue-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().is4xxClientError)
    }
}
